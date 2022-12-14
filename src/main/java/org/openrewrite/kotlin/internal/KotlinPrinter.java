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
package org.openrewrite.kotlin.internal;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.kotlin.tree.KRightPadded;
import org.openrewrite.kotlin.tree.KSpace;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

public class KotlinPrinter<P> extends KotlinVisitor<PrintOutputCapture<P>> {
    private final KotlinJavaPrinter delegate = new KotlinJavaPrinter();

    @Override
    public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (!(tree instanceof K)) {
            // re-route printing to the java printer
            return delegate.visit(tree, p);
        } else {
            return super.visit(tree, p);
        }
    }

    @Override
    public J visitJavaSourceFile(JavaSourceFile sourceFile, PrintOutputCapture<P> p) {
        K.CompilationUnit cu = (K.CompilationUnit) sourceFile;

        beforeSyntax(cu, Space.Location.COMPILATION_UNIT_PREFIX, p);

        JRightPadded<J.Package> pkg = cu.getPadding().getPackageDeclaration();
        if (pkg != null) {
            visit(pkg.getElement(), p);
            visitSpace(pkg.getAfter(), Space.Location.PACKAGE_SUFFIX, p);
        }

        for (JRightPadded<J.Import> imprt : cu.getPadding().getImports()) {
            visitRightPadded(imprt, KRightPadded.Location.TOP_LEVEL_STATEMENT_SUFFIX, p);
        }

        for (JRightPadded<Statement> statement : cu.getPadding().getStatements()) {
            visitRightPadded(statement, KRightPadded.Location.TOP_LEVEL_STATEMENT_SUFFIX, p);
        }

        visitSpace(cu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        afterSyntax(cu, p);
        return cu;
    }

    @Override
    public J visitFunctionType(K.FunctionType functionType, PrintOutputCapture<P> p) {
        if (functionType.getReceiver() != null) {
            visitRightPadded(functionType.getReceiver(), KRightPadded.Location.FUNCTION_TYPE_RECEIVER, p);
            p.out.append(".");
        }
        visit(functionType.getTypedTree(), p);
        return functionType;
    }

    private class KotlinJavaPrinter extends JavaPrinter<P> {
        @Override
        public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
            if (tree instanceof K) {
                // re-route printing back up to groovy
                return KotlinPrinter.this.visit(tree, p);
            } else {
                return super.visit(tree, p);
            }
        }

        @Override
        public J visitAnnotation(J.Annotation annotation, PrintOutputCapture<P> p) {
            beforeSyntax(annotation, Space.Location.ANNOTATION_PREFIX, p);
            boolean isKModifier = annotation.getMarkers().findFirst(Modifier.class).isPresent();
            if (!isKModifier) {
                p.append("@");
            }
            visit(annotation.getAnnotationType(), p);
            if (!isKModifier) {
                visitContainer("(", annotation.getPadding().getArguments(), JContainer.Location.ANNOTATION_ARGUMENTS, ",", ")", p);
            }
            afterSyntax(annotation, p);
            return annotation;
        }

        @Override
        public J visitBlock(J.Block block, PrintOutputCapture<P> p) {
            beforeSyntax(block, Space.Location.BLOCK_PREFIX, p);

            if (block.isStatic()) {
                p.append("static");
                visitRightPadded(block.getPadding().getStatic(), JRightPadded.Location.STATIC_INIT, p);
            }

            SingleExpressionBlock singleExpressionBlock = block.getMarkers().findFirst(SingleExpressionBlock.class).orElse(null);
            if (singleExpressionBlock != null) {
                p.out.append("=");
            }

            boolean omitParens = block.getMarkers().findFirst(OmitBraces.class).isPresent();
            if (!omitParens) {
                p.append("{");
            }
            visitStatements(block.getPadding().getStatements(), JRightPadded.Location.BLOCK_STATEMENT, p);
            visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
            if (!omitParens) {
                p.append("}");
            }
            afterSyntax(block, p);
            return block;
        }

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, PrintOutputCapture<P> p) {
            String kind;
            if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Class || classDecl.getKind() == J.ClassDeclaration.Kind.Type.Enum || classDecl.getKind() == J.ClassDeclaration.Kind.Type.Annotation) {
                kind = "class";
            } else if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Interface) {
                kind = "interface";
            } else {
                throw new IllegalStateException("Implement me.");
            }

            beforeSyntax(classDecl, Space.Location.CLASS_DECLARATION_PREFIX, p);
            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(classDecl.getLeadingAnnotations(), p);
            for (J.Modifier m : classDecl.getModifiers()) {
                visitModifier(m, p);
            }
            visit(classDecl.getAnnotations().getKind().getAnnotations(), p);
            visitSpace(classDecl.getAnnotations().getKind().getPrefix(), Space.Location.CLASS_KIND, p);
            p.append(kind);
            visit(classDecl.getName(), p);
            visitContainer("<", classDecl.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);

            if (classDecl.getImplements() != null) {
                visitContainer(":", classDecl.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, ",", null, p);
            }

            if (!classDecl.getBody().getMarkers().findFirst(OmitBraces.class).isPresent()) {
                visit(classDecl.getBody(), p);
            }
            afterSyntax(classDecl, p);
            return classDecl;
        }

        @Override
        public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
            beforeSyntax(lambda, Space.Location.LAMBDA_PREFIX, p);
            boolean omitBraces = lambda.getMarkers().findFirst(OmitBraces.class).isPresent();
            if (!omitBraces) {
                p.out.append('{');
            }
            visitMarkers(lambda.getParameters().getMarkers(), p);
            if (lambda.getParameters().isParenthesized()) {
                p.append('(');
                visitRightPadded(lambda.getParameters().getPadding().getParams(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
                p.append(')');
            } else {
                visitRightPadded(lambda.getParameters().getPadding().getParams(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
            }
            if (!lambda.getParameters().getParameters().isEmpty()) {
                visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
                p.out.append("->");
            }
            if (lambda.getBody() instanceof J.Block) {
                J.Block block = (J.Block) lambda.getBody();
                visitStatements(block.getPadding().getStatements(), JRightPadded.Location.BLOCK_STATEMENT, p);
                visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
            } else {
                visit(lambda.getBody(), p);
            }
            if (!omitBraces) {
                p.out.append('}');
            }
            afterSyntax(lambda, p);
            return lambda;
        }

        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
            beforeSyntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p);
            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(method.getLeadingAnnotations(), p);
            for (J.Modifier m : method.getModifiers()) {
                visitModifier(m, p);
            }

            boolean isInfix = method.getMarkers().findFirst(ReceiverType.class).isPresent();
            if (isInfix) {
                J.VariableDeclarations infixReceiver = (J.VariableDeclarations) method.getParameters().get(0);
                JRightPadded<J.VariableDeclarations.NamedVariable> receiver = infixReceiver.getPadding().getVariables().get(0);
                visitRightPadded(receiver, JRightPadded.Location.NAMED_VARIABLE, p);
                p.out.append(".");
            }

            J.TypeParameters typeParameters = method.getAnnotations().getTypeParameters();
            if (typeParameters != null) {
                visit(typeParameters.getAnnotations(), p);
                visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
                visitMarkers(typeParameters.getMarkers(), p);
                p.append("<");
                visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
                p.append(">");
            }

            visit(method.getName(), p);

            JContainer<Statement> params = method.getPadding().getParameters();
            beforeSyntax(params.getBefore(), params.getMarkers(), JContainer.Location.METHOD_DECLARATION_PARAMETERS.getBeforeLocation(), p);
            p.append("(");
            int i = isInfix ? 1 : 0;
            List<JRightPadded<Statement>> elements = params.getPadding().getElements();
            for (;i < elements.size(); i++) {
                JRightPadded<Statement> element = elements.get(i);
                String suffix = i == elements.size() - 1 ? "" : ",";
                visitRightPadded(element, JContainer.Location.METHOD_DECLARATION_PARAMETERS.getElementLocation(), suffix, p);
            }
            afterSyntax(params.getMarkers(), p);
            p.append(")");

            if (method.getReturnTypeExpression() != null) {
                TypeReferencePrefix typeReferencePrefix = method.getMarkers().findFirst(TypeReferencePrefix.class).orElse(null);
                if (typeReferencePrefix != null) {
                    KotlinPrinter.this.visitSpace(typeReferencePrefix.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p);
                    p.append(":");
                }
                visit(method.getReturnTypeExpression(), p);
            }
            visit(method.getBody(), p);
            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);

            visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, p);
            if (method.getSelect() != null && !method.getMarkers().findFirst(ReceiverType.class).isPresent()) {
                p.out.append(".");
            }

            visit(method.getName(), p);
            visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
            JContainer<Expression> argContainer = method.getPadding().getArguments();

            visitSpace(argContainer.getBefore(), Space.Location.METHOD_INVOCATION_ARGUMENTS, p);
            List<JRightPadded<Expression>> args = argContainer.getPadding().getElements();
            boolean omitParensOnMethod = method.getMarkers().findFirst(OmitParentheses.class).isPresent();
            for (int i = 0; i < args.size(); i++) {
                JRightPadded<Expression> arg = args.get(i);
                boolean omitParens = arg.getElement().getMarkers()
                        .findFirst(OmitParentheses.class)
                        .isPresent() ||
                        arg.getElement().getMarkers()
                                .findFirst(org.openrewrite.java.marker.OmitParentheses.class)
                                .isPresent();

                if (i == 0 && !omitParens && !omitParensOnMethod) {
                    p.out.append('(');
                } else if (i > 0 && omitParens && (
                        !args.get(0).getElement().getMarkers().findFirst(OmitParentheses.class).isPresent() &&
                                !args.get(0).getElement().getMarkers().findFirst(org.openrewrite.java.marker.OmitParentheses.class).isPresent()
                )) {
                    p.out.append(')');
                } else if (i > 0) {
                    p.out.append(',');
                }

                visitRightPadded(arg, JRightPadded.Location.METHOD_INVOCATION_ARGUMENT, p);

                if (i == args.size() - 1 && !omitParens && !omitParensOnMethod) {
                    p.out.append(')');
                }
            }

            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitNewClass(J.NewClass newClass, PrintOutputCapture<P> p) {
            AnonymousObjectPrefix anonymousObjectPrefix = newClass.getMarkers().findFirst(AnonymousObjectPrefix.class).orElse(null);
            if (anonymousObjectPrefix != null) {
                KotlinPrinter.this.visitSpace(anonymousObjectPrefix.getPrefix(), KSpace.Location.ANONYMOUS_OBJECT_PREFIX, p);
                p.out.append("object");
            }

            beforeSyntax(newClass, Space.Location.NEW_CLASS_PREFIX, p);

            if (anonymousObjectPrefix != null) {
                p.out.append(":");
            }

            visitRightPadded(newClass.getPadding().getEnclosing(), JRightPadded.Location.NEW_CLASS_ENCLOSING, ".", p);
            visitSpace(newClass.getNew(), Space.Location.NEW_PREFIX, p);
            visit(newClass.getClazz(), p);
            if (!newClass.getPadding().getArguments().getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                visitContainer("(", newClass.getPadding().getArguments(), JContainer.Location.NEW_CLASS_ARGUMENTS, ",", ")", p);
            }
            visit(newClass.getBody(), p);
            afterSyntax(newClass, p);
            return newClass;
        }

        @Override
        public J visitReturn(J.Return retrn, PrintOutputCapture<P> p) {
            if (retrn.getMarkers().findFirst(ImplicitReturn.class).isPresent() ||
                    retrn.getMarkers().findFirst(org.openrewrite.java.marker.ImplicitReturn.class).isPresent()) {
                visitSpace(retrn.getPrefix(), Space.Location.RETURN_PREFIX, p);
                visitMarkers(retrn.getMarkers(), p);
                visit(retrn.getExpression(), p);
                afterSyntax(retrn, p);
                return retrn;
            }
            return super.visitReturn(retrn, p);
        }

        @Override
        public J visitTypeParameter(J.TypeParameter typeParam, PrintOutputCapture<P> p) {
            beforeSyntax(typeParam, Space.Location.TYPE_PARAMETERS_PREFIX, p);
            visit(typeParam.getAnnotations(), p);
            visit(typeParam.getName(), p);
            visitContainer(":", typeParam.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, "&", "", p);
            afterSyntax(typeParam, p);
            return typeParam;
        }

        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
            beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(multiVariable.getLeadingAnnotations(), p);
            for (J.Modifier m : multiVariable.getModifiers()) {
                visitModifier(m, p);
            }

            PropertyClassifier propertyClassifier = multiVariable.getMarkers().findFirst(PropertyClassifier.class).orElse(null);
            if (propertyClassifier != null) {
                boolean isVal = propertyClassifier.getClassifierType() == PropertyClassifier.ClassifierType.VAL;
                p.append(propertyClassifier.getPrefix().getWhitespace());
                p.append(isVal ? "val" : "var");
            }

            boolean containsTypeReceiver = multiVariable.getMarkers().findFirst(ReceiverType.class).isPresent();
            int variablePos = 0;
            if (containsTypeReceiver) {
                JRightPadded<J.VariableDeclarations.NamedVariable> receiver = multiVariable.getPadding().getVariables().get(0);
                visitRightPadded(receiver, JRightPadded.Location.NAMED_VARIABLE, p);
                p.out.append(".");
                variablePos = 1;
            }

            J.VariableDeclarations.NamedVariable variable = multiVariable.getVariables().get(variablePos);
            beforeSyntax(variable, Space.Location.VARIABLE_PREFIX, p);
            visit(variable.getName(), p);

            if (multiVariable.getTypeExpression() != null) {
                TypeReferencePrefix typeReferencePrefix = multiVariable.getMarkers().findFirst(TypeReferencePrefix.class).orElse(null);
                if (typeReferencePrefix != null) {
                    KotlinPrinter.this.visitSpace(typeReferencePrefix.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p);
                    p.append(":");
                }
                visit(multiVariable.getTypeExpression(), p);
                IsNullable isNullable = multiVariable.getTypeExpression().getMarkers().findFirst(IsNullable.class).orElse(null);
                if (isNullable != null) {
                    KotlinPrinter.this.visitSpace(isNullable.getPrefix(), KSpace.Location.IS_NULLABLE_PREFIX, p);
                    p.append("?");
                }
            }

            boolean implicitEquals = variable.getInitializer() instanceof K.StatementExpression &&
                    ((K.StatementExpression) variable.getInitializer()).getStatement() instanceof J.MethodDeclaration;
            visitLeftPadded(implicitEquals ? "" : "=", variable.getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p);

            visitMarkers(multiVariable.getPadding().getVariables().get(0).getMarkers(), p);
            afterSyntax(multiVariable, p);
            return multiVariable;
        }

        protected void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location, PrintOutputCapture<P> p) {
            if (paddedStat != null) {
                visit(paddedStat.getElement(), p);
                visitSpace(paddedStat.getAfter(), location.getAfterLocation(), p);
                visitMarkers(paddedStat.getMarkers(), p);
            }
        }

        @Override
        protected void visitContainer(String before, @Nullable JContainer<? extends J> container, JContainer.Location location,
                                      String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
            if (container == null) {
                return;
            }
            beforeSyntax(container.getBefore(), container.getMarkers(), location.getBeforeLocation(), p);
            p.append(before);
            visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
            afterSyntax(container.getMarkers(), p);
            p.append(after == null ? "" : after);
        }

        @Override
        public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
            if (marker instanceof Semicolon) {
                p.out.append(';');
            } else if (marker instanceof Reified) {
                p.out.append("reified");
            }

            return super.visitMarker(marker, p);
        }
    }

    @Override
    public Space visitSpace(Space space, KSpace.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, loc, p);
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        return delegate.visitMarker(marker, p);
    }

    private static final UnaryOperator<String> JAVA_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    private void beforeSyntax(K k, Space.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(k.getPrefix(), k.getMarkers(), loc, p);
    }

    private void beforeSyntax(Space prefix, Markers markers, @Nullable Space.Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
        if (loc != null) {
            visitSpace(prefix, loc, p);
        }
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(K k, PrintOutputCapture<P> p) {
        afterSyntax(k.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }
}
