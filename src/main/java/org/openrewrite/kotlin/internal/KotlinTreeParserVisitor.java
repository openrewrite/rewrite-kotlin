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
import org.jetbrains.kotlin.com.intellij.psi.*;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiErrorElementImpl;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.fir.ClassMembersKt;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport;
import org.jetbrains.kotlin.fir.declarations.FirVariable;
import org.jetbrains.kotlin.fir.expressions.FirConstExpression;
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall;
import org.jetbrains.kotlin.fir.resolve.LookupTagUtilsKt;
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol;
import org.jetbrains.kotlin.fir.types.ConeClassLikeType;
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtToken;
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
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * PSI based parser
 */
public class KotlinTreeParserVisitor extends KtVisitor<J, ExecutionContext> {
    private final KotlinSource kotlinSource;
    private final FirSession firSession;
    private final KotlinTypeMapping typeMapping;
    private final PsiElementAssociations psiElementAssociations;
    private final List<NamedStyles> styles;
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;

    private final Charset charset;
    private final Boolean charsetBomMarked;
    private final FirFile currentFile;
    private final ExecutionContext executionContext;

    public KotlinTreeParserVisitor(KotlinSource kotlinSource,
                                   FirSession firSession,
                                   KotlinTypeMapping typeMapping,
                                   PsiElementAssociations psiElementAssociations,
                                   List<NamedStyles> styles,
                                   @Nullable Path relativeTo,
                                   ExecutionContext ctx) {
        this.kotlinSource = kotlinSource;
        this.firSession = firSession;
        this.typeMapping = typeMapping;
        this.psiElementAssociations = psiElementAssociations;
        this.styles = styles;
        sourcePath = kotlinSource.getInput().getRelativePath(relativeTo);
        fileAttributes = kotlinSource.getInput().getFileAttributes();
        EncodingDetectingInputStream stream = kotlinSource.getInput().getSource(ctx);
        charset = stream.getCharset();
        charsetBomMarked = stream.isCharsetBomMarked();
        currentFile = Objects.requireNonNull(kotlinSource.getFirFile());
        executionContext = ctx;
    }

    public K.CompilationUnit parse() {
        return (K.CompilationUnit) visitKtFile(kotlinSource.getKtFile(), executionContext);
    }

    @Override
    public J visitParenthesizedExpression(KtParenthesizedExpression expression, ExecutionContext data) {
        assert expression.getExpression() != null;

        PsiElement rPar = expression.getLastChild();
        if (rPar == null || !(")".equals(rPar.getText()))) {
            throw new UnsupportedOperationException("TODO");
        }

        return new J.Parentheses<>(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                padRight(expression.getExpression().accept(this, data), prefix(rPar))
        );
    }

    @Override
    public J visitForExpression(KtForExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitAnnotatedExpression(KtAnnotatedExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitAnnotationUseSiteTarget(KtAnnotationUseSiteTarget annotationTarget, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitAnonymousInitializer(KtAnonymousInitializer initializer, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitArrayAccessExpression(KtArrayAccessExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitBackingField(KtBackingField accessor, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitBinaryWithTypeRHSExpression(KtBinaryExpressionWithTypeRHS expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitBlockStringTemplateEntry(KtBlockStringTemplateEntry entry, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitBreakExpression(KtBreakExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitCallableReferenceExpression(KtCallableReferenceExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitCatchSection(KtCatchClause catchClause, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitClassInitializer(KtClassInitializer initializer, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitClassLiteralExpression(KtClassLiteralExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitClassOrObject(KtClassOrObject classOrObject, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitCollectionLiteralExpression(KtCollectionLiteralExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitConstructorCalleeExpression(KtConstructorCalleeExpression constructorCalleeExpression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitConstructorDelegationCall(KtConstructorDelegationCall call, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitContextReceiverList(KtContextReceiverList contextReceiverList, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitContinueExpression(KtContinueExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitDeclaration(KtDeclaration dcl, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitDelegatedSuperTypeEntry(KtDelegatedSuperTypeEntry specifier, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitDestructuringDeclarationEntry(KtDestructuringDeclarationEntry multiDeclarationEntry, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitDoubleColonExpression(KtDoubleColonExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitDoWhileExpression(KtDoWhileExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitDynamicType(KtDynamicType type, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitEnumEntry(KtEnumEntry enumEntry, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitEscapeStringTemplateEntry(KtEscapeStringTemplateEntry entry, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitExpression(KtExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitExpressionWithLabel(KtExpressionWithLabel expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitFileAnnotationList(KtFileAnnotationList fileAnnotationList, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitFinallySection(KtFinallySection finallySection, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitFunctionType(KtFunctionType type, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitImportAlias(KtImportAlias importAlias, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitImportList(KtImportList importList, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitInitializerList(KtInitializerList list, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitIntersectionType(KtIntersectionType definitelyNotNullType, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitIsExpression(KtIsExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitLabeledExpression(KtLabeledExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitLambdaExpression(KtLambdaExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitLiteralStringTemplateEntry(KtLiteralStringTemplateEntry entry, ExecutionContext data) {
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
    public J visitLoopExpression(KtLoopExpression loopExpression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitModifierList(KtModifierList list, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitNamedDeclaration(KtNamedDeclaration declaration, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitNullableType(KtNullableType nullableType, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitParameter(KtParameter parameter, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitParameterList(KtParameterList list, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitPrimaryConstructor(KtPrimaryConstructor constructor, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitPropertyAccessor(KtPropertyAccessor accessor, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitQualifiedExpression(KtQualifiedExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitReferenceExpression(KtReferenceExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitReturnExpression(KtReturnExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSafeQualifiedExpression(KtSafeQualifiedExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitScript(KtScript script, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitScriptInitializer(KtScriptInitializer initializer, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSecondaryConstructor(KtSecondaryConstructor constructor, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSelfType(KtSelfType type, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSimpleNameStringTemplateEntry(KtSimpleNameStringTemplateEntry entry, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitStringTemplateEntryWithExpression(KtStringTemplateEntryWithExpression entry, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSuperExpression(KtSuperExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSuperTypeEntry(KtSuperTypeEntry specifier, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitSuperTypeListEntry(KtSuperTypeListEntry specifier, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitThisExpression(KtThisExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitThrowExpression(KtThrowExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitTryExpression(KtTryExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitTypeAlias(KtTypeAlias typeAlias, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitTypeArgumentList(KtTypeArgumentList typeArgumentList, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitTypeConstraint(KtTypeConstraint constraint, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitTypeConstraintList(KtTypeConstraintList list, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitTypeParameter(KtTypeParameter parameter, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitTypeParameterList(KtTypeParameterList list, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitTypeProjection(KtTypeProjection typeProjection, ExecutionContext data) {
        return typeProjection.getTypeReference().accept(this, data);
    }

    @Override
    public J visitUnaryExpression(KtUnaryExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitWhenConditionInRange(KtWhenConditionInRange condition, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitWhenConditionIsPattern(KtWhenConditionIsPattern condition, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitWhenConditionWithExpression(KtWhenConditionWithExpression condition, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitWhenEntry(KtWhenEntry ktWhenEntry, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitWhenExpression(KtWhenExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitWhileExpression(KtWhileExpression expression, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void visitBinaryFile(PsiBinaryFile file) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void visitComment(PsiComment comment) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void visitDirectory(PsiDirectory dir) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitKtElement(KtElement element, ExecutionContext data) {
        return element.accept(this, data);
    }

    /*====================================================================
     * PSI to J tree mapping methods
     * ====================================================================*/
    @Override
    public J visitKtFile(KtFile file, ExecutionContext data) {
        Space prefix = Space.EMPTY;
        List<J.Annotation> annotations = file.getAnnotationEntries().isEmpty() ? emptyList() : new ArrayList<>(file.getAnnotationEntries().size());
        if (!file.getAnnotationEntries().isEmpty()) {
            prefix = prefix(file.getFileAnnotationList());
            annotations = mapFileAnnotations(file.getFileAnnotationList(), data);
        }

        JRightPadded<J.Package> pkg = null;
        if (file.getPackageDirective() != null) {
            pkg = maybeSemicolon((J.Package) file.getPackageDirective().accept(this, data), file.getPackageDirective());
        }

        List<JRightPadded<J.Import>> imports = file.getImportDirectives().isEmpty() ? emptyList() : new ArrayList<>(file.getImportDirectives().size());
        if (!file.getImportDirectives().isEmpty()) {
            List<KtImportDirective> importDirectives = file.getImportDirectives();
            for (int i = 0; i < importDirectives.size(); i++) {
                KtImportDirective importDirective = importDirectives.get(i);
                J.Import anImport = (J.Import) importDirective.accept(this, data);
                if (i == 0) {
                    anImport = anImport.withPrefix(prefix(file.getImportList()));
                }
                imports.add(maybeSemicolon(anImport, importDirective));
            }
        }

        List<JRightPadded<Statement>> statements = file.getDeclarations().isEmpty() ? emptyList() : new ArrayList<>(file.getDeclarations().size());
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
                pkg,
                imports,
                statements,
                Space.EMPTY
        );
    }

    @Override
    public J visitAnnotation(KtAnnotation annotation, ExecutionContext data) {
        throw new UnsupportedOperationException("KtAnnotation");
    }

    @Override
    public J visitAnnotationEntry(@NotNull KtAnnotationEntry annotationEntry, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        if (annotationEntry.getUseSiteTarget() != null) {
            markers = markers.addIfAbsent(new AnnotationCallSite(
                    randomId(),
                    annotationEntry.getUseSiteTarget().getText(),
                    suffix(annotationEntry.getUseSiteTarget())
            ));
        }

        // List includes Parens.
        if (annotationEntry.getValueArgumentList() != null) {
            throw new UnsupportedOperationException("TODO");
        }
        if (annotationEntry.getTypeReference() != null) {
            throw new UnsupportedOperationException("TODO");
        }

        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public J visitArgument(KtValueArgument argument, ExecutionContext data) {
        if (argument.getArgumentExpression() == null) {
            throw new UnsupportedOperationException("TODO");
        }

        return argument.getArgumentExpression().accept(this, data);
    }

    @Override
    public J visitBinaryExpression(KtBinaryExpression expression, ExecutionContext data) {
        assert expression.getLeft() != null;
        assert expression.getRight() != null;

        return new K.Binary(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                convertToExpression(expression.getLeft().accept(this, data)).withPrefix(Space.EMPTY),
                padLeft(prefix(expression.getOperationReference()), mapBinaryType(expression.getOperationReference())),
                convertToExpression((expression.getRight()).accept(this, data))
                        .withPrefix(prefix(expression.getRight())),
                Space.EMPTY,
                methodInvocationType(expression)
        );
    }

    @Override
    public J visitBlockExpression(KtBlockExpression expression, ExecutionContext data) {
        List<JRightPadded<Statement>> statements = new ArrayList<>();
        for (KtExpression s : expression.getStatements()) {
            J accepted = s.accept(this, data);
            Statement statement = convertToStatement(accepted);
            JRightPadded<Statement> build = maybeSemicolon(statement, s);
            statements.add(build);
        }

        boolean hasBraces = expression.getLBrace() != null;
        return new J.Block(
                randomId(),
                prefix(expression.getLBrace()),
                hasBraces ? Markers.EMPTY : Markers.EMPTY.addIfAbsent(new OmitBraces(randomId())),
                JRightPadded.build(false),
                statements,
                prefix(expression.getRBrace())
        );
    }

    @Override
    public J visitCallExpression(KtCallExpression expression, ExecutionContext data) {
        if (expression.getCalleeExpression() == null) {
            throw new UnsupportedOperationException("TODO");
        }

        PsiElementAssociations.ExpressionType type = psiElementAssociations.getExpressionType(expression);
        if (type == PsiElementAssociations.ExpressionType.CONSTRUCTOR) {
            TypeTree name = (J.Identifier) expression.getCalleeExpression().accept(this, data);
            if (!expression.getTypeArguments().isEmpty()) {
                // FIXME: create a list.
                Expression expr = (Expression) expression.getTypeArguments().get(0).accept(this, data);
                name = new J.ParameterizedType(
                        randomId(),
                        name.getPrefix(),
                        Markers.EMPTY,
                        name.withPrefix(Space.EMPTY),
                        JContainer.build(prefix(expression.getTypeArgumentList()), singletonList(padRight(expr, Space.EMPTY)), Markers.EMPTY),
                        type(expression)
                );
            }

            // createIdentifier(expression.getCalleeExpression(), type(expression));
            List<KtValueArgument> arguments = expression.getValueArguments();

            JContainer<Expression> args;
            if (arguments.isEmpty()) {
                args = JContainer.build(singletonList(padRight(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY), Space.EMPTY)));
            } else {
                List<JRightPadded<Expression>> expressions = new ArrayList<>(arguments.size());
                Markers markers = Markers.EMPTY;

                for (KtValueArgument arg : arguments) {
                    expressions.add(padRight(convertToExpression(arg.accept(this, data)).withPrefix(prefix(arg)), suffix(arg)));
                }
                args = JContainer.build(prefix(expression.getValueArgumentList()), expressions, markers);
            }
            return new J.NewClass(
                    randomId(),
                    Space.SINGLE_SPACE,
                    Markers.EMPTY,
                    null,
                    Space.EMPTY,
                    name,
                    args,
                    null,
                    methodInvocationType(expression)
            );
        } else if (type == PsiElementAssociations.ExpressionType.METHOD_INVOCATION) {
            J.Identifier name = (J.Identifier) expression.getCalleeExpression().accept(this, data);
            if (!expression.getTypeArguments().isEmpty()) {
                Expression expr = (Expression) expression.accept(this, data);
                throw new UnsupportedOperationException("TODO");
//                name = new J.ParameterizedType(
//                        randomId(),
//                        name.getPrefix(),
//                        Markers.EMPTY,
//                        name.withPrefix(Space.EMPTY),
//                        JContainer.build(prefix(expression.getTypeArgumentList()), singletonList(padRight(expr, Space.EMPTY)), Markers.EMPTY),
//                        type(expression)
//                );
            }

            // createIdentifier(expression.getCalleeExpression(), type(expression));
            List<KtValueArgument> arguments = expression.getValueArguments();

            List<JRightPadded<Expression>> expressions = new ArrayList<>(arguments.size());
            Markers markers = Markers.EMPTY;

            for (KtValueArgument arg : arguments) {
                expressions.add(padRight(convertToExpression(arg.accept(this, data)).withPrefix(prefix(arg)), suffix(arg)));
            }

            if (expressions.isEmpty()) {
                expressions.add(padRight(new J.Empty(randomId(), prefix(expression.getValueArgumentList().getRightParenthesis()), Markers.EMPTY),  Space.EMPTY));
            }

            JContainer<Expression> args = JContainer.build(prefix(expression.getValueArgumentList()), expressions, markers);

            JavaType.Method methodType = methodInvocationType(expression);
            return new J.MethodInvocation(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    null,
                    null,
                    (J.Identifier) name.withType(methodType).withPrefix(prefix(expression)),
                    args,
                    methodType
            );
        } else {
            throw new UnsupportedOperationException("TODO");
        }
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
    public J visitClass(KtClass klass, ExecutionContext data) {
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

        // TODO: fix NPE.
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
    public J visitClassBody(KtClassBody classBody, ExecutionContext data) {
        List<JRightPadded<Statement>> list = new ArrayList<>();
        for (KtDeclaration d : classBody.getDeclarations()) {
            J j = d.accept(this, data).withPrefix(prefix(d));
            Statement statement = convertToStatement(j);
            JRightPadded<Statement> build = maybeSemicolon(statement, d);
            list.add(build);
        }
        return new J.Block(
                randomId(),
                prefix(classBody),
                Markers.EMPTY,
                padRight(false, Space.EMPTY),
                list,
                prefix(classBody.getRBrace())
        );
    }

    @Override
    public J visitDestructuringDeclaration(KtDestructuringDeclaration multiDeclaration, ExecutionContext data) {
        List<J.Modifier> modifiers = new ArrayList<>();
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> vars = new ArrayList<>();
        JLeftPadded<Expression> paddedInitializer;

        J.Modifier modifier = new J.Modifier(
                Tree.randomId(),
                prefix(multiDeclaration.getValOrVarKeyword()),
                Markers.EMPTY,
                multiDeclaration.isVar() ? "var" : null,
                multiDeclaration.isVar() ? J.Modifier.Type.LanguageExtension : J.Modifier.Type.Final,
                Collections.emptyList()
        );
        modifiers.add(modifier);

        if (multiDeclaration.getInitializer() == null) {
            throw new UnsupportedOperationException("TODO");
        }

        paddedInitializer = padLeft(suffix(multiDeclaration.getRPar()),
                convertToExpression(multiDeclaration.getInitializer().accept(this, data))
                        .withPrefix(prefix(multiDeclaration.getInitializer())));

        List<KtDestructuringDeclarationEntry> entries = multiDeclaration.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            KtDestructuringDeclarationEntry entry = entries.get(i);

            if (entry.getModifierList() != null) {
                throw new UnsupportedOperationException("TODO");
            }

            JavaType.Variable vt = variableType(entry);

            if (entry.getName() == null) {
                throw new UnsupportedOperationException();
            }

            J.Identifier nameVar = createIdentifier(entry.getName(), prefix(entry), vt != null ? vt.getType() : null, vt);
            J.VariableDeclarations.NamedVariable namedVariable = new J.VariableDeclarations.NamedVariable(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    nameVar,
                    emptyList(),
                    null,
                    vt
            );

            JavaType.Method methodType = null;
            if (paddedInitializer.getElement() instanceof J.NewClass) {
                // TODO: fix what is NC for?
                J.NewClass nc = (J.NewClass) paddedInitializer.getElement();
                methodType = methodInvocationType(entry);
            }
            J.MethodInvocation initializer = buildSyntheticDestructInitializer(i + 1)
                    .withMethodType(methodType);
            namedVariable = namedVariable.getPadding().withInitializer(padLeft(Space.SINGLE_SPACE, initializer));
            vars.add(padRight(namedVariable, suffix(entry)));
        }

        J.VariableDeclarations.NamedVariable emptyWithInitializer = new J.VariableDeclarations.NamedVariable(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                new J.Identifier(
                        randomId(),
                        Space.SINGLE_SPACE,
                        Markers.EMPTY,
                        emptyList(),
                        "<destruct>",
                        null,
                        null
                ),
                emptyList(),
                paddedInitializer,
                null
        );

        J.VariableDeclarations variableDeclarations = new J.VariableDeclarations(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                leadingAnnotations,
                modifiers,
                null,
                null,
                emptyList(),
                singletonList(padRight(emptyWithInitializer, Space.EMPTY)
                )
        );

        return new K.DestructuringDeclaration(
                randomId(),
                prefix(multiDeclaration),
                Markers.EMPTY,
                variableDeclarations,
                JContainer.build(prefix(multiDeclaration.getLPar()), vars, Markers.EMPTY)
        );
    }

    private J.MethodInvocation buildSyntheticDestructInitializer(int id) {
        J.Identifier name = new J.Identifier(
                randomId(),
                Space.SINGLE_SPACE,
                Markers.EMPTY,
                emptyList(),
                "component" + id,
                null,
                null
        );

        return new J.MethodInvocation(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                null,
                null,
                name,
                JContainer.empty(),
                null
        );
    }

    @Override
    public J visitDotQualifiedExpression(KtDotQualifiedExpression expression, ExecutionContext data) {
        assert expression.getSelectorExpression() != null;
        if (expression.getSelectorExpression() instanceof KtCallExpression) {

            KtCallExpression callExpression = (KtCallExpression) expression.getSelectorExpression();
            if (!callExpression.getTypeArguments().isEmpty()) {
                throw new UnsupportedOperationException("TODO");
            }
            J j = expression.getSelectorExpression().accept(this, data);
            if (j instanceof J.MethodInvocation) {
                J.MethodInvocation methodInvocation = (J.MethodInvocation) j;
                methodInvocation = methodInvocation.getPadding().withSelect(
                        padRight(expression.getReceiverExpression().accept(this, data).withPrefix(Space.EMPTY), suffix(expression.getReceiverExpression()))
                );
                return methodInvocation;
            }
            return new J.MethodInvocation(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    padRight(expression.getReceiverExpression().accept(this, data).withPrefix(Space.EMPTY), Space.EMPTY),
                    null,
                    expression.getSelectorExpression().accept(this, data).withPrefix(Space.EMPTY),
                    JContainer.empty(),
                    methodInvocationType(expression) // TODO: fix for constructors of inner classes
            );
        } else {
            // Maybe need to type check before creating a field access.
            return new J.FieldAccess(
                    randomId(),
                    prefix(expression),
                    Markers.EMPTY,
                    expression.getReceiverExpression().accept(this, data).withPrefix(Space.EMPTY),
                    padLeft(prefix(expression.getSelectorExpression()), (J.Identifier) expression.getSelectorExpression().accept(this, data)),
                    type(expression)
            );
        }
    }

    @Override
    public J visitIfExpression(KtIfExpression expression, ExecutionContext data) {
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
    public J visitImportDirective(KtImportDirective importDirective, ExecutionContext data) {
        FirElement firElement = psiElementAssociations.primary(importDirective);
        boolean hasParentClassId = firElement instanceof FirResolvedImport && ((FirResolvedImport) firElement).getResolvedParentClassId() != null;
        JLeftPadded<Boolean> statik = padLeft(Space.EMPTY, hasParentClassId);
        KtImportAlias alias = importDirective.getAlias();
        String text = nodeRangeText(
                importDirective.getNode().findChildByType(KtTokens.WHITE_SPACE),
                importDirective.isAllUnder() ? importDirective.getNode().findChildByType(KtTokens.MUL)
                        : importDirective.getNode().findChildByType(KtNodeTypes.DOT_QUALIFIED_EXPRESSION));
        J reference = TypeTree.build(text);
//        J reference = importDirective.getImportedReference().accept(this, data);
//        if (importDirective.isAllUnder()) {
//            reference = new J.FieldAccess(
//                    randomId(),
//                    Space.EMPTY,
//                    Markers.EMPTY,
//                    (Expression) reference,
//                    padLeft(Space.EMPTY, createIdentifier(
//                            "*",
//                            prefix((PsiElement) importDirective.getNode().findChildByType(KtTokens.MUL)),
//                            null
//                    )),
//                    null
//            );
//        }
        if (reference instanceof J.Identifier) {
            reference = new J.FieldAccess(
                    randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY),
                    padLeft(Space.EMPTY, (J.Identifier) reference),
                    null
            );
        }

        return new J.Import(
                randomId(),
                prefix(importDirective),
                Markers.EMPTY,
                statik,
                (J.FieldAccess) reference,
                // TODO: fix NPE.
                alias != null ? padLeft(prefix(alias), createIdentifier(alias.getNameIdentifier(), null)) : null
        );
    }

    @Override
    public J visitNamedFunction(KtNamedFunction function, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = mapModifiers(function.getModifierList());
        J.TypeParameters typeParameters = null;
        TypeTree returnTypeExpression = null;

        if (function.getTypeReference() != null) {
            throw new UnsupportedOperationException("TODO");
        }
        if (function.getReceiverTypeReference() != null) {
            throw new UnsupportedOperationException("TODO");
        }

        List<KtTypeParameter> typeParams = function.getTypeParameters();

        boolean isOpen = function.hasModifier(KtTokens.OPEN_KEYWORD);
        if (!isOpen) {
            modifiers.add(
                    new J.Modifier(
                            randomId(),
                            prefix(function.getFunKeyword()),
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
                        Space.EMPTY,
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
                                    // TODO: fix NPE.
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
                prefix(function),
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
    public J visitObjectLiteralExpression(KtObjectLiteralExpression expression, ExecutionContext data) {
        return expression.getObjectDeclaration().accept(this, data).withPrefix(prefix(expression));
    }

    @Override
    public J visitObjectDeclaration(KtObjectDeclaration declaration, ExecutionContext data) {
        TypeTree clazz;
        Markers markers = Markers.EMPTY;
        JContainer<Expression> args;

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

        // TODO: fix NPE.
        J.Block body = (J.Block) declaration.getBody().accept(this, data);

        if (declaration.getObjectKeyword() != null) {
            markers = markers.add(new KObject(randomId(), Space.EMPTY));
            markers = markers.add(new TypeReferencePrefix(randomId(), prefix(declaration.getColon())));
        }

        clazz = declaration.getSuperTypeList().accept(this, data).withPrefix(Space.EMPTY);

        return new J.NewClass(
                randomId(),
                prefix(declaration),
                markers,
                null,
                suffix(declaration.getColon()),
                clazz,
                args,
                body,
                null
        );
    }

    @Override
    @Nullable
    public J visitPackageDirective(KtPackageDirective directive, ExecutionContext data) {
        if (directive.getPackageNameExpression() == null) {
            return null;
        }
        return new J.Package(
                randomId(),
                prefix(directive),
                Markers.EMPTY,
                (Expression) directive.getPackageNameExpression().accept(this, data),
                emptyList()
        );
    }

    @Override
    public J visitPrefixExpression(KtPrefixExpression expression, ExecutionContext data) {
        assert expression.getBaseExpression() != null;

        KtSimpleNameExpression ktSimpleNameExpression = expression.getOperationReference();
        if (ktSimpleNameExpression == null || !("!".equals(ktSimpleNameExpression.getReferencedName()))) {
            throw new UnsupportedOperationException("TODO");
        }

        return new J.Unary(
                randomId(),
                prefix(expression),
                Markers.EMPTY,
                padLeft(prefix(expression.getOperationReference()), J.Unary.Type.Not),
                (Expression) expression.getBaseExpression().accept(this, data).withPrefix(suffix(ktSimpleNameExpression)),
                methodInvocationType(expression)
        );
    }

    @Override
    public J visitPostfixExpression(KtPostfixExpression expression, ExecutionContext data) {
        // TODO: fix NPE.
        J j = expression.getBaseExpression().accept(this, data);
        if (expression.getOperationReference().getReferencedNameElementType() == KtTokens.EXCLEXCL) {
            j = j.withMarkers(j.getMarkers().addIfAbsent(new CheckNotNull(randomId(), prefix(expression.getOperationReference()))));
        } else {
            throw new UnsupportedOperationException("TODO");
        }
        return j;
    }

    @Override
    public J visitProperty(KtProperty property, ExecutionContext data) {
        Markers markers = Markers.EMPTY;
        List<J.Annotation> leadingAnnotations = new ArrayList<>();
        List<J.Modifier> modifiers = mapModifiers(property.getModifierList());
        TypeTree typeExpression = null;
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = new ArrayList<>();

        modifiers.add(new J.Modifier(
                Tree.randomId(),
                prefix(property.getValOrVarKeyword()),
                Markers.EMPTY,
                property.isVar() ? "var" : null,
                property.isVar() ? J.Modifier.Type.LanguageExtension : J.Modifier.Type.Final,
                Collections.emptyList() // FIXME
        ));

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
                        // TODO: fix NPE.
                        createIdentifier(property.getNameIdentifier(), type(property)),
                        emptyList(),
                        initializer,
                        variableType(property)
                );

        variables.add(padRight(namedVariable, Space.EMPTY));

        if (property.getColon() != null) {
            markers = markers.addIfAbsent(new TypeReferencePrefix(randomId(), prefix(property.getColon())));
            // TODO: fix NPE.
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
                modifiers,
                typeExpression,
                null,
                Collections.emptyList(),
                variables
        );
    }

    private List<J.Modifier> mapModifiers(@Nullable KtModifierList modifierList) {
        ArrayList<J.Modifier> modifiers = new ArrayList<>();
        if (modifierList == null) {
            return modifiers;
        }

        List<J.Annotation> annotations = new ArrayList<>();
        for (PsiElement child = PsiTreeUtil.firstChild(modifierList); child != null; child = child.getNextSibling()) {
            if (child instanceof LeafPsiElement && child.getNode().getElementType() instanceof KtModifierKeywordToken) {
                // TODO: fix leading annotations and modifier annotations.
                modifiers.add(mapModifier(child, annotations));
            } else if (child instanceof KtAnnotationEntry) {
                System.out.println();
            }
        }
        return modifiers;
    }

    @Override
    public J visitPropertyDelegate(KtPropertyDelegate delegate, ExecutionContext data) {
        throw new UnsupportedOperationException("Unsupported KtPropertyDelegate");
    }

    @Override
    public J visitSimpleNameExpression(KtSimpleNameExpression expression, ExecutionContext data) {
        FirBasedSymbol<?> symbol = psiElementAssociations.symbol(expression);
        if (symbol instanceof FirVariableSymbol<?>) {
            JavaType.FullyQualified owner = null;
            ConeClassLikeLookupTag lookupTag = ClassMembersKt.containingClassLookupTag((FirCallableSymbol<?>) symbol);
            if (lookupTag != null && !(LookupTagUtilsKt.toSymbol(lookupTag, firSession) instanceof FirAnonymousObjectSymbol)) {
                // TODO check type attribution for `FirAnonymousObjectSymbol` case
                owner =
                        // TODO: fix NPE.
                        (JavaType.FullyQualified) typeMapping.type(LookupTagUtilsKt.toFirRegularClassSymbol(lookupTag, firSession).getFir());
            }
            return createIdentifier(
                    expression,
                    // TODO: fix NPE.
                    typeMapping.variableType((FirVariableSymbol<?>) symbol, owner, psiElementAssociations.getFile().getSymbol())
            );
        }
        return createIdentifier(expression, type(expression));
    }

    @Override
    public J visitStringTemplateExpression(KtStringTemplateExpression expression, ExecutionContext data) {
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
    public J visitStringTemplateEntry(KtStringTemplateEntry entry, ExecutionContext data) {
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
    public J visitSuperTypeList(KtSuperTypeList list, ExecutionContext data) {
        List<KtSuperTypeListEntry> typeListEntries = list.getEntries();

        if (typeListEntries.size() > 1) {
            throw new UnsupportedOperationException("KtSuperTypeList size is bigger than 1, TODO");
        }
        return typeListEntries.get(0).accept(this, data);
    }

    @Override
    public J visitSuperTypeCallEntry(KtSuperTypeCallEntry call, ExecutionContext data) {
        // TODO: fix NPE.
        return call.getTypeReference().accept(this, data);
    }

    @Override
    public J visitTypeReference(KtTypeReference typeReference, ExecutionContext data) {
        // TODO: fix NPE.
        return typeReference.getTypeElement().accept(this, data);
    }

    @Override
    public J visitUserType(KtUserType type, ExecutionContext data) {
        if (!type.getTypeArguments().isEmpty()) {
            throw new UnsupportedOperationException("TODO");
        }
        // TODO: fix NPE.
        return type.getReferenceExpression().accept(this, data);
    }

    @Override
    public J visitValueArgumentList(KtValueArgumentList list, ExecutionContext data) {
        throw new UnsupportedOperationException("TODO");
    }

    /*====================================================================
     * Mapping methods
     * ====================================================================*/
    private K.Binary.Type mapBinaryType(KtOperationReferenceExpression operationReference) {
        IElementType elementType = operationReference.getOperationSignTokenType();
        if (elementType == KtTokens.PLUS)
            return K.Binary.Type.Plus;
        else if (elementType == KtTokens.MINUS)
            return K.Binary.Type.Minus;
        else if (elementType == KtTokens.MUL)
            return K.Binary.Type.Mul;
        else if (elementType == KtTokens.DIV)
            return K.Binary.Type.Div;
        else if (elementType == KtTokens.NOT_IN)
            return K.Binary.Type.NotContains;
        else if (elementType == KtTokens.RANGE)
            return K.Binary.Type.RangeTo;
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
                // TODO: fix NPE.
                padRight(convertToExpression(expression.getCondition().accept(this, executionContext))
                                .withPrefix(suffix(expression.getLeftParenthesis())),
                        prefix(expression.getRightParenthesis()))
        );
    }

    private JRightPadded<Statement> buildIfThenPart(KtIfExpression expression) {
        // TODO: fix NPE.
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
        // TODO: fix NPE.
        return typeMapping.primitive((ConeClassLikeType) ((FirResolvedTypeRef) ((FirConstExpression<?>) psiElementAssociations.primary(expression)).getTypeRef()).getType());
    }

    @Nullable
    private JavaType.Variable variableType(PsiElement psi) {
        if (psi instanceof KtDeclaration) {
            FirBasedSymbol<?> basedSymbol = psiElementAssociations.symbol((KtDeclaration) psi);
            if (basedSymbol instanceof FirVariableSymbol) {
                FirVariableSymbol<? extends FirVariable> variableSymbol = (FirVariableSymbol<? extends FirVariable>) basedSymbol;
                return psiElementAssociations.getTypeMapping().variableType(variableSymbol, null, psiElementAssociations.getFile().getSymbol());
            }
        }
        return null;
    }

    @Nullable
    private JavaType.Method methodDeclarationType(PsiElement psi) {
        if (psi instanceof KtNamedFunction) {
            FirBasedSymbol<?> basedSymbol = psiElementAssociations.symbol((KtNamedFunction) psi);
            if (basedSymbol instanceof FirNamedFunctionSymbol) {
                FirNamedFunctionSymbol functionSymbol = (FirNamedFunctionSymbol) basedSymbol;
                return psiElementAssociations.getTypeMapping().methodDeclarationType(functionSymbol.getFir(), null, psiElementAssociations.getFile().getSymbol());
            }
        }
        return null;
    }

    @Nullable
    private JavaType.Method methodInvocationType(PsiElement psi) {
        FirElement firElement = psiElementAssociations.component(psi);
        if (firElement instanceof FirFunctionCall) {
            return typeMapping.methodInvocationType((FirFunctionCall) firElement, psiElementAssociations.getFile().getSymbol());
        }
        return null;
    }

    /*====================================================================
     * Other helper methods
     * ====================================================================*/
    private J.Identifier createIdentifier(PsiElement name, @Nullable JavaType type) {
        return createIdentifier(name.getNode().getText(), prefix(name), type);
    }

    private J.Identifier createIdentifier(String name, Space prefix,
                                          @Nullable JavaType type) {
        return createIdentifier(name, prefix,
                type instanceof JavaType.Variable ? ((JavaType.Variable) type).getType() : type,
                type instanceof JavaType.Variable ? (JavaType.Variable) type : null);
    }

    private J.Identifier createIdentifier(String name, Space prefix,
                                          @Nullable JavaType type,
                                          @Nullable JavaType.Variable fieldType) {
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

    private <J2 extends J> JRightPadded<J2> maybeSemicolon(J2 j, KtElement element) {
        PsiElement maybeSemicolon = element.getLastChild();
        boolean hasSemicolon = maybeSemicolon instanceof LeafPsiElement && ((LeafPsiElement) maybeSemicolon).getElementType() == KtTokens.SEMICOLON;
        return hasSemicolon ? new JRightPadded<>(j, prefix(maybeSemicolon), Markers.EMPTY.add(new Semicolon(randomId())))
                : JRightPadded.build(j);
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private <T> JRightPadded<T> padRight(T tree, @Nullable Space right) {
        return padRight(tree, right, Markers.EMPTY);
    }

    private <T> JRightPadded<T> padRight(T tree, @Nullable Space right, Markers markers) {
        return new JRightPadded<>(tree, right == null ? Space.EMPTY : right, markers);
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> J2 convertToExpression(J j) {
        if (j instanceof Statement && !(j instanceof Expression)) {
            j = new K.StatementExpression(randomId(), (Statement) j);
        }
        return (J2) j;
    }

    private Statement convertToStatement(J j) {
        if (!(j instanceof Statement) && j instanceof Expression) {
            j = new K.ExpressionStatement(randomId(), (Expression) j);
        }
        if (!(j instanceof Statement)) {
            throw new UnsupportedOperationException("TODO");
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

    private Space openPrefix(@Nullable PsiElement element) {
        if (element == null) {
            return Space.EMPTY;
        }

        PsiElement whitespace = prev(element);
        if (whitespace == null || !isSpace(whitespace.getNode())) {
            return Space.EMPTY;
        }
        PsiElement prev;
        while ((prev = prev(whitespace)) != null && isSpace(prev.getNode())) {
            whitespace = prev;
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

    private String nodeRangeText(@Nullable ASTNode first, @Nullable ASTNode last) {
        StringBuilder builder = new StringBuilder();
        while (first != null) {
            builder.append(first.getText());
            if (first == last) {
                break;
            }
            first = first.getTreeNext();
        }
        return builder.toString();
    }

    private List<J.Annotation> mapFileAnnotations(@Nullable KtFileAnnotationList ktFileAnnotationList, ExecutionContext data) {
        if (ktFileAnnotationList == null && ktFileAnnotationList.getAnnotationEntries().isEmpty()) {
            return emptyList();
        }

        List<J.Annotation> annotations = new ArrayList<>(ktFileAnnotationList.getAnnotationEntries().size());
        List<KtAnnotationEntry> annotationEntries = ktFileAnnotationList.getAnnotationEntries();
        for (int i = 0; i < annotationEntries.size(); i++) {
            KtAnnotationEntry annotation = annotationEntries.get(i);
            J.Annotation ann = (J.Annotation) annotation.accept(this, data);
            System.out.println();
        }
        return annotations;
    }

    private J.Modifier mapModifier(PsiElement modifier, List<J.Annotation> annotations) {
        Space prefix = prefix(modifier);
        J.Modifier.Type type;
        String keyword = null;
        switch (modifier.getText()) {
            case "public":
                type = J.Modifier.Type.Public;
                break;
            case "protected":
                type = J.Modifier.Type.Protected;
                break;
            case "private":
                type = J.Modifier.Type.Private;
                break;
            case "abstract":
                type = J.Modifier.Type.Abstract;
                break;
            case "val":
                type = J.Modifier.Type.Final;
                break;
            default:
                type = J.Modifier.Type.LanguageExtension;
                keyword = modifier.getText();
                break;
        }
        return new J.Modifier(
                randomId(),
                prefix,
                Markers.EMPTY,
                keyword,
                type,
                annotations
        );
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
}
