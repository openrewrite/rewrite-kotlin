/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiErrorElementImpl;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.declarations.FirFunction;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.expressions.FirConstExpression;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol;
import org.jetbrains.kotlin.fir.types.ConeClassLikeType;
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.parsing.ParseUtilsKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.Tree;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinTypeMapping;
import org.openrewrite.kotlin.marker.KObject;
import org.openrewrite.kotlin.marker.OmitBraces;
import org.openrewrite.kotlin.marker.TypeReferencePrefix;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * PSI based parser
 */
@SuppressWarnings("UnstableApiUsage")
public class KotlinTreeParser extends KtVisitor<J, ExecutionContext> {
    private final KotlinSource kotlinSource;
    private final KotlinTypeMapping typeMapping;
    private final PsiElementAssociations psiElementAssociations;
    private final List<NamedStyles> styles;
    private final Path sourcePath;
    private final FileAttributes fileAttributes;
    private final Charset charset;
    private final Boolean charsetBomMarked;
    @Nullable
    private final FirFile currentFile;
    private final ExecutionContext executionContext;

    public KotlinTreeParser(KotlinSource kotlinSource,
                            KotlinTypeMapping typeMapping,
                            PsiElementAssociations psiElementAssociations,
                            List<NamedStyles> styles,
                            @Nullable Path relativeTo,
                            ExecutionContext ctx) {
        this.kotlinSource = kotlinSource;
        this.typeMapping = typeMapping;
        this.psiElementAssociations = psiElementAssociations;
        this.styles = styles;
        sourcePath = kotlinSource.getInput().getRelativePath(relativeTo);
        fileAttributes = kotlinSource.getInput().getFileAttributes();
        EncodingDetectingInputStream stream = kotlinSource.getInput().getSource(ctx);
        charset = stream.getCharset();
        charsetBomMarked = stream.isCharsetBomMarked();
        currentFile = kotlinSource.getFirFile();
        executionContext = ctx;
    }

    public K.CompilationUnit parse() {
        return (K.CompilationUnit) visitKtFile(kotlinSource.getKtFile(), executionContext);
    }

    @Override
    public J visitKtElement(KtElement element, ExecutionContext data) {
        IElementType type = element.getNode().getElementType();

        if (type == KtNodeTypes.KT_FILE)
            return element.accept(this, data);
        else if (type == KtNodeTypes.PROPERTY)
            return element.accept(this, data);
        else if (type == KtNodeTypes.INTEGER_CONSTANT)
            return element.accept(this, data);
        else if (type == KtNodeTypes.BINARY_EXPRESSION)
            return element.accept(this, data);
        else
            throw new UnsupportedOperationException("Unsupported PSI type " + type);
    }

    /*====================================================================
     * PSI to J tree mapping methods
     * ====================================================================*/
    @Override
    public J visitKtFile(KtFile file, ExecutionContext data) {
        List<J.Annotation> annotations = new ArrayList<>();
        @Nullable JRightPadded<J.Package> packageDeclaration = null;
        List<JRightPadded<J.Import>> imports = new ArrayList<>();
        List<JRightPadded<Statement>> statements = new ArrayList<>();

        if (file.getPackageDirective() != null && file.getPackageDirective().getPackageNameExpression() != null) {
            throw new UnsupportedOperationException("TODO");
        } else if (!file.getImportDirectives().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        } else if (!file.getAnnotationEntries().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        }

        List<KtDeclaration> declarations = file.getDeclarations();
        for (int i = 0; i < declarations.size(); i++) {
            boolean last = i == declarations.size() - 1;
            KtDeclaration declaration = declarations.get(i);
            Statement statement = convertToStatement(declaration.accept(this, data).withPrefix(prefix(declaration)));
            statements.add(padRight(statement, last ? suffix(declaration) : Space.EMPTY));
        }

        return new K.CompilationUnit(
                Tree.randomId(),
                Space.EMPTY,
                Markers.build(styles),
                sourcePath,
                fileAttributes,
                charset.name(),
                charsetBomMarked,
                null,
                annotations,
                packageDeclaration,
                imports,
                statements,
                Space.EMPTY
        );
    }

    @Override
    public J visitAnnotation(@NotNull KtAnnotation annotation, ExecutionContext data) {
        throw new UnsupportedOperationException("KtAnnotation");
    }

    @Override
    public J visitBinaryExpression(KtBinaryExpression expression, ExecutionContext data) {
        return new J.Binary(
                randomId(),
                space(expression.getFirstChild()),
                Markers.EMPTY,
                convertToExpression(expression.getLeft().accept(this, data)),
                padLeft(prefix(expression.getOperationReference()), mapBinaryType(expression.getOperationReference())),
                convertToExpression((expression.getRight()).accept(this, data))
                        .withPrefix(prefix(expression.getRight())),
                type(expression)
        );
    }

    @Override
    public J visitBlockExpression(@NotNull KtBlockExpression expression, ExecutionContext data) {
        List<JRightPadded<Statement>> statements = expression.getStatements().stream()
                .map(s -> s.accept(this, data))
                .map(this::convertToStatement)
                .map(JRightPadded::build)
                .collect(Collectors.toList());

        return new J.Block(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY, // todo, maybe(OmitBraces(randomId())),
                JRightPadded.build(false),
                statements,
                prefix(expression.getRBrace())
        );
    }

    @Override
    public J visitConstantExpression(KtConstantExpression expression, ExecutionContext data) {
        IElementType elementType = expression.getElementType();
        Object value;
        if (elementType == KtNodeTypes.INTEGER_CONSTANT || elementType == KtNodeTypes.FLOAT_CONSTANT) {
            value = ParseUtilsKt.parseNumericLiteral(expression.getText(), elementType);
        } else if (elementType == KtNodeTypes.BOOLEAN_CONSTANT) {
            value = ParseUtilsKt.parseBoolean(expression.getText());
        } else if (elementType == KtNodeTypes.CHARACTER_CONSTANT) {
            value = expression.getText().charAt(0);
        } else if (elementType == KtNodeTypes.NULL) {
            value = null;
        } else {
            throw new UnsupportedOperationException("Unsupported constant expression elementType : " + elementType);
        }
        return new J.Literal(
                Tree.randomId(),
                prefix(expression),
                Markers.EMPTY,
                value,
                expression.getText(),
                null,
                primitiveType(expression)
        );
    }

    @Override
    public J visitClass(@NotNull KtClass klass, ExecutionContext data) {
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = new ArrayList<>();
        JContainer<J.TypeParameter> typeParams = null;
        JContainer<TypeTree> implementings = null;

        if (klass.getModifierList() != null) {
            PsiElement child = klass.getModifierList().getFirstChild();
            while (child != null) {
                if (!isSpace(child.getNode())) {
                    modifiers.add(new J.Modifier(randomId(), prefix(child), Markers.EMPTY, child.getText(), mapModifierType(child), emptyList())
                    );
                }
                child = child.getNextSibling();
            }
        }
        if (!klass.hasModifier(KtTokens.OPEN_KEYWORD)) {
            modifiers.add(
                    new J.Modifier(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            null,
                            J.Modifier.Type.Final,
                            emptyList()
                    )
            );
        }

        J.ClassDeclaration.Kind kind;
        if (klass.getClassKeyword() != null) {
            kind = new J.ClassDeclaration.Kind(
                    randomId(),
                    prefix(klass.getClassKeyword()),
                    Markers.EMPTY,
                    emptyList(),
                    J.ClassDeclaration.Kind.Type.Class
            );
        } else {
            throw new UnsupportedOperationException("TODO");
        }

        J.Identifier name = createIdentifier(klass.getIdentifyingElement(), type(klass));

        J.Block body;
        if (klass.getBody() != null) {
            body = (J.Block) klass.getBody().accept(this, data);
        } else {
            body = new J.Block(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY.add(new OmitBraces(randomId())),
                    padRight(false, Space.EMPTY),
                    emptyList(),
                    Space.EMPTY
            );
        }

        if (klass.getPrimaryConstructor() != null) {
            throw new UnsupportedOperationException("TODO");
        } else if (!klass.getSuperTypeListEntries().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        } else if (!klass.getTypeParameters().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        }

        return new J.ClassDeclaration(
                randomId(),
                prefix(klass),
                Markers.EMPTY,
                leadingAnnotations,
                modifiers,
                kind,
                name,
                typeParams,
                null,
                null,
                implementings,
                null,
                body,
                (JavaType.FullyQualified) type(klass)
        );
    }

    @Override
    public J visitClassBody(@NotNull KtClassBody classBody, ExecutionContext data) {
        return new J.Block(
                randomId(),
                prefix(classBody),
                Markers.EMPTY,
                padRight(false, Space.EMPTY),
                classBody.getDeclarations().stream()
                        .map(d -> d.accept(this, data).withPrefix(prefix(d)))
                        .map(J.class::cast)
                        .map(this::convertToStatement)
                        .map(JRightPadded::build)
                        .collect(Collectors.toList()),
                prefix(classBody.getRBrace())
        );
    }

    @Override
    public J visitIfExpression(@NotNull KtIfExpression expression, ExecutionContext data) {
        return new J.If(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                buildIfCondition(expression),
                buildIfThenPart(expression),
                buildIfElsePart(expression)
        );
    }

    @Override
    public J visitNamedFunction(@NotNull KtNamedFunction function, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = new ArrayList<>();
        J.TypeParameters typeParameters = null;
        TypeTree returnTypeExpression = null;

        if (function.getModifierList() != null) {
            throw new UnsupportedOperationException("TODO");
        } else if (function.getTypeReference() != null) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean hasTypeReference = PsiTreeUtil.getChildOfType(function, KtTypeReference.class) != null;
        if (hasTypeReference) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean isOpen = false; // TODO
        if (!isOpen) {
            modifiers.add(
                    new J.Modifier(
                            randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            null,
                            J.Modifier.Type.Final,
                            emptyList()
                    )
            );
        }

        modifiers.add(
                new J.Modifier(
                        randomId(),
                        prefix(function.getFunKeyword()),
                        Markers.EMPTY,
                        "fun",
                        J.Modifier.Type.LanguageExtension,
                        emptyList()
                )
        );

        if (function.getNameIdentifier() == null) {
            throw new UnsupportedOperationException("TODO");
        }

        J.Identifier name = createIdentifier(function.getNameIdentifier(), type(function));

        // parameters
        JContainer<Statement> params;
        List<KtParameter> ktParameters = function.getValueParameters();

        if (ktParameters.isEmpty()) {
            params = JContainer.build(prefix(function.getValueParameterList()),
                    singletonList(padRight(new J.Empty(randomId(),
                            prefix(function.getValueParameterList().getRightParenthesis()),
                            Markers.EMPTY),
                            Space.EMPTY)
                    ), Markers.EMPTY
            );
        } else {
              throw new UnsupportedOperationException("TODO");
        }

        if (function.getBodyBlockExpression() == null) {
            throw new UnsupportedOperationException("TODO");
        }
        J.Block body = function.getBodyBlockExpression().accept(this, data)
                .withPrefix(prefix(function.getBodyBlockExpression()));
        JavaType.Method methodType = methodDeclarationType(function);

        return new J.MethodDeclaration(
                randomId(),
                Space.EMPTY,
                markers,
                leadingAnnotations,
                modifiers,
                typeParameters,
                returnTypeExpression,
                new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                params,
                null,
                body,
                null,
                methodType
        );
    }

    @Override
    public J visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expression, ExecutionContext data) {
        return expression.getObjectDeclaration().accept(this, data).withPrefix(prefix(expression));
    }

    @Override
    public J visitObjectDeclaration(@NotNull KtObjectDeclaration declaration, ExecutionContext data) {
        TypeTree clazz = null;
        Markers markers = Markers.EMPTY;
        JContainer<Expression> args = null;

        if (declaration.getSuperTypeList() == null) {
            throw new UnsupportedOperationException("TODO");
        }

        KtValueArgumentList ktArgs = declaration.getSuperTypeList().getEntries().get(0).getStubOrPsiChild(KtStubElementTypes.VALUE_ARGUMENT_LIST);

        if (ktArgs != null && ktArgs.getArguments().isEmpty()) {
            args = JContainer.build(
                    prefix(ktArgs),
                    singletonList(padRight(new J.Empty(randomId(), prefix(ktArgs.getRightParenthesis()), Markers.EMPTY), Space.EMPTY)
                    ), Markers.EMPTY
            );
        } else {
            throw new UnsupportedOperationException("TODO, support multiple ObjectDeclaration arguments");
        }

        J.Block body = (J.Block) declaration.getBody().accept(this, data);

        if (declaration.getObjectKeyword() != null) {
            markers = markers.add(new KObject(randomId(), Space.EMPTY));
            markers = markers.add(new TypeReferencePrefix(randomId(), prefix(declaration.getColon())));
        }

        clazz = (TypeTree) declaration.getSuperTypeList().accept(this, data);

        return new J.NewClass(
                randomId(),
                Space.EMPTY,
                markers,
                null,
                prefix(declaration.getSuperTypeList()),
                clazz,
                args,
                body,
                null
        );
    }

    @Override
    public J visitProperty(KtProperty property, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        TypeTree typeExpression = null;
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = new ArrayList<>();

        if (property.getModifierList() != null) {
            throw new UnsupportedOperationException("TODO");
        }

        J.Modifier modifier = new J.Modifier(
                Tree.randomId(),
                prefix(property.getValOrVarKeyword()),
                Markers.EMPTY,
                property.isVar() ? "var" : null,
                property.isVar() ? J.Modifier.Type.LanguageExtension : J.Modifier.Type.Final,
                Collections.emptyList() // FIXME
        );

        JLeftPadded<Expression> initializer = property.getInitializer() != null ?
                padLeft(prefix(property.getEqualsToken()),
                        convertToExpression(property.getInitializer().accept(this, data)
                                .withPrefix(prefix(property.getInitializer()))))
                : null;

        J.VariableDeclarations.NamedVariable namedVariable =
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        createIdentifier(property.getNameIdentifier(), type(property)),
                        emptyList(),
                        initializer,
                        variableType(property)
                );

        variables.add(padRight(namedVariable, Space.EMPTY));

        if (property.getColon() != null) {
            markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), prefix(property.getColon())));
            typeExpression = (TypeTree) property.getTypeReference().accept(this, data);
            typeExpression = typeExpression.withPrefix(suffix(property.getColon()));
        }

        if (property.getGetter() != null || property.getSetter() != null) {
            throw new UnsupportedOperationException("TODO");
        } else if (property.getLastChild().getNode().getElementType() == KtTokens.SEMICOLON) {
            throw new UnsupportedOperationException("TODO");
        } else if (!property.getAnnotationEntries().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        }

        if (PsiTreeUtil.getChildOfType(property, KtPropertyDelegate.class) != null) {
            throw new UnsupportedOperationException("TODO");
        }

        return new J.VariableDeclarations(
                Tree.randomId(),
                Space.EMPTY, // overlaps with right-padding of previous statement
                markers,
                leadingAnnotations,
                singletonList(modifier),
                typeExpression,
                null,
                Collections.emptyList(),
                variables
        );
    }

    @Override
    public J visitPropertyDelegate(@NotNull KtPropertyDelegate delegate, ExecutionContext data) {
        throw new UnsupportedOperationException("Unsupported KtPropertyDelegate");
    }

    @Override
    public J visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression, ExecutionContext data) {
        return createIdentifier(expression, type(expression));
    }

    @Override
    public J visitStringTemplateExpression(@NotNull KtStringTemplateExpression expression, ExecutionContext data) {
        KtStringTemplateEntry[] entries = expression.getEntries();
        if (entries.length > 1) {
            throw new UnsupportedOperationException("Unsupported constant expression elementType, TODO");
        }

        if (entries.length == 0) {
            return new J.Literal(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    "",
                    expression.getText(),
                    null,
                    primitiveType(expression)
            );
        }
        return entries[0].accept(this, data).withPrefix(prefix(expression));
    }

    @Override
    public J visitStringTemplateEntry(@NotNull KtStringTemplateEntry entry, ExecutionContext data) {
        PsiElement leaf = entry.getFirstChild();
        if (!(leaf instanceof LeafPsiElement)) {
            throw new UnsupportedOperationException("Unsupported KtStringTemplateEntry child");
        }

        return new J.Literal(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                leaf.getText(),
                "\"" + leaf.getText() + "\"", // todo, support text block
                null,
                primitiveType(entry)
        );
    }

    @Override
    public J visitSuperTypeList(@NotNull KtSuperTypeList list, ExecutionContext data) {
        List<KtSuperTypeListEntry> typeListEntries = list.getEntries();

        if (typeListEntries.size() > 1) {
            throw new UnsupportedOperationException("KtSuperTypeList size is bigger than 1, TODO");
        }
        return typeListEntries.get(0).accept(this, data);
    }

    @Override
    public J visitSuperTypeCallEntry(@NotNull KtSuperTypeCallEntry call, ExecutionContext data) {
        return call.getTypeReference().accept(this, data);
    }

    @Override
    public J visitTypeReference(@NotNull KtTypeReference typeReference, ExecutionContext data) {
        return typeReference.getTypeElement().accept(this, data);
    }

    @Override
    public J visitUserType(@NotNull KtUserType type, ExecutionContext data) {
        return type.getReferenceExpression().accept(this, data);
    }

    /*====================================================================
     * Mapping methods
     * ====================================================================*/
    private J.Binary.Type mapBinaryType(KtOperationReferenceExpression operationReference) {
        IElementType elementType = operationReference.getOperationSignTokenType();
        if (elementType == KtTokens.PLUS)
            return J.Binary.Type.Addition;
        else if (elementType == KtTokens.MINUS)
            return J.Binary.Type.Subtraction;
        else if (elementType == KtTokens.MUL)
            return J.Binary.Type.Multiplication;
        else if (elementType == KtTokens.DIV)
            return J.Binary.Type.Division;
        else
            throw new UnsupportedOperationException("Unsupported OPERATION_REFERENCE type :" + elementType);
    }

    private J.Modifier.Type mapModifierType(PsiElement modifier) {
        switch (modifier.getText()) {
            case "public":
                return J.Modifier.Type.Public;
            case "private":
                return J.Modifier.Type.Private;
            case "sealed":
                return J.Modifier.Type.Sealed;
            case "open":
                return J.Modifier.Type.LanguageExtension;
            default:
                throw new UnsupportedOperationException("Unsupported ModifierType : " + modifier);
        }
    }

    private J.ControlParentheses<Expression> buildIfCondition(KtIfExpression expression) {
        return new J.ControlParentheses<>(randomId(),
                prefix(expression.getLeftParenthesis()),
                Markers.EMPTY,
                padRight(convertToExpression(expression.getCondition().accept(this, executionContext))
                                .withPrefix(suffix(expression.getLeftParenthesis())),
                        prefix(expression.getRightParenthesis()))
        );
    }

    private JRightPadded<Statement> buildIfThenPart(KtIfExpression expression) {
        return padRight(convertToStatement(expression.getThen().accept(this, executionContext))
                        .withPrefix(prefix(expression.getThen().getParent())),
                Space.EMPTY);
    }

    @Nullable
    private J.If.Else buildIfElsePart(KtIfExpression expression) {
        if (expression.getElse() == null) {
            return null;
        }

        return new J.If.Else(
                randomId(),
                prefix(expression.getElseKeyword()),
                Markers.EMPTY,
                padRight(convertToStatement(expression.getElse().accept(this, executionContext))
                        .withPrefix(suffix(expression.getElseKeyword())), Space.EMPTY)
        );
    }

    /*====================================================================
     * Type related methods
     * ====================================================================*/
    @Nullable
    private JavaType type(PsiElement psi) {
        return psiElementAssociations.type(psi, currentFile.getSymbol());
    }

    private JavaType.Primitive primitiveType(PsiElement expression) {
        return  typeMapping.primitive((ConeClassLikeType) ((FirResolvedTypeRef) ((FirConstExpression<?>) psiElementAssociations.primary(expression)).getTypeRef()).getType());
    }

    @Nullable
    private JavaType.Variable variableType(PsiElement psi) {
        if (psi instanceof KtDeclaration) {
            FirBasedSymbol basedSymbol = psiElementAssociations.symbol((KtDeclaration) psi);
            if (basedSymbol instanceof FirVariableSymbol) {
                FirVariableSymbol<? extends FirVariable> variableSymbol = (FirVariableSymbol<? extends FirVariable>) basedSymbol;
                return psiElementAssociations.getTypeMapping().variableType(variableSymbol, null, getCurrentFile());
            }
        }
        return null;
    }

    @Nullable
    private JavaType.Method methodDeclarationType(PsiElement psi) {
        if (psi instanceof KtNamedFunction) {
            FirBasedSymbol basedSymbol = psiElementAssociations.symbol((KtNamedFunction) psi);
            if (basedSymbol instanceof FirNamedFunctionSymbol) {
                FirNamedFunctionSymbol functionSymbol = (FirNamedFunctionSymbol) basedSymbol;
                return psiElementAssociations.getTypeMapping().methodDeclarationType(functionSymbol.getFir(), null, getCurrentFile());
            }
        }
        return null;
    }

    /*====================================================================
     * Other helper methods
     * ====================================================================*/
    private J.Identifier createIdentifier(PsiElement name, JavaType type) {
        return createIdentifier(name.getNode().getText(), prefix(name), type);
    }

    private J.Identifier createIdentifier(String name, Space prefix, @Nullable JavaType type) {
        return createIdentifier(name, prefix,
                type instanceof JavaType.Variable ? ((JavaType.Variable) type).getType() : type,
                type instanceof JavaType.Variable ? (JavaType.Variable) type : null);
    }

    private J.Identifier createIdentifier(String name, Space prefix, @Nullable JavaType
            type, @Nullable JavaType.Variable fieldType) {
        return new J.Identifier(
                randomId(),
                prefix,
                Markers.EMPTY,
                emptyList(),
                name,
                type,
                fieldType
        );
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private <T> JRightPadded<T> padRight(T tree, @Nullable Space right) {
        return new JRightPadded<>(tree, right == null ? Space.EMPTY : right, Markers.EMPTY);
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> J2 convertToExpression(J j) {
        if (j instanceof Statement && !(j instanceof Expression)) {
            j = new K.StatementExpression(randomId(), (Statement) j);
        }
        return (J2) j;
    }

    @SuppressWarnings("DataFlowIssue")
    private Statement convertToStatement(J j) {
        if (!(j instanceof Statement) && j instanceof Expression) {
            j = new K.ExpressionStatement(randomId(), (Expression) j);
        }
        return (Statement) j;
    }

    private Space prefix(@Nullable PsiElement element) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement whitespace = element.getPrevSibling();
        if (whitespace == null || !isSpace(whitespace.getNode())) {
            return Space.EMPTY;
        }
        while (whitespace.getPrevSibling() != null && isSpace(whitespace.getPrevSibling().getNode())) {
            whitespace = whitespace.getPrevSibling();
        }
        return space(whitespace);
    }

    private Space suffix(@Nullable PsiElement element) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement whitespace = element.getLastChild();
        if (whitespace == null || !isSpace(whitespace.getNode())) {
            whitespace = element.getNextSibling();
        } else {
            while (whitespace.getPrevSibling() != null && isSpace(whitespace.getPrevSibling().getNode())) {
                whitespace = whitespace.getPrevSibling();
            }
        }
        if (whitespace == null || !isSpace(whitespace.getNode())) {
            return Space.EMPTY;
        }
        return space(whitespace);
    }

    private boolean isSpace(ASTNode node) {
        IElementType elementType = node.getElementType();
        return elementType == KtTokens.WHITE_SPACE ||
                elementType == KtTokens.BLOCK_COMMENT ||
                elementType == KtTokens.EOL_COMMENT ||
                elementType == KtTokens.DOC_COMMENT ||
                isCRLF(node);
    }

    private boolean isWhiteSpace(@Nullable PsiElement node) {
        if (node == null) {
            return false;
        }
        return node instanceof PsiWhiteSpace || isCRLF(node.getNode());
    }

    private boolean isCRLF(ASTNode node) {
        return node instanceof PsiErrorElementImpl && node.getText().equals("\r");
    }

    private Space space(PsiElement node) {
        Space space = null;
        PsiElement preNode = null;

        for (; node != null; node = next(node)) {
            PsiElement finalNode = node;
            if (isWhiteSpace(node)) {
                if (space == null) {
                    space = Space.build(node.getText(), emptyList());
                } else {
                    if (isWhiteSpace(preNode)) {
                        // merge space
                        space = space.withWhitespace(space.getWhitespace() + node.getText());
                    } else {
                        space = space.withComments(ListUtils.mapLast(space.getComments(), c -> c.withSuffix(finalNode.getText())));
                    }
                }
            } else if (node instanceof PsiComment) {
                if (space == null) {
                    space = Space.EMPTY;
                }
                String nodeText = node.getText();
                boolean isBlockComment = ((PsiComment) node).getTokenType() == KtTokens.BLOCK_COMMENT;
                String comment = isBlockComment ? nodeText.substring(2, nodeText.length() - 2) : nodeText.substring(2);
                space = space.withComments(ListUtils.concat(space.getComments(), new TextComment(isBlockComment, comment, "", Markers.EMPTY)));
            } else {
                break;
            }

            preNode = node;
        }
        return space == null ? Space.EMPTY : space;
    }

    @Nullable
    private PsiElement prev(PsiElement node) {
        return PsiTreeUtil.prevLeaf(node);
    }

    @Nullable
    private PsiElement next(PsiElement node) {
        return PsiTreeUtil.nextLeaf(node);
    }


    @Nullable
    private FirBasedSymbol getCurrentFile() {
        return currentFile != null ? currentFile.getSymbol() : null;
    }
}
