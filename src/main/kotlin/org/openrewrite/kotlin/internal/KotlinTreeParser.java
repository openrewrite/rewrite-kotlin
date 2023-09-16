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

import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.Tree;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
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

        List<PsiElement> childNodes = getAllChildren(file);
        for (PsiElement child : childNodes) {
            IElementType elementType = child.getNode().getElementType();
            if (elementType == KtNodeTypes.PACKAGE_DIRECTIVE || elementType == KtNodeTypes.IMPORT_LIST) {
                // todo
            } else if (elementType == KtNodeTypes.PROPERTY) {
                J.VariableDeclarations v = (J.VariableDeclarations) ((KtElement) child).accept(this, data);
                statements.add(padRight(v, Space.EMPTY));
            } else {
                throw new UnsupportedOperationException("Unsupported child PSI type in kotlin.FILE :" + elementType);
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
    public J visitProperty(KtProperty property, ExecutionContext data) {
        Space prefix = Space.EMPTY;
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = new ArrayList<>();
        TypeTree typeExpression = null;
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = new ArrayList<>();

        List<PsiElement> childNodes = getAllChildren(property);
        Space space = null;
        boolean afterEQ = false;
        J.Identifier identifier = null;
        JLeftPadded<Expression> initializer = null;
        String identifierName;
        JavaType identifierType;
        Space spaceBeforeEQ = null;

        for (PsiElement child : childNodes) {
            String nodeText = child.getText();

            IElementType childType = child.getNode().getElementType();
            if (childType == KtTokens.VAL_KEYWORD) {
                modifiers.add(
                        new J.Modifier(
                                Tree.randomId(),
                                space != null ? space : Space.EMPTY,
                                Markers.EMPTY,
                                null,
                                J.Modifier.Type.Final,
                                Collections.emptyList() // FIXME
                        )
                );
                space = null;
            } else if (childType == KtTokens.VAR_KEYWORD) {
                modifiers.add(
                        new J.Modifier(
                                randomId(),
                                space != null ? space : Space.EMPTY,
                                Markers.EMPTY,
                                "var",
                                J.Modifier.Type.LanguageExtension,
                                Collections.emptyList() // FIXME
                        )
                );
                space = null;
            } else if (childType == KtTokens.WHITE_SPACE) {
                if (space == null) {
                    space = Space.build(nodeText, new ArrayList<>());
                } else {
                    space = space.withComments(ListUtils.mapLast(space.getComments(), c -> c.withSuffix(nodeText)));
                }
            } else if (childType == KtTokens.EOL_COMMENT || childType == KtTokens.BLOCK_COMMENT) {
                boolean isMultiLineComment = childType == KtTokens.BLOCK_COMMENT;
                String comment;
                // unwrap `//` or `/* */`
                comment = isMultiLineComment ? nodeText.substring(2, nodeText.length() - 2) : nodeText.substring(2);
                if (space == null) {
                    space = Space.build("", new ArrayList<>());
                }
                space = space.withComments(ListUtils.concat(space.getComments(), new TextComment(isMultiLineComment, comment, "", Markers.EMPTY)));
            } else if (childType == KtTokens.IDENTIFIER) {
                identifierName = nodeText;
                identifierType = type(property);
                identifier = createIdentifier(identifierName,
                        Optional.ofNullable(space).orElse(Space.EMPTY), identifierType);
                space = null;
            } else if (childType == KtTokens.EQ) {
                spaceBeforeEQ = space;
                space = null;
                afterEQ = true;
            } else {
                if (afterEQ) {
                    // build initializer
                    Expression exp = convertToExpression(((KtElement) child).accept(this, data));
                    initializer = padLeft(spaceBeforeEQ != null ? spaceBeforeEQ : Space.EMPTY,
                            exp.withPrefix(space != null ? space : Space.EMPTY));
                    space = null;
                    continue;
                }
                throw new UnsupportedOperationException("Unsupported child PSI type in PROPERTY :" + childType);
            }
        }

        J.VariableDeclarations.NamedVariable namedVariable =
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        identifier,
                        emptyList(),
                        initializer,
                        variableType(property)
                );

        variables.add(padRight(namedVariable, Space.EMPTY));

        return new J.VariableDeclarations(
                Tree.randomId(),
                prefix,
                markers,
                leadingAnnotations,
                modifiers,
                typeExpression,
                null,
                Collections.emptyList(),
                variables
        );
    }

    @Override
    public J visitConstantExpression(KtConstantExpression expression, ExecutionContext data) {
        Object value = Integer.valueOf(expression.getText());
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
        List<PsiElement> childNodes = getAllChildren(expression);
        Space space = null;
        boolean afterOp = false;
        Space binaryPrefix = null;
        JLeftPadded<J.Binary.Type> operator = null;
        Expression left = null;

        for (PsiElement child : childNodes) {
            String nodeText = child.getText();
            IElementType childType = child.getNode().getElementType();
            if (childType == KtTokens.WHITE_SPACE) {
                if (space == null) {
                    space = Space.build(nodeText, new ArrayList<>());
                } else {
                    space = space.withComments(ListUtils.mapLast(space.getComments(), c -> c.withSuffix(nodeText)));
                }
            } else if (childType == KtTokens.EOL_COMMENT || childType == KtTokens.BLOCK_COMMENT) {
                boolean isMultiLineComment = childType == KtTokens.BLOCK_COMMENT;
                String comment;
                // unwrap `//` or `/* */`
                comment = isMultiLineComment ? nodeText.substring(2, nodeText.length() - 2) : nodeText.substring(2);
                if (space == null) {
                    space = Space.build("", new ArrayList<>());
                }
                space = space.withComments(ListUtils.concat(space.getComments(), new TextComment(isMultiLineComment, comment, "", Markers.EMPTY)));
            } else if (childType == KtNodeTypes.OPERATION_REFERENCE) {
                afterOp = true;
                J.Binary.Type javaBinaryType = mapBinaryType(child); // J.Binary.Type.Addition;
                operator = padLeft(space, javaBinaryType);
                space = null;
            } else {
                Expression exp = convertToExpression(((KtElement) child).accept(this, data));
                if (!afterOp) {
                    binaryPrefix = space;
                    space = null;
                    left = exp;
                } else {
                    return new J.Binary(
                            randomId(),
                            spaceOrEmpty(binaryPrefix),
                            Markers.EMPTY,
                            left,
                            operator,
                            exp.withPrefix(spaceOrEmpty(space)),
                            type(expression)
                    );
                }
            }
        }

        throw new IllegalArgumentException("Parsing error with BINARY_EXPRESSION");
    }


    private J.Binary.Type mapBinaryType(PsiElement operationReference) {
        List<PsiElement> childNodes = getAllChildren(operationReference);
        if (childNodes.size() > 1) {
            throw new IllegalArgumentException("Parsing error with OPERATION_REFERENCE, unknown case");
        }
        PsiElement child = childNodes.get(0);
        IElementType elementType = child.getNode().getElementType();
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
    @NonNull
    private Space spaceOrEmpty(@Nullable Space space) {
        return space != null ? space : Space.EMPTY;
    }

    private List<PsiElement> getAllChildren(PsiElement parent) {
        List<PsiElement> children = new ArrayList<>();
        Iterator<PsiElement> iterator = PsiUtilsKt.getAllChildren(parent).iterator();
        while (iterator.hasNext()) {
            PsiElement it = iterator.next();
            children.add(it);
        }
        return children;
    }

    private J.Identifier createIdentifier(String name, Space prefix, @Nullable JavaType type) {
        return createIdentifier(name, prefix,
                type instanceof JavaType.Variable ? ((JavaType.Variable) type).getType() : type,
                type instanceof JavaType.Variable ? (JavaType.Variable) type : null);
    }

    private J.Identifier createIdentifier(String name, Space prefix, @Nullable JavaType type, @Nullable JavaType.Variable fieldType) {
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

    @Nullable
    private FirBasedSymbol getCurrentFile() {
        return currentFile != null ? currentFile.getSymbol() : null;
    }
}
