package org.openrewrite.kotlin

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.openrewrite.java.JavaTypeMapping
import org.openrewrite.java.internal.JavaTypeCache
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.COVARIANT
import org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.INVARIANT
import org.openrewrite.java.tree.TypeUtils

class KotlinIrTypeMapping(typeCache: JavaTypeCache): JavaTypeMapping<Any> {
    private val signatureBuilder: KotlinTypeIrSignatureBuilder = KotlinTypeIrSignatureBuilder()
    private val typeCache: JavaTypeCache

    init {
        this.typeCache = typeCache
    }

    // TODO: temp to map types without parser visitor.
    fun type(irFile: IrFile) {
        for (ann in irFile.annotations) {
            type(ann)
        }
        for (dec in irFile.declarations) {
            type(dec)
        }
    }

    override fun type(type: Any?): JavaType {
        if (type == null || type is IrErrorType) {
            return JavaType.Unknown.getInstance()
        }

        if (type is IrSimpleType) {
            return type(type.classifier)
        }

        if (type is IrClassifierSymbol) {
            return type(type.owner)
        }

        val signature = signatureBuilder.signature(type)
        val existing: JavaType? = typeCache.get(signature)
        if (existing != null) {
            return existing
        }

        when (type) {
            is IrFile -> {
                return fileType(signature)
            }
            is IrClass -> {
               return classType(type, signature)
            }

            is IrFunction -> {
                return methodDeclarationType(type)
            }

            is IrProperty -> {
                return variableType(type)
            }

            is IrVariable -> {
                TODO("IrVariable not implemented")
            }

            is IrField -> {
                TODO("IrField not implemented")
            }

            is IrTypeParameter -> {
               return generic(type, signature)
            }
        }

        throw UnsupportedOperationException("Unsupported type: ${type.javaClass}")
    }

    private fun fileType(signature: String): JavaType {
        val existing = typeCache.get<JavaType.FullyQualified>(signature)
        if (existing != null) {
            return existing
        }
        val fileType = JavaType.ShallowClass.build(signature)
        typeCache.put(signature, fileType)
        return fileType
    }

    private fun classType(irClass: IrClass, signature: String): JavaType {
        val fqn: String = signatureBuilder.classSignature(irClass)
        val fq: JavaType.FullyQualified? = typeCache.get(fqn)
        var clazz: JavaType.Class? = (if (fq is JavaType.Parameterized) fq.type else fq) as JavaType.Class?
        if (clazz == null) {
            clazz = JavaType.Class(
                null,
                mapToFlagsBitmap(irClass.visibility, irClass.modality),
                fqn,
                mapKind(irClass.kind),
                null, null, null, null, null, null, null
            )

            typeCache.put(fqn, clazz)

            var supertype: JavaType.FullyQualified? = null
            var interfaceSymbols: MutableList<IrSymbolOwner>? = null
            for (sType in irClass.superTypes) {
                when (val classifier: IrClassifierSymbol? = sType.classifierOrNull) {
                    is IrClassSymbol -> {
                        when (classifier.owner.kind) {
                            ClassKind.CLASS -> {
                                supertype = TypeUtils.asFullyQualified(type(classifier.owner))
                            }
                            ClassKind.INTERFACE -> {
                                if (interfaceSymbols == null) {
                                    interfaceSymbols = ArrayList()
                                }
                                interfaceSymbols.add(classifier.owner)
                            }
                            else -> {
                                TODO()
                            }
                        }
                    }
                    else -> {
                        TODO()
                    }
                }
            }
            var owner: JavaType.FullyQualified? = null
            if (irClass.parent is IrClass) {
                owner = TypeUtils.asFullyQualified(type(irClass.parent))
            }
            var fields: MutableList<JavaType.Variable>? = null
            for (property: IrProperty in irClass.properties) {
                if (fields == null) {
                    fields = ArrayList(irClass.properties.toList().size)
                }
                val vt = variableType(property)
                fields.add(vt)
            }

            var methods: MutableList<JavaType.Method>? = null
            for (function: IrFunction in irClass.functions) {
                if (methods == null) {
                    methods = ArrayList(irClass.functions.toList().size)
                }
                val mt = methodDeclarationType(function)
                methods.add(mt)
            }

            var interfaces: MutableList<JavaType.FullyQualified>? = null
            if (!interfaceSymbols.isNullOrEmpty()) {
                interfaces = ArrayList(interfaceSymbols.size)
                for (interfaceSymbol: IrSymbolOwner in interfaceSymbols) {
                    val sym: Any = if (interfaceSymbol is Fir2IrLazyClass) interfaceSymbol.symbol.owner.symbol else interfaceSymbol
                    val javaType = TypeUtils.asFullyQualified(type(sym))
                    if (javaType != null) {
                        interfaces.add(javaType)
                    }
                }
            }
            clazz.unsafeSet(null, supertype, owner, listAnnotations(irClass.annotations), interfaces, fields, methods)
        }

        if (irClass.typeParameters.isNotEmpty()) {
            var pt = typeCache.get<JavaType.Parameterized>(signature)
            if (pt == null) {
                pt = JavaType.Parameterized(null, null, null)
                val typeParameters: MutableList<JavaType> = ArrayList(irClass.typeParameters.size)
                for (typeParam: IrTypeParameter in irClass.typeParameters) {
                    typeParameters.add(type(typeParam))
                }
                pt.unsafeSet(clazz, typeParameters)
            }
            return pt
        }
        return clazz
    }

    private fun generic(type: IrTypeParameter, signature: String): JavaType {
        val name = type.name.asString()
        val gtv: JavaType.GenericTypeVariable = JavaType.GenericTypeVariable(null, name, INVARIANT, null)
        typeCache.put(signature, gtv)

        var bounds: MutableList<JavaType>? = null
        if (type.isReified) {
            TODO()
        }
        for (bound: IrType in type.superTypes) {
            if (isNotAny(bound)) {
                if (bounds == null) {
                    bounds = ArrayList()
                }
                bounds.add(type(bound))
            }
        }
        gtv.unsafeSet(gtv.name, if (bounds == null) INVARIANT else COVARIANT, bounds)
        return gtv
    }

    private fun methodDeclarationType(function: IrFunction): JavaType.Method {
        val signature = signatureBuilder.methodDeclarationSignature(function)
        val existing = typeCache.get<JavaType.Method>(signature)
        if (existing != null) {
            return existing
        }

        val paramNames: MutableList<String>? = if (function.valueParameters.isEmpty()) null else ArrayList(function.valueParameters.size)
        for (param: IrValueParameter in function.valueParameters) {
            paramNames!!.add(param.name.asString())
        }
        val method = JavaType.Method(
            null,
            mapToFlagsBitmap(function.visibility),
            null,
            if (function is IrConstructor) "<constructor>" else function.name.asString(),
            null,
            paramNames,
            null, null, null, null
        )
        typeCache.put(signature, method)
        val declaringType = TypeUtils.asFullyQualified(type(function.parent))
        if (declaringType == null) {
            TODO()
        }
        val returnType = type(function.returnType)
        val paramTypes: MutableList<JavaType>? = if (function.valueParameters.isEmpty()) null else ArrayList(function.valueParameters.size)
        for (param: IrValueParameter in function.valueParameters) {
            paramTypes!!.add(type(param.type))
        }
        method.unsafeSet(
            declaringType,
            if (function is IrConstructor) declaringType else returnType,
            paramTypes, null, listAnnotations(function.annotations)
        )
        return method
    }

    fun variableType(
        property: IrProperty
    ): JavaType.Variable {
        val signature = signatureBuilder.variableSignature(property)
        val existing = typeCache.get<JavaType.Variable>(signature)
        if (existing != null) {
            return existing
        }
        val variable = JavaType.Variable(
            null,
            0,
            property.name.asString(),
            null, null, null
        )
        typeCache.put(signature, variable)
        val annotations = listAnnotations(property.annotations)
        val owner = type(property.parent)
        val typeRef = if (property.getter != null) {
            type(property.getter!!.returnType)
        } else if (property.backingField != null) {
            type(property.backingField!!.type)
        } else {
            TODO()
        }
        variable.unsafeSet(owner, typeRef, annotations)
        return variable
    }

    private fun listAnnotations(annotations: List<IrConstructorCall>): List<JavaType.FullyQualified> {
        val mapped: MutableList<JavaType.FullyQualified> = ArrayList(annotations.size)
        for (annotation: IrConstructorCall in annotations) {
            val type = TypeUtils.asFullyQualified(type(annotation.type))
            if (type != null) {
                mapped.add(type)
            }
        }
        return mapped.toList()
    }

    private fun mapKind(kind: ClassKind): JavaType.FullyQualified.Kind {
        return when (kind) {
            ClassKind.INTERFACE -> JavaType.FullyQualified.Kind.Interface
            ClassKind.ENUM_CLASS -> JavaType.FullyQualified.Kind.Enum
            ClassKind.ENUM_ENTRY -> TODO()
            ClassKind.ANNOTATION_CLASS -> JavaType.FullyQualified.Kind.Annotation
            else -> JavaType.FullyQualified.Kind.Class
        }
    }

    private fun mapToFlagsBitmap(visibility: DescriptorVisibility): Long {
        return mapToFlagsBitmap(visibility, null)
    }

    private fun mapToFlagsBitmap(visibility: DescriptorVisibility, modality: Modality?): Long {
        var bitMask: Long = 0

        when (visibility.externalDisplayName.lowercase()) {
            "public" -> bitMask += 1L
            "private" -> bitMask += 1L shl 1
            "protected" -> bitMask += 1L shl 2
            "internal", "package-private" -> {}
            else -> {
                throw UnsupportedOperationException("Unsupported visibility: ${visibility.name.lowercase()}")
            }
        }

        if (modality != null) {
            bitMask += when (modality.name.lowercase()) {
                "final" -> 1L shl 4
                "abstract" -> 1L shl 10
                "sealed" -> 1L shl 62
                "open" -> 0
                else -> {
                    throw UnsupportedOperationException("Unsupported modality: ${modality.name.lowercase()}")
                }
            }
        }
        return bitMask
    }

    private fun isNotAny(type: IrType): Boolean {
        return !(type.classifierOrNull != null && type.classifierOrNull!!.owner is IrClass && "kotlin.Any" == (type.classifierOrNull!!.owner as IrClass).kotlinFqName.asString())
    }
}