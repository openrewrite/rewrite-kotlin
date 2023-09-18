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
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.parsing.ParseUtilsKt;
import org.jetbrains.kotlin.psi.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.Tree;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
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
    private final PsiElementAssociations psiElementAssociations;
    private final List<NamedStyles> styles;
    private final Path sourcePath;
    private final FileAttributes fileAttributes;
    private final Charset charset;
    private final Boolean charsetBomMarked;
    @Nullable
    private final FirFile currentFile;

    public KotlinTreeParser(KotlinSource kotlinSource,
                            PsiElementAssociations psiElementAssociations,
                            List<NamedStyles> styles,
                            @Nullable Path relativeTo,
                            ExecutionContext ctx) {
        this.kotlinSource = kotlinSource;
        this.psiElementAssociations = psiElementAssociations;
        this.styles = styles;
        sourcePath = kotlinSource.getInput().getRelativePath(relativeTo);
        fileAttributes = kotlinSource.getInput().getFileAttributes();
        EncodingDetectingInputStream stream = kotlinSource.getInput().getSource(ctx);
        charset = stream.getCharset();
        charsetBomMarked = stream.isCharsetBomMarked();
        currentFile = kotlinSource.getFirFile();
    }

    public K.CompilationUnit parse(ExecutionContext ctx) {
        return (K.CompilationUnit) visitKtFile(kotlinSource.getKtFile(), ctx);
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
            if (declaration instanceof KtProperty || declaration instanceof KtClass) {
                Statement statement = (Statement) declaration.accept(this, data);
                statement = statement.withPrefix(prefix(declaration));
                statements.add(padRight(statement, last ? suffix(declaration) : Space.EMPTY));
            } else {
                throw new UnsupportedOperationException("Unsupported PSI type :" + declaration.getNode().getElementType());
            }
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
    public J visitClass(@NotNull KtClass klass, ExecutionContext data) {

        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = new ArrayList<>();
        JContainer<J.TypeParameter> typeParams = null;
        JContainer<TypeTree> implementings = null;

        if (klass.getModifierList() != null) {
            PsiElement child = klass.getModifierList().getFirstChild();
            while (child != null) {
                if (!isWhitespace(child.getNode())) {
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

        J.ClassDeclaration.Kind kind = new J.ClassDeclaration.Kind(
                randomId(),
                prefix(klass.getClassKeyword()),
                Markers.EMPTY,
                emptyList(),
                J.ClassDeclaration.Kind.Type.Class
        );

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
        if (!classBody.getDeclarations().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        } else if (classBody.getLBrace() != null && classBody.getLBrace().getNextSibling() != classBody.getRBrace()) {
            throw new UnsupportedOperationException("TODO");
        }
        return new J.Block(
                randomId(),
                prefix(classBody),    // FIXME
                Markers.EMPTY,
                padRight(false, Space.EMPTY),
                classBody.getDeclarations().stream()
                        .map(d -> d.accept(this, data))
                        .map(Statement.class::cast)
                        .map(JRightPadded::build)
                        .collect(Collectors.toList()),
                Space.EMPTY
        );
    }

    @Override
    public J visitNamedFunction(@NotNull KtNamedFunction function, ExecutionContext data) {
        throw new UnsupportedOperationException("KtNamedFunction");
    }

    @Override
    public J visitAnnotation(@NotNull KtAnnotation annotation, ExecutionContext data) {
        throw new UnsupportedOperationException("KtAnnotation");
    }

    @Override
    public J visitProperty(KtProperty property, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        TypeTree typeExpression = null;
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = new ArrayList<>();

        J.Modifier modifier = new J.Modifier(
                Tree.randomId(),
                prefix(property.getValOrVarKeyword()),
                Markers.EMPTY,
                property.isVar() ? "var" : null,
                property.isVar() ? J.Modifier.Type.LanguageExtension : J.Modifier.Type.Final,
                Collections.emptyList() // FIXME
        );

        J.VariableDeclarations.NamedVariable namedVariable =
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        createIdentifier(property.getNameIdentifier(), type(property)),
                        emptyList(),
                        property.getInitializer() != null ?
                                padLeft(prefix(property.getEqualsToken()),
                                        property.getInitializer().accept(this, data)
                                                .withPrefix(prefix(property.getInitializer())))
                                : null,
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
    public J visitTypeReference(@NotNull KtTypeReference typeReference, ExecutionContext data) {
        return typeReference.getTypeElement().accept(this, data);
    }

    @Override
    public J visitUserType(@NotNull KtUserType type, ExecutionContext data) {
        return type.getReferenceExpression().accept(this, data);
    }

    @Override
    public J visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression, ExecutionContext data) {
        return createIdentifier(expression, type(expression));
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
            throw new IllegalArgumentException();
        }
        return new J.Literal(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                value,
                expression.getText(),
                null,
                JavaType.Primitive.Int
        );
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
            throw new IllegalArgumentException("Unsupported OPERATION_REFERENCE type :" + elementType.getDebugName());
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

    /*====================================================================
     * Type related methods
     * ====================================================================*/
    @Nullable
    private JavaType type(PsiElement psi) {
        return psiElementAssociations.type(psi, currentFile.getSymbol());
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

    private Space prefix(@Nullable PsiElement element) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement whitespace = element.getPrevSibling();
        if (whitespace == null || !isWhitespace(whitespace.getNode())) {
            return Space.EMPTY;
        }
        while (whitespace.getPrevSibling() != null && isWhitespace(whitespace.getPrevSibling().getNode())) {
            whitespace = whitespace.getPrevSibling();
        }
        return space(whitespace);
    }

    private Space suffix(@Nullable PsiElement element) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement whitespace = element.getLastChild();
        if (whitespace == null || !isWhitespace(whitespace.getNode())) {
            whitespace = element.getNextSibling();
        } else {
            while (whitespace.getPrevSibling() != null && isWhitespace(whitespace.getPrevSibling().getNode())) {
                whitespace = whitespace.getPrevSibling();
            }
        }
        if (whitespace == null || !isWhitespace(whitespace.getNode())) {
            return Space.EMPTY;
        }
        return space(whitespace);
    }

    private boolean isWhitespace(ASTNode node) {
        IElementType elementType = node.getElementType();
        return elementType == KtTokens.WHITE_SPACE || elementType == KtTokens.BLOCK_COMMENT || elementType == KtTokens.EOL_COMMENT || elementType == KtTokens.DOC_COMMENT;
    }

    private Space space(PsiElement node) {
        Space space = null;
        for (; node != null; node = next(node)) {
            PsiElement finalNode = node;
            if (node instanceof PsiWhiteSpace) {
                if (space == null) {
                    space = Space.build(node.getText(), emptyList());
                } else {
                    space = space.withComments(ListUtils.mapLast(space.getComments(), c -> c.withSuffix(finalNode.getText())));
                }
            } else if (node instanceof PsiComment) {
                if (space == null) {
                    space = Space.EMPTY;
                }
                String nodeText = node.getText();
                boolean isBlockComment = ((PsiComment) node).getTokenType() == KtTokens.BLOCK_COMMENT;
                String comment = isBlockComment ? nodeText.substring(2, nodeText.length() - 2) : nodeText.substring(2);
                space = space.withComments(ListUtils.concat(space.getComments(), new TextComment(true, comment, "", Markers.EMPTY)));
            } else {
                break;
            }
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
