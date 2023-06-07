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
import org.openrewrite.kotlin.tree.KContainer;
import org.openrewrite.kotlin.tree.KRightPadded;
import org.openrewrite.kotlin.tree.KSpace;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Objects;
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
    public J visitCompilationUnit(K.CompilationUnit sourceFile, PrintOutputCapture<P> p) {
        K.CompilationUnit cu = sourceFile;

        beforeSyntax(cu, Space.Location.COMPILATION_UNIT_PREFIX, p);

        visit((sourceFile).getAnnotations(), p);

        JRightPadded<J.Package> pkg = cu.getPadding().getPackageDeclaration();
        if (pkg != null) {
            visit(pkg.getElement(), p);
            visitSpace(pkg.getAfter(), Space.Location.PACKAGE_SUFFIX, p);
        }

        for (JRightPadded<J.Import> import_ : cu.getPadding().getImports()) {
            visitRightPadded(import_, KRightPadded.Location.TOP_LEVEL_STATEMENT_SUFFIX, p);
        }

        for (JRightPadded<Statement> statement : cu.getPadding().getStatements()) {
            visitRightPadded(statement, KRightPadded.Location.TOP_LEVEL_STATEMENT_SUFFIX, p);
        }

        visitSpace(cu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        afterSyntax(cu, p);
        return cu;
    }

    @Override
    public J visitBinary(K.Binary binary, PrintOutputCapture<P> p) {
        String keyword = "";
        switch (binary.getOperator()) {
            case Contains:
                keyword = "in";
                break;
            case IdentityEquals:
                keyword = "===";
                break;
            case IdentityNotEquals:
                keyword = "!==";
                break;
            case RangeTo:
                keyword = "..";
                break;
        }
        beforeSyntax(binary, KSpace.Location.BINARY_PREFIX, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), KSpace.Location.BINARY_OPERATOR, p);
        p.append(keyword);
        if (binary.getOperator() == K.Binary.Type.Get) {
            p.append("[");
        }

        visit(binary.getRight(), p);
        afterSyntax(binary, p);

        visitSpace(binary.getAfter(), KSpace.Location.BINARY_SUFFIX, p);
        if (binary.getOperator() == K.Binary.Type.Get) {
            p.append("]");
        }
        return binary;
    }

    @Override
    public J visitFunctionType(K.FunctionType functionType, PrintOutputCapture<P> p) {
        if (functionType.getSuspendModifier() != null) {
            visitAnnotation(functionType.getSuspendModifier(), p);
        }
        if (functionType.getReceiver() != null) {
            visitRightPadded(functionType.getReceiver(), KRightPadded.Location.FUNCTION_TYPE_RECEIVER, p);
            p.append(".");
        }
        visit(functionType.getTypedTree(), p);
        return functionType;
    }

    @Override
    public J visitKReturn(K.KReturn kReturn, PrintOutputCapture<P> p) {
        visit(kReturn.getExpression(), p);
        if (kReturn.getLabel() != null) {
            p.append("@");
            visit(kReturn.getLabel(), p);
        }
        return kReturn;
    }

    @Override
    public J visitKString(K.KString kString, PrintOutputCapture<P> p) {
        beforeSyntax(kString, KSpace.Location.KSTRING_PREFIX, p);

        String delimiter = kString.getDelimiter();
        p.append(delimiter);

        visit(kString.getStrings(), p);
        p.append(delimiter);

        afterSyntax(kString, p);
        return kString;
    }

    @Override
    public J visitKStringValue(K.KString.Value value, PrintOutputCapture<P> p) {
        beforeSyntax(value, KSpace.Location.KSTRING_PREFIX, p);
        if (value.isEnclosedInBraces()) {
            p.append("${");
        } else {
            p.append("$");
        }
        visit(value.getTree(), p);
        if (value.isEnclosedInBraces()) {
            p.append('}');
        }
        afterSyntax(value, p);
        return value;
    }

    @Override
    public J visitListLiteral(K.ListLiteral listLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(listLiteral, KSpace.Location.LIST_LITERAL_PREFIX, p);
        visitContainer("[", listLiteral.getPadding().getElements(), KContainer.Location.LIST_LITERAL_ELEMENTS,
                ",", "]", p);
        afterSyntax(listLiteral, p);
        return listLiteral;
    }

    @Override
    public J visitWhen(K.When when, PrintOutputCapture<P> p) {
        beforeSyntax(when, KSpace.Location.WHEN_PREFIX, p);
        p.append("when");
        visit(when.getSelector(), p);
        visit(when.getBranches(), p);

        afterSyntax(when, p);
        return when;
    }

    @Override
    public J visitWhenBranch(K.WhenBranch whenBranch, PrintOutputCapture<P> p) {
        beforeSyntax(whenBranch, KSpace.Location.WHEN_BRANCH_PREFIX, p);
        visitContainer("", whenBranch.getPadding().getExpressions(), KContainer.Location.WHEN_BRANCH_EXPRESSION, ",", "->", p);
        visit(whenBranch.getBody(), p);
        return whenBranch;
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

            AnnotationCallSite callSite = annotation.getMarkers().findFirst(AnnotationCallSite.class).orElse(null);
            if (callSite != null) {
                p.append(callSite.getName());
                KotlinPrinter.this.visitSpace(callSite.getSuffix(), KSpace.Location.FILE_ANNOTATION_SUFFIX, p);
                p.append(":");
            }
            visit(annotation.getAnnotationType(), p);
            if (!isKModifier) {
                visitContainer("(", annotation.getPadding().getArguments(), JContainer.Location.ANNOTATION_ARGUMENTS, ",", ")", p);
            }
            afterSyntax(annotation, p);
            return annotation;
        }

        @Override
        public J visitBinary(J.Binary binary, PrintOutputCapture<P> p) {
            String keyword = "";
            switch (binary.getOperator()) {
                case Addition:
                    keyword = "+";
                    break;
                case Subtraction:
                    keyword = "-";
                    break;
                case Multiplication:
                    keyword = "*";
                    break;
                case Division:
                    keyword = "/";
                    break;
                case Modulo:
                    keyword = "%";
                    break;
                case LessThan:
                    keyword = "<";
                    break;
                case GreaterThan:
                    keyword = ">";
                    break;
                case LessThanOrEqual:
                    keyword = "<=";
                    break;
                case GreaterThanOrEqual:
                    keyword = ">=";
                    break;
                case Equal:
                    keyword = "==";
                    break;
                case NotEqual:
                    keyword = "!=";
                    break;
                case BitAnd:
                    keyword = "&";
                    break;
                case BitOr:
                    keyword = "|";
                    break;
                case BitXor:
                    keyword = "^";
                    break;
                case LeftShift:
                    keyword = "<<";
                    break;
                case RightShift:
                    keyword = ">>";
                    break;
                case UnsignedRightShift:
                    keyword = ">>>";
                    break;
                case Or:
                    keyword = (binary.getMarkers().findFirst(LogicalComma.class).isPresent()) ? "," : "||";
                    break;
                case And:
                    keyword = "&&";
                    break;
            }
            beforeSyntax(binary, Space.Location.BINARY_PREFIX, p);
            visit(binary.getLeft(), p);
            visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
            p.append(keyword);
            visit(binary.getRight(), p);
            afterSyntax(binary, p);
            return binary;
        }

        @Override
        public J visitBlock(J.Block block, PrintOutputCapture<P> p) {
            beforeSyntax(block, Space.Location.BLOCK_PREFIX, p);

            if (block.isStatic()) {
                p.append("init");
                visitRightPadded(block.getPadding().getStatic(), JRightPadded.Location.STATIC_INIT, p);
            }

            boolean singleExpressionBlock = block.getMarkers().findFirst(SingleExpressionBlock.class).isPresent();
            if (singleExpressionBlock) {
                p.append("=");
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
        public J visitBreak(J.Break breakStatement, PrintOutputCapture<P> p) {
            beforeSyntax(breakStatement, Space.Location.BREAK_PREFIX, p);
            p.append("break");
            if (breakStatement.getLabel() != null) {
                p.append("@");
            }
            visit(breakStatement.getLabel(), p);
            afterSyntax(breakStatement, p);
            return breakStatement;
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
            classDecl.getLeadingAnnotations().forEach(a -> {
                if (!a.getMarkers().findFirst(ExplicitInlineConstructor.class).isPresent()) {
                    visit(a, p);
                }
            });
            for (J.Modifier m : classDecl.getModifiers()) {
                visitModifier(m, p);
            }

            visit(classDecl.getAnnotations().getKind().getAnnotations(), p);
            visitSpace(classDecl.getAnnotations().getKind().getPrefix(), Space.Location.CLASS_KIND, p);

            KObject KObject = classDecl.getMarkers().findFirst(KObject.class).orElse(null);
            if (KObject != null) {
                p.append("object");
                if (classDecl.getLeadingAnnotations().stream().noneMatch(a -> a.getAnnotationType() instanceof J.Identifier && "companion".equals(((J.Identifier) a.getAnnotationType()).getSimpleName()))) {
                    visit(classDecl.getName(), p);
                }
            } else {
                p.append(kind);
                visit(classDecl.getName(), p);
            }

            classDecl.getLeadingAnnotations().forEach(a -> {
                if (a.getMarkers().findFirst(ExplicitInlineConstructor.class).isPresent()) {
                    visit(a, p);
                }
            });

            visitContainer("<", classDecl.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);

            if (classDecl.getPrimaryConstructor() != null) {
                // Record state vector is misleading and should, but is the only Java equivalent in the model.
                visitContainer("(", classDecl.getPadding().getPrimaryConstructor(), JContainer.Location.RECORD_STATE_VECTOR, ",", ")", p);
            }

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
        public J visitContinue(J.Continue continueStatement, PrintOutputCapture<P> p) {
            beforeSyntax(continueStatement, Space.Location.CONTINUE_PREFIX, p);
            p.append("continue");
            if (continueStatement.getLabel() != null) {
                p.append("@");
            }
            visit(continueStatement.getLabel(), p);
            afterSyntax(continueStatement, p);
            return continueStatement;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            beforeSyntax(fieldAccess, Space.Location.FIELD_ACCESS_PREFIX, p);
            visit(fieldAccess.getTarget(), p);
            String prefix = fieldAccess.getMarkers().findFirst(IsNullable.class).isPresent() ? "?." : ".";
            visitLeftPadded(prefix, fieldAccess.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p);
            afterSyntax(fieldAccess, p);
            return fieldAccess;
        }

        @Override
        public J visitForEachLoop(J.ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
            beforeSyntax(forEachLoop, Space.Location.FOR_EACH_LOOP_PREFIX, p);
            p.append("for");
            J.ForEachLoop.Control ctrl = forEachLoop.getControl();
            visitSpace(ctrl.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p);
            p.append('(');
            visitRightPadded(ctrl.getPadding().getVariable(), JRightPadded.Location.FOREACH_VARIABLE, "in", p);
            visitRightPadded(ctrl.getPadding().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, "", p);
            p.append(')');
            visitStatement(forEachLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
            afterSyntax(forEachLoop, p);
            return forEachLoop;
        }

        @Override
        public J visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
            beforeSyntax(ident, Space.Location.IDENTIFIER_PREFIX, p);
            p.append(ident.getSimpleName());
            IsNullable isNullable = ident.getMarkers().findFirst(IsNullable.class).orElse(null);
            if (isNullable != null) {
                KotlinPrinter.this.visitSpace(isNullable.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p);
                p.append("?");
            }
            CheckNotNull checkNotNull = ident.getMarkers().findFirst(CheckNotNull.class).orElse(null);
            if (checkNotNull != null) {
                KotlinPrinter.this.visitSpace(checkNotNull.getPrefix(), KSpace.Location.CHECK_NOT_NULL_PREFIX, p);
                p.append("!!");
            }
            afterSyntax(ident, p);
            return ident;
        }

        @Override
        public J visitImport(J.Import import_, PrintOutputCapture<P> p) {
            beforeSyntax(import_, Space.Location.IMPORT_PREFIX, p);
            p.append("import");
            visit(import_.getQualid(), p);
            JLeftPadded<J.Identifier> alias = import_.getPadding().getAlias();
            if (alias != null) {
                visitSpace(alias.getBefore(), Space.Location.IMPORT_ALIAS_PREFIX, p);
                p.append("as");
                visit(alias.getElement(), p);
            }
            afterSyntax(import_, p);
            return import_;
        }

        @Override
        public J visitInstanceOf(J.InstanceOf instanceOf, PrintOutputCapture<P> p) {
            beforeSyntax(instanceOf, Space.Location.INSTANCEOF_PREFIX, p);
            String suffix = instanceOf.getMarkers().findFirst(NotIs.class).isPresent() ? "!is" : "is";
            visitRightPadded(instanceOf.getPadding().getExpr(), JRightPadded.Location.INSTANCEOF, suffix, p);
            visit(instanceOf.getClazz(), p);
            visit(instanceOf.getPattern(), p);
            afterSyntax(instanceOf, p);
            return instanceOf;
        }

        @Override
        public J visitLabel(J.Label label, PrintOutputCapture<P> p) {
            beforeSyntax(label, Space.Location.LABEL_PREFIX, p);
            visitRightPadded(label.getPadding().getLabel(), JRightPadded.Location.LABEL, "@", p);
            visit(label.getStatement(), p);
            afterSyntax(label, p);
            return label;
        }

        @Override
        public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
            beforeSyntax(lambda, Space.Location.LAMBDA_PREFIX, p);
            boolean omitBraces = lambda.getMarkers().findFirst(OmitBraces.class).isPresent();
            if (!omitBraces) {
                p.append('{');
            }

            visitLambdaParameters(lambda.getParameters(), p);
            if (!lambda.getParameters().getParameters().isEmpty()) {
                visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
                p.append("->");
            }
            if (lambda.getBody() instanceof J.Block) {
                J.Block block = (J.Block) lambda.getBody();
                visitStatements(block.getPadding().getStatements(), JRightPadded.Location.BLOCK_STATEMENT, p);
                visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
            } else {
                visit(lambda.getBody(), p);
            }
            if (!omitBraces) {
                p.append('}');
            }
            afterSyntax(lambda, p);
            return lambda;
        }

        private void visitLambdaParameters(J.Lambda.Parameters parameters, PrintOutputCapture<P> p) {
            visitMarkers(parameters.getMarkers(), p);
            if (parameters.isParenthesized()) {
                visitSpace(parameters.getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p);
                p.append('(');
                visitRightPadded(parameters.getPadding().getParams(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
                p.append(')');
            } else {
                List<JRightPadded<J>> params = parameters.getPadding().getParams();
                for (int i = 0; i < params.size(); i++) {
                    JRightPadded<J> param = params.get(i);
                    if (param.getElement() instanceof J.Lambda.Parameters) {
                        visitLambdaParameters((J.Lambda.Parameters) param.getElement(), p);
                        visitSpace(param.getAfter(), JRightPadded.Location.LAMBDA_PARAM.getAfterLocation(), p);
                    } else {
                        visit(param.getElement(), p);
                        visitSpace(param.getAfter(), JRightPadded.Location.LAMBDA_PARAM.getAfterLocation(), p);
                        visitMarkers(param.getMarkers(), p);
                        if (i < params.size() - 1) {
                            p.append(',');
                        }
                    }
                }
            }
        }

        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
            if (method.getMarkers().findFirst(Implicit.class).isPresent()) {
                return method;
            }

            beforeSyntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p);
            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(method.getLeadingAnnotations(), p);
            for (J.Modifier m : method.getModifiers()) {
                visitModifier(m, p);
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

            boolean hasReceiverType = method.getMarkers().findFirst(ReceiverType.class).isPresent();
            if (hasReceiverType) {
                J.VariableDeclarations infixReceiver = (J.VariableDeclarations) method.getParameters().get(0);
                JRightPadded<J.VariableDeclarations.NamedVariable> receiver = infixReceiver.getPadding().getVariables().get(0);
                visitRightPadded(receiver, JRightPadded.Location.NAMED_VARIABLE, ".", p);
            }

            visit(method.getName(), p);

            JContainer<Statement> params = method.getPadding().getParameters();
            beforeSyntax(params.getBefore(), params.getMarkers(), JContainer.Location.METHOD_DECLARATION_PARAMETERS.getBeforeLocation(), p);
            p.append("(");
            int i = hasReceiverType ? 1 : 0;
            List<JRightPadded<Statement>> elements = params.getPadding().getElements();
            for (;i < elements.size(); i++) {
                JRightPadded<Statement> element = elements.get(i);
                if (element.getElement().getMarkers().findFirst(Implicit.class).isPresent()) {
                    continue;
                }
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
                if (method.getMarkers().findFirst(IsNullable.class).isPresent()) {
                    p.append("?");
                }
                p.append(".");
            }

            visit(method.getName(), p);
            visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
            JContainer<Expression> argContainer = method.getPadding().getArguments();

            visitSpace(argContainer.getBefore(), Space.Location.METHOD_INVOCATION_ARGUMENTS, p);
            List<JRightPadded<Expression>> args = argContainer.getPadding().getElements();
            boolean omitParensOnMethod = method.getMarkers().findFirst(OmitParentheses.class).isPresent();
            boolean isTrailingLambda = !args.isEmpty() && args.get(args.size() - 1).getElement().getMarkers().findFirst(TrailingLambdaArgument.class).isPresent();
            for (int i = 0; i < args.size(); i++) {
                JRightPadded<Expression> arg = args.get(i);

                // Print trailing lambda.
                if (i == args.size() - 1 && isTrailingLambda) {
                    visitSpace(arg.getAfter(), JRightPadded.Location.METHOD_INVOCATION_ARGUMENT.getAfterLocation(), p);
                    p.append(")");
                    visit(arg.getElement(), p);
                    break;
                }

                if (i == 0 && !omitParensOnMethod) {
                    p.append('(');
                } else if (i > 0 && omitParensOnMethod && (
                        !args.get(0).getElement().getMarkers().findFirst(OmitParentheses.class).isPresent() &&
                                !args.get(0).getElement().getMarkers().findFirst(org.openrewrite.java.marker.OmitParentheses.class).isPresent())) {
                    p.append(')');
                } else if (i > 0) {
                    p.append(',');
                }

                visitRightPadded(arg, JRightPadded.Location.METHOD_INVOCATION_ARGUMENT, p);

                if (i == args.size() - 1 && !omitParensOnMethod) {
                    p.append(')');
                }
            }

            CheckNotNull checkNotNull = method.getMarkers().findFirst(CheckNotNull.class).orElse(null);
            if (checkNotNull != null) {
                KotlinPrinter.this.visitSpace(checkNotNull.getPrefix(), KSpace.Location.CHECK_NOT_NULL_PREFIX, p);
                p.append("!!");
            }
            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitNewClass(J.NewClass newClass, PrintOutputCapture<P> p) {
            KObject kObject = newClass.getMarkers().findFirst(KObject.class).orElse(null);
            if (kObject != null) {
                KotlinPrinter.this.visitSpace(kObject.getPrefix(), KSpace.Location.OBJECT_PREFIX, p);
                p.append("object");
            }

            beforeSyntax(newClass, Space.Location.NEW_CLASS_PREFIX, p);

            if (kObject != null) {
                p.append(":");
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
        public J visitParameterizedType(J.ParameterizedType type, PrintOutputCapture<P> p) {
            beforeSyntax(type, Space.Location.PARAMETERIZED_TYPE_PREFIX, p);
            visit(type.getClazz(), p);
            visitContainer("<", type.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
            IsNullable isNullable = type.getMarkers().findFirst(IsNullable.class).orElse(null);
            if (isNullable != null) {
                KotlinPrinter.this.visitSpace(isNullable.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p);
                p.append("?");
            }
            afterSyntax(type, p);
            return type;
        }

        @Override
        public J visitReturn(J.Return return_, PrintOutputCapture<P> p) {
            if (return_.getMarkers().findFirst(ImplicitReturn.class).isPresent()) {
                visitSpace(return_.getPrefix(), Space.Location.RETURN_PREFIX, p);
                visitMarkers(return_.getMarkers(), p);
                visit(return_.getExpression(), p);
                afterSyntax(return_, p);
                return return_;
            }
            return super.visitReturn(return_, p);
        }

        @Override
        public J visitTernary(J.Ternary ternary, PrintOutputCapture<P> p) {
            beforeSyntax(ternary, Space.Location.TERNARY_PREFIX, p);
            visitLeftPadded("", ternary.getPadding().getTruePart(), JLeftPadded.Location.TERNARY_TRUE, p);
            visitLeftPadded("?:", ternary.getPadding().getFalsePart(), JLeftPadded.Location.TERNARY_FALSE, p);
            afterSyntax(ternary, p);
            return ternary;
        }

        @Override
        public J visitTypeCast(J.TypeCast typeCast, PrintOutputCapture<P> p) {
            beforeSyntax(typeCast, Space.Location.TYPE_CAST_PREFIX, p);
            boolean printParens = !typeCast.getMarkers().findFirst(OmitParentheses.class).isPresent();
            if (printParens) {
                p.append('(');
            }
            visit(typeCast.getExpression(), p);

            J.ControlParentheses<TypeTree> controlParens = typeCast.getClazz();
            beforeSyntax(controlParens, Space.Location.CONTROL_PARENTHESES_PREFIX, p);

            String as = typeCast.getMarkers().findFirst(IsNullable.class).isPresent() ? "as?" : "as";
            p.append(as);

            visit(controlParens.getTree(), p);
            if (printParens) {
                p.append(')');
            }
            afterSyntax(typeCast, p);
            return typeCast;
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
        public J visitWildcard(J.Wildcard wildcard, PrintOutputCapture<P> p) {
            beforeSyntax(wildcard, Space.Location.WILDCARD_PREFIX, p);
            if (wildcard.getPadding().getBound() != null) {
                p.append(wildcard.getPadding().getBound().getElement() == J.Wildcard.Bound.Super ? "in" : "out");
            }
            if (wildcard.getBoundedType() == null) {
                p.append('*');
            } else {
                visit(wildcard.getBoundedType(), p);
            }
            afterSyntax(wildcard, p);
            return wildcard;
        }

        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
            // TypeAliases are converted into a J.VariableDeclaration to re-use complex recipes like RenameVariable and ChangeType.
            // However, a type alias has different syntax and is printed separately to reduce code complexity in visitVariableDeclarations.
            // This is a temporary solution until K.TypeAlias is added to the model, and RenameVariable is revised to operator from a J.Identifier.
            if (multiVariable.getLeadingAnnotations().stream().anyMatch(it -> "typealias".equals(it.getSimpleName()))) {
                visitTypeAlias(multiVariable, p);
                return multiVariable;
            }

            beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(multiVariable.getLeadingAnnotations(), p);
            for (J.Modifier m : multiVariable.getModifiers()) {
                visitModifier(m, p);
            }

            boolean containsTypeReceiver = multiVariable.getMarkers().findFirst(ReceiverType.class).isPresent();
            // This may be changed after K.VariableDeclaration is added and getters and setters exist on the model.
            // The implicit receiver should be added to the first position of the methods.
            if (containsTypeReceiver) {
                Expression expression = multiVariable.getVariables().get(0).getInitializer();
                if (expression instanceof K.NamedVariableInitializer) {
                    for (J init : ((K.NamedVariableInitializer) expression).getInitializations()) {
                        if (init instanceof J.MethodDeclaration && "get".equals(((J.MethodDeclaration) init).getSimpleName())) {
                            J.MethodDeclaration getter = (J.MethodDeclaration) init;
                            JRightPadded<Statement> receiver = getter.getPadding().getParameters().getPadding().getElements().get(0);
                            visitRightPadded(receiver, JRightPadded.Location.NAMED_VARIABLE, p);
                            p.append(".");
                            break;
                        }
                    }
                }
            }

            List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = multiVariable.getPadding().getVariables();
            // V1: Covers and unique case in `mapForLoop` of the KotlinParserVisitor caused by how the FirElement represents for loops.
            for (int i = 0; i < variables.size(); i++) {
                JRightPadded<J.VariableDeclarations.NamedVariable> variable = variables.get(i);
                beforeSyntax(variable.getElement(), Space.Location.VARIABLE_PREFIX, p);
                if (variables.size() > 1 && !containsTypeReceiver && i == 0) {
                    p.append("(");
                }

                visit(variable.getElement().getName(), p);

                if (multiVariable.getTypeExpression() != null) {
                    TypeReferencePrefix typeReferencePrefix = multiVariable.getMarkers().findFirst(TypeReferencePrefix.class).orElse(null);
                    if (typeReferencePrefix != null) {
                        KotlinPrinter.this.visitSpace(typeReferencePrefix.getPrefix(), KSpace.Location.TYPE_REFERENCE_PREFIX, p);
                        p.append(":");
                    }
                    visit(multiVariable.getTypeExpression(), p);
                }

                visitSpace(variable.getAfter(), Space.Location.VARIABLE_INITIALIZER, p);
                if (i < variables.size() - 1) {
                    p.append(",");
                } else if (variables.size() > 1 && !containsTypeReceiver) {
                    p.append(")");
                }

                if (variable.getElement().getInitializer() != null) {
                    String equals = "=";
                    for (Marker marker : variable.getElement().getInitializer().getMarkers().getMarkers()) {
                        if (marker instanceof By) {
                            equals = "by";
                            break;
                        } else if (marker instanceof OmitEquals) {
                            equals = "";
                            break;
                        }
                    }

                    visitSpace(Objects.requireNonNull(variable.getElement().getPadding().getInitializer()).getBefore(), Space.Location.VARIABLE_INITIALIZER, p);
                    p.append(equals);
                }
                visit(variable.getElement().getInitializer(), p);
            }

            visitMarkers(multiVariable.getPadding().getVariables().get(0).getMarkers(), p);
            afterSyntax(multiVariable, p);
            return multiVariable;
        }

        @Override
        public J visitVariable(J.VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
            beforeSyntax(variable, Space.Location.VARIABLE_PREFIX, p);
            boolean isTypeReceiver = variable.getMarkers().findFirst(ReceiverType.class).isPresent();
            if (!isTypeReceiver) {
                visit(variable.getName(), p);
            }
            visitLeftPadded(isTypeReceiver ? "" : "=", variable.getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p);
            afterSyntax(variable, p);
            return variable;
        }

        private void visitTypeAlias(J.VariableDeclarations typeAlias, PrintOutputCapture<P> p) {
            beforeSyntax(typeAlias, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(typeAlias.getLeadingAnnotations(), p);
            for (J.Modifier m : typeAlias.getModifiers()) {
                visitModifier(m, p);
            }

            visit(typeAlias.getTypeExpression(), p);
            visitVariable(typeAlias.getPadding().getVariables().get(0).getElement(), p);
            visitMarkers(typeAlias.getPadding().getVariables().get(0).getMarkers(), p);
            afterSyntax(typeAlias, p);
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
                p.append(';');
            } else if (marker instanceof Reified) {
                p.append("reified");
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

    protected void visitContainer(String before, @Nullable JContainer<? extends J> container, KContainer.Location location,
                                  String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), location.getBeforeLocation(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
        p.append(after == null ? "" : after);
    }

    protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, KRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
        }
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        return delegate.visitMarker(marker, p);
    }

    private static final UnaryOperator<String> JAVA_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    private void beforeSyntax(K k, KSpace.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(k.getPrefix(), k.getMarkers(), loc, p);
    }

    private void beforeSyntax(J j, Space.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), loc, p);
    }

    private void beforeSyntax(Space prefix, Markers markers, @Nullable KSpace.Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
        if (loc != null) {
            visitSpace(prefix, loc, p);
        }
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }

    private void beforeSyntax(K k, Space.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(k.getPrefix(), k.getMarkers(), loc, p);
    }

    private void beforeSyntax(Space prefix, Markers markers, @Nullable Space.Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
        if (loc != null) {
            visitSpace(prefix, loc, p);
        }
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(J j, PrintOutputCapture<P> p) {
        afterSyntax(j.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }
}
