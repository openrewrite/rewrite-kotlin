/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.kotlin;

import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.expressions.FirAnnotation;
import org.jetbrains.kotlin.fir.resolve.LookupTagUtilsKt;
import org.jetbrains.kotlin.fir.symbols.impl.*;
import org.jetbrains.kotlin.fir.types.*;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.StandardClassIds;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.COVARIANT;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.INVARIANT;

public class KotlinTypeMapping implements JavaTypeMapping<FirElement> {
    private final KotlinTypeSignatureBuilder signatureBuilder;
    private final JavaTypeCache typeCache;
    private final FirSession firSession;
    private final JavaReflectionTypeMapping reflectionTypeMapping;


    public KotlinTypeMapping(JavaTypeCache typeCache, FirSession firSession) {
        this.signatureBuilder = new KotlinTypeSignatureBuilder(firSession);
        this.typeCache = typeCache;
        this.firSession = firSession;
        this.reflectionTypeMapping = new JavaReflectionTypeMapping(typeCache);
    }

    public JavaType type(@Nullable FirElement type) {
        if (type == null) {
            return JavaType.Class.Unknown.getInstance();
        }

        String signature = signatureBuilder.signature(type);
        JavaType existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        if (type instanceof FirClass) {
            return classType((FirClass) type, signature);
        } else if (type instanceof FirResolvedTypeRef) {
            ConeKotlinType coneKotlinType = FirTypeUtilsKt.getConeType((FirResolvedTypeRef) type);
            if (coneKotlinType instanceof ConeTypeParameterType) {
                FirClassifierSymbol<?> classifierSymbol = LookupTagUtilsKt.toSymbol(((ConeTypeParameterType) coneKotlinType).getLookupTag(), firSession);
                if (classifierSymbol != null && classifierSymbol.getFir() instanceof FirTypeParameter) {
                    return generic((FirTypeParameter) classifierSymbol.getFir(), signature);
                }
            }
            return resolvedTypeRef((FirResolvedTypeRef) type, signature);
        } else if (type instanceof FirTypeParameter) {
            return generic((FirTypeParameter) type, signature);
        } else if (type instanceof FirValueParameter) {
            return type(((FirValueParameter) type).getReturnTypeRef());
        }

        throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
    }

    private JavaType.FullyQualified classType(FirClass firClass, String signature) {
        FirClassSymbol<? extends FirClass> sym = firClass.getSymbol();

        String classFqn = sym.getClassId().asFqNameString();

        JavaType.FullyQualified fq = typeCache.get(classFqn);
        JavaType.Class clazz = (JavaType.Class) (fq instanceof JavaType.Parameterized ? ((JavaType.Parameterized) fq).getType() : fq);
        if (clazz == null) {
            clazz = new JavaType.Class(
                    null,
                    convertToFlagsBitMap(firClass.getStatus()),
                    classFqn,
                    convertToClassKind(firClass.getClassKind()),
                    null, null, null, null, null, null, null
            );
            typeCache.put(classFqn, clazz);

            FirTypeRef superTypeRef = null;
            List<FirTypeRef> interfaceTypeRefs = null;
            for (FirTypeRef typeRef : firClass.getSuperTypeRefs()) {
                FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(FirTypeUtilsKt.getConeType(typeRef), firSession);
                if (symbol != null && ClassKind.CLASS == symbol.getFir().getClassKind()) {
                    superTypeRef = typeRef;
                } else if (symbol != null && ClassKind.INTERFACE == symbol.getFir().getClassKind()) {
                    if (interfaceTypeRefs == null) {
                        interfaceTypeRefs = new ArrayList<>();
                    }
                    interfaceTypeRefs.add(typeRef);
                }
            }

            JavaType.FullyQualified supertype = superTypeRef == null ? null : TypeUtils.asFullyQualified(type(superTypeRef));

            // TODO: figure out how to access the class owner .. the name exists on the Sym, but there isn't a link through the classId.
            JavaType.FullyQualified owner = null;

            List<FirProperty> properties = new ArrayList<>(firClass.getDeclarations().size());
            List<FirFunction> functions = new ArrayList<>(firClass.getDeclarations().size());
            List<FirEnumEntry> enumEntries = new ArrayList<>(firClass.getDeclarations().size());

            for (FirDeclaration declaration : firClass.getDeclarations()) {
                if (declaration instanceof FirProperty) {
                    // TODO: enums contain an entries property tied to the default getting ... this must be filtered out unless the user explicitly created it.
                    // temp hack until properties are understood in enums.
                    if (!(JavaType.FullyQualified.Class.Kind.Enum == clazz.getKind())) {
                        properties.add((FirProperty) declaration);
                    }
                } else if (declaration instanceof FirSimpleFunction) {
                    functions.add((FirFunction) declaration);
                } else if (declaration instanceof FirConstructor) {
                    // TODO: identify generated constructors.
                    functions.add((FirFunction) declaration);
                } else if (declaration instanceof FirRegularClass) {
                    // TODO: Companion Objects and possible inner classes.
                } else if (declaration instanceof FirEnumEntry) {
                    enumEntries.add((FirEnumEntry) declaration);
                } else {
                    throw new IllegalStateException("Implement me.");
                }
            }

            // May be helpful.
//            FirStatusUtilsKt

            List<JavaType.Variable> fields = null;
            if (!enumEntries.isEmpty()) {
                fields = new ArrayList<>(properties.size() + enumEntries.size());
                for (FirEnumEntry enumEntry : enumEntries) {
                    fields.add(variableType(enumEntry.getSymbol(), clazz));
                }
            }

            if (!properties.isEmpty()) {
                if (fields == null) {
                    fields = new ArrayList<>(properties.size());
                }

                for (FirProperty property : properties) {
                    // TODO: detect and filter out synthetic
                    fields.add(variableType(property.getSymbol(), clazz));
                }
            }

            List<JavaType.Method> methods = null;
            if(!functions.isEmpty()) {
                methods = new ArrayList<>(functions.size());
                for (FirFunction function : functions) {
                    // TODO: detect and filter out synthetic
                    methods.add(methodDeclarationType(function.getSymbol(), clazz));
                }
            }

            List<JavaType.FullyQualified> interfaces = null;
            // TODO: interfaces need to be detected through superTypeRefs with ClassKind

            if (interfaceTypeRefs != null && !interfaceTypeRefs.isEmpty()) {
                interfaces = new ArrayList<>(interfaceTypeRefs.size());
                for (FirTypeRef iParam : interfaceTypeRefs) {
                    JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(iParam));
                    if (javaType != null) {
                        interfaces.add(javaType);
                    }
                }
            }

            List<JavaType.FullyQualified> annotations = getAnnotations(firClass.getAnnotations());

            clazz.unsafeSet(null, supertype, owner, annotations, interfaces, fields, methods);
        }

        if (!firClass.getTypeParameters().isEmpty()) {
            JavaType.Parameterized pt = typeCache.get(signature);
            if (pt == null) {
                pt = new JavaType.Parameterized(null, null, null);
                typeCache.put(signature, pt);

                List<JavaType> typeParameters = new ArrayList<>(firClass.getTypeParameters().size());
                for (FirTypeParameterRef tParam : firClass.getTypeParameters()) {
                    typeParameters.add(type(tParam));
                }

                pt.unsafeSet(clazz, typeParameters);
            }
            return pt;
        }

        return clazz;
    }

    public JavaType.FullyQualified resolvedTypeRef(FirResolvedTypeRef resolvedTypeRef, String signature) {
        FirRegularClass firRegularClass = convertToRegularClass(resolvedTypeRef.getType());
        if (firRegularClass == null) {
            throw new IllegalStateException("unexpected null symbol");
        }
        return classType(firRegularClass, signature);
    }

    @Nullable
    public JavaType.Method methodDeclarationType(@Nullable FirFunctionSymbol<? extends FirFunction> functionSymbol, @Nullable JavaType.FullyQualified declaringType) {
        if (functionSymbol == null) {
            return null;
        }

        String signature = signatureBuilder.methodSignature(functionSymbol);
        JavaType.Method existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        List<String> paramNames = null;
        if (!functionSymbol.getValueParameterSymbols().isEmpty()) {
            paramNames = new ArrayList<>(functionSymbol.getValueParameterSymbols().size());
            for (FirValueParameterSymbol p : functionSymbol.getValueParameterSymbols()) {
                String s = p.getName().asString();
                paramNames.add(s);
            }
        }
        List<String> defaultValues = null;
        JavaType.Method method = new JavaType.Method(
                null,
                convertToFlagsBitMap(functionSymbol.getResolvedStatus()),
                null,
                functionSymbol instanceof FirConstructorSymbol ? "<constructor>" : functionSymbol.getName().asString(),
                null,
                paramNames,
                null, null, null,
                defaultValues
        );

        typeCache.put(signature, method);

        FirRegularClass signatureType =
//                functionSymbol.type instanceof Type.ForAll ?
//                ((Type.ForAll) methodSymbol.type).qtype :
                convertToRegularClass(functionSymbol.getDispatchReceiverType());

        // TODO: thrown exceptions don't exist in Kotlin, but may be specified as annotations to apply to java classes.
        // The annotations will be created ... should the annotations be placed here to align with JavaTypes?
        List<JavaType.FullyQualified> exceptionTypes = null;

        FirRegularClass selectType = null;
//        Type selectType = functionSymbol.getC.type;
//        if (selectType instanceof Type.ForAll) {
//            selectType = ((Type.ForAll) selectType).qtype;
//        }

        JavaType.FullyQualified resolvedDeclaringType = declaringType;
        if (declaringType == null) {
            throw new UnsupportedOperationException("Find owner and resolve declaring type.");
        }

//        if (resolvedDeclaringType == null) {
//            return null;
//        }

        JavaType returnType = type(functionSymbol.getResolvedReturnTypeRef());
        List<JavaType> parameterTypes = null;

        if (!functionSymbol.getValueParameterSymbols().isEmpty()) {
            parameterTypes = new ArrayList<>(functionSymbol.getValueParameterSymbols().size());
            for (FirValueParameterSymbol valueParameterSymbol : functionSymbol.getValueParameterSymbols()) {
                JavaType javaType = type(valueParameterSymbol.getFir());
                parameterTypes.add(javaType);
            }
        }

        method.unsafeSet(resolvedDeclaringType,
                functionSymbol instanceof FirConstructorSymbol ? resolvedDeclaringType : returnType,
                parameterTypes, exceptionTypes, getAnnotations(functionSymbol.getAnnotations()));
        return method;
    }

    @Nullable
    public JavaType.Variable variableType(@Nullable FirVariableSymbol<? extends FirVariable> symbol) {
        return variableType(symbol, null);
    }

    @Nullable
    public JavaType.Variable variableType(@Nullable FirVariableSymbol<? extends FirVariable> symbol, @Nullable JavaType.FullyQualified owner) {
        if (symbol == null) {
            return null;
        }

        String signature = signatureBuilder.variableSignature(symbol);
        JavaType.Variable existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        JavaType.Variable variable = new JavaType.Variable(
                null,
                convertToFlagsBitMap(symbol.getRawStatus()),
                symbol.getName().asString(),
                null, null, null);

        typeCache.put(signature, variable);

        JavaType resolvedOwner = owner;
        if (owner == null) {
            throw new IllegalStateException("implement me.");
        }

        List<JavaType.FullyQualified> annotations = getAnnotations(symbol.getAnnotations());

        variable.unsafeSet(resolvedOwner, type(symbol.getResolvedReturnTypeRef()), annotations);

        return variable;
    }

    public JavaType.Primitive primitive(ConeClassLikeType type) {
        ClassId classId = type.getLookupTag().getClassId();
        if (StandardClassIds.INSTANCE.getByte().equals(classId)) {
            return JavaType.Primitive.Byte;
        } else if (StandardClassIds.INSTANCE.getBoolean().equals(classId)) {
            return JavaType.Primitive.Boolean;
        } else if (StandardClassIds.INSTANCE.getChar().equals(classId)) {
            return JavaType.Primitive.Char;
        } else if (StandardClassIds.INSTANCE.getDouble().equals(classId)) {
            return JavaType.Primitive.Double;
        } else if (StandardClassIds.INSTANCE.getFloat().equals(classId)) {
            return JavaType.Primitive.Float;
        } else if (StandardClassIds.INSTANCE.getInt().equals(classId)) {
            return JavaType.Primitive.Int;
        } else if (StandardClassIds.INSTANCE.getLong().equals(classId)) {
            return JavaType.Primitive.Long;
        } else if (StandardClassIds.INSTANCE.getShort().equals(classId)) {
            return JavaType.Primitive.Short;
        } else if (StandardClassIds.INSTANCE.getString().equals(classId)) {
            return JavaType.Primitive.String;
        } else if (StandardClassIds.INSTANCE.getUnit().equals(classId)) {
            return JavaType.Primitive.Void;
        } else if (StandardClassIds.INSTANCE.getNothing().equals(classId)) {
            return JavaType.Primitive.Null;
        }

        throw new UnsupportedOperationException("Unknown primitive type " + type);
    }

    private JavaType generic(FirTypeParameter typeParameter, String signature) {
        String name = typeParameter.getName().asString();
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null,
                name, INVARIANT, null);
        typeCache.put(signature, gtv);

        List<JavaType> bounds = null;
        JavaType.GenericTypeVariable.Variance variance = null;
        if (!"INVARIANT".equals(typeParameter.getVariance().name())) {
            bounds = new ArrayList<>(typeParameter.getBounds().size());
            for (FirTypeRef bound : typeParameter.getBounds()) {
                if (!(bound instanceof FirImplicitNullableAnyTypeRef)) {
                    bounds.add(type(bound));
                }
            }
            variance = COVARIANT;
        } else {
            variance = INVARIANT;
        }

        gtv.unsafeSet(variance, bounds);
        return gtv;
    }

    private long convertToFlagsBitMap(FirDeclarationStatus status) {
        // TODO ... map status to eq flags.
        return 0;
    }

    private JavaType.FullyQualified.Kind convertToClassKind(ClassKind classKind) {
        JavaType.FullyQualified.Kind kind;
        if (ClassKind.CLASS == classKind) {
            kind = JavaType.FullyQualified.Kind.Class;
        } else if (ClassKind.ANNOTATION_CLASS == classKind) {
            kind = JavaType.FullyQualified.Kind.Annotation;
        } else if (ClassKind.ENUM_CLASS == classKind) {
            kind = JavaType.FullyQualified.Kind.Enum;
        } else if (ClassKind.INTERFACE == classKind) {
            kind = JavaType.FullyQualified.Kind.Interface;
        } else if (ClassKind.OBJECT == classKind) {
            // TODO: fix me ... public object Name
            /*
                public object Unit {
                    override fun toString() = "kotlin.Unit"
                }
             */
            kind = JavaType.FullyQualified.Kind.Class;
        } else {
            throw new UnsupportedOperationException("Unexpected classKind: " + classKind.name());
        }

        return kind;
    }

    private List<JavaType.FullyQualified> getAnnotations(List<FirAnnotation> firAnnotations) {
        List<JavaType.FullyQualified> annotations = new ArrayList<>(firAnnotations.size());
        for (FirAnnotation firAnnotation : firAnnotations) {
            JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) type(convertToRegularClass(firAnnotation.getTypeRef()));
            annotations.add(fullyQualified);
        }

        return annotations;
    }

    // This might exist somewhere among Kotlin's many utils.
    @Nullable
    public FirRegularClass convertToRegularClass(@Nullable ConeKotlinType kotlinType) {
        if (kotlinType != null) {
            FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(kotlinType, firSession);
            if (symbol != null) {
                return symbol.getFir();
            }
        }

        return null;
    }

    @Nullable
    public FirRegularClass convertToRegularClass(@Nullable FirTypeRef firTypeRef) {
        if (firTypeRef != null) {
            ConeKotlinType coneKotlinType = FirTypeUtilsKt.getConeType(firTypeRef);
            return convertToRegularClass(coneKotlinType);
        }

        return null;
    }
}
