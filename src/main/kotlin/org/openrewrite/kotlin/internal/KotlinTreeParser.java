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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.Tree;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;


@SuppressWarnings("UnstableApiUsage")
public class KotlinTreeParser {

    private final KotlinSource kotlinSource;
    private final PsiElementAssociations psiElementAssociations;
    private final List<NamedStyles> styles;
    private final Path sourcePath;
    private final FileAttributes fileAttributes;

    private final Charset charset;
    private final Boolean charsetBomMarked;
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

    public K.CompilationUnit parse() {
        return (K.CompilationUnit) map(kotlinSource.getKtFile());
    }

    private J map(PsiElement psiElement) {
        String type = psiElement.getNode().getElementType().getDebugName();

        switch (type) {
            case "kotlin.FILE":
                return mapCompilationUnit(psiElement);
            case "PROPERTY":
                return mapVariableDeclarations(psiElement);
            case "INTEGER_CONSTANT":
                return mapIntegerConstant(psiElement);

            default:
                throw new UnsupportedOperationException("Unsupported PSI type " + type);
        }
    }


    /*====================================================================
    * PIS to J tree mapping methods
    * ====================================================================*/
    private K.CompilationUnit mapCompilationUnit(PsiElement kotlinFile) {
        List<J.Annotation> annotations = new ArrayList<>();
        @Nullable JRightPadded<J.Package> packageDeclaration = null;
        List<JRightPadded<J.Import>> imports = new ArrayList<>();
        List<JRightPadded<Statement>> statements = new ArrayList<>();

        List<PsiElement> childNodes = getAllChildren(kotlinFile);
        for (PsiElement child : childNodes) {

            switch (child.getNode().getElementType().getDebugName()) {
                case "PACKAGE_DIRECTIVE":
                case "IMPORT_LIST":
                    // todo
                    break;
                case "PROPERTY":
                    J.VariableDeclarations v = (J.VariableDeclarations) map(child);
                    statements.add(padRight(v, Space.EMPTY));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported child PSI type in kotlin.FILE :" + child.getNode().getElementType());
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

    private J.VariableDeclarations mapVariableDeclarations(PsiElement property) {

        // todo
        Space prefix = Space.EMPTY;
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = new ArrayList<>();
        TypeTree typeExpression = null;
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = new ArrayList<>();

        List<PsiElement> childNodes = getAllChildren(property);
        String space = "";
        boolean afterEQ = false;

        J.Identifier identifier = null;
        JLeftPadded<Expression> initializer = null;

        String identifierName = "";
        JavaType identifierType = null;
        String spaceBeforeIdentifier = "";
        String spaceBeforeEQ = "";
        String spaceAfterEQ = "";

        for (PsiElement child : childNodes) {
            String nodeText = child.getText();

            String childType = child.getNode().getElementType().getDebugName();
            switch (childType) {
                case "val":  {
                    modifiers.add(
                            new J.Modifier(
                                    Tree.randomId(),
                                    Space.EMPTY, // todo
                                    Markers.EMPTY,
                                    null,
                                    J.Modifier.Type.Final,
                                    Collections.emptyList()
                            )
                    );
                    continue;
                }
                case "WHITE_SPACE": {
                    space = nodeText;
                    continue;
                }
                case "IDENTIFIER": {
                    identifierName = nodeText;
                    identifierType = type(property);
                    spaceBeforeIdentifier = space;
                    identifier = createIdentifier(identifierName,
                            Space.EMPTY.withWhitespace(spaceBeforeIdentifier), identifierType);
                    space = "";
                    continue;
                }
                case "EQ": {
                    spaceBeforeEQ = space;
                    space = "";
                    afterEQ = true;
                    continue;
                }
                default: {
                    if (afterEQ) {
                        spaceAfterEQ = space;
                        space = "";
                        Expression exp = convertToExpression(map(child));
                        initializer = padLeft(Space.EMPTY.withWhitespace(spaceBeforeEQ),
                                exp.withPrefix(Space.EMPTY.withWhitespace(spaceAfterEQ))) ;
                        continue;
                    }
                    throw new UnsupportedOperationException("Unsupported child PSI type in PROPERTY :" + child.getNode().getElementType());
                }
            }
        }

        // todo, fill this type
        JavaType.Variable jtv = null;

        J.VariableDeclarations.NamedVariable namedVariable =
                new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        identifier,
                        emptyList(),
                        initializer,
                        jtv
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

    private J.Literal mapIntegerConstant(PsiElement integerConstant) {
        Object value = Integer.valueOf(integerConstant.getText());
        return new J.Literal(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                value,
                integerConstant.getText(),
                null,
                JavaType.Primitive.Int
        );
    }


    /*====================================================================
     * Type methods
     * ====================================================================*/
    @Nullable
    private JavaType type(PsiElement psi) {
        return psiElementAssociations.type(psi, currentFile.getSymbol());
    }



    /*====================================================================
     * Other helper methods
     * ====================================================================*/
    private List<PsiElement> getAllChildren(PsiElement parent) {
        List<PsiElement> children = new ArrayList<>();
        Iterator<PsiElement> iterator = PsiUtilsKt.getAllChildren(parent).iterator();
        while (iterator.hasNext()) {
            PsiElement it = iterator.next();
            children.add(it);
        }
        return children;
    }

    private J.Identifier createIdentifier(String name, Space prefix, JavaType type) {
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
}
