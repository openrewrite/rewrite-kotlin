/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.kotlin.format;

import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
//import org.openrewrite.java.style.EmptyForInitializerPadStyle;
//import org.openrewrite.java.style.EmptyForIteratorPadStyle;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.kotlin.style.SpacesStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.tree.K;

import java.util.List;

public class SpacesVisitor<P> extends KotlinIsoVisitor<P> {

    @Nullable
    private final Tree stopAfter;

    private final SpacesStyle style;

    // Unconfigurable default formatting in IntelliJ's Kotlin formatting.
    private static final boolean beforeKeywords = true;
    private static final boolean beforeLeftBrace = true;
    private static final boolean beforeSemiColon = false;
    private static final boolean withinParentheses = false;

//    @Nullable
//    private final EmptyForInitializerPadStyle emptyForInitializerPadStyle;

//    @Nullable
//    private final EmptyForIteratorPadStyle emptyForIteratorPadStyle;

    public SpacesVisitor(SpacesStyle style) {
        this(style, null);
    }

    public SpacesVisitor(SpacesStyle style, @Nullable Tree stopAfter) {
        this.style = style;
//        this.emptyForInitializerPadStyle = emptyForInitializerPadStyle;
//        this.emptyForIteratorPadStyle = emptyForIteratorPadStyle;
        this.stopAfter = stopAfter;
    }

    <T extends J> T spaceBefore(T j, boolean spaceBefore) {
        if (!j.getComments().isEmpty()) {
            return j;
        }

        if (spaceBefore && notSingleSpace(j.getPrefix().getWhitespace())) {
            return j.withPrefix(j.getPrefix().withWhitespace(" "));
        } else if (!spaceBefore && onlySpacesAndNotEmpty(j.getPrefix().getWhitespace())) {
            return j.withPrefix(j.getPrefix().withWhitespace(""));
        } else {
            return j;
        }
    }

    <T> JContainer<T> spaceBefore(JContainer<T> container, boolean spaceBefore, boolean formatComment) {
        if (!container.getBefore().getComments().isEmpty()) {
            if (formatComment) {
                // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
                List<Comment> comments = spaceLastCommentSuffix(container.getBefore().getComments(), spaceBefore);
                return container.withBefore(container.getBefore().withComments(comments));
            }
            return container;
        }

        if (spaceBefore && notSingleSpace(container.getBefore().getWhitespace())) {
            return container.withBefore(container.getBefore().withWhitespace(" "));
        } else if (!spaceBefore && onlySpacesAndNotEmpty(container.getBefore().getWhitespace())) {
            return container.withBefore(container.getBefore().withWhitespace(""));
        } else {
            return container;
        }
    }

    <T extends J> JLeftPadded<T> spaceBefore(JLeftPadded<T> container, boolean spaceBefore) {
        if (!container.getBefore().getComments().isEmpty()) {
            return container;
        }

        if (spaceBefore && notSingleSpace(container.getBefore().getWhitespace())) {
            return container.withBefore(container.getBefore().withWhitespace(" "));
        } else if (!spaceBefore && onlySpacesAndNotEmpty(container.getBefore().getWhitespace())) {
            return container.withBefore(container.getBefore().withWhitespace(""));
        } else {
            return container;
        }
    }

    <T extends J> JLeftPadded<T> spaceBeforeLeftPaddedElement(JLeftPadded<T> container, boolean spaceBefore) {
        return container.withElement(spaceBefore(container.getElement(), spaceBefore));
    }

    <T extends J> JRightPadded<T> spaceBeforeRightPaddedElement(JRightPadded<T> container, boolean spaceBefore) {
        return container.withElement(spaceBefore(container.getElement(), spaceBefore));
    }

    <T extends J> JRightPadded<T> spaceAfter(JRightPadded<T> container, boolean spaceAfter) {
        if (!container.getAfter().getComments().isEmpty()) {
            // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
            List<Comment> comments = spaceLastCommentSuffix(container.getAfter().getComments(), spaceAfter);
            return container.withAfter(container.getAfter().withComments(comments));
        }

        if (spaceAfter && notSingleSpace(container.getAfter().getWhitespace())) {
            return container.withAfter(container.getAfter().withWhitespace(" "));
        } else if (!spaceAfter && onlySpacesAndNotEmpty(container.getAfter().getWhitespace())) {
            return container.withAfter(container.getAfter().withWhitespace(""));
        } else {
            return container;
        }
    }

    private static List<Comment> spaceLastCommentSuffix(List<Comment> comments, boolean spaceSuffix) {
        return ListUtils.mapLast(comments,
                comment -> spaceSuffix(comment, spaceSuffix));
    }

    private static Comment spaceSuffix(Comment comment, boolean spaceSuffix) {
        if (spaceSuffix && notSingleSpace(comment.getSuffix())) {
            return comment.withSuffix(" ");
        } else if (!spaceSuffix && onlySpacesAndNotEmpty(comment.getSuffix())) {
            return comment.withSuffix("");
        } else {
            return comment;
        }
    }

    /**
     * Checks if a string only contains spaces or tabs (excluding newline characters).
     *
     * @return true if contains spaces or tabs only, or true for empty string.
     */
    private static boolean onlySpaces(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    private static boolean onlySpacesAndNotEmpty(String s) {
        return !StringUtils.isNullOrEmpty(s) && onlySpaces(s);
    }

    private static boolean notSingleSpace(String str) {
        return onlySpaces(str) && !" ".equals(str);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        if (c.getBody() != null) {
            c = c.withBody(spaceBefore(c.getBody(), beforeLeftBrace));
            if (c.getBody().getStatements().isEmpty()) {
                if (c.getKind() != J.ClassDeclaration.Kind.Type.Enum) {
                    // Defaulted to `false` in IntelliJ's Kotlin formatting.
                    boolean withinCodeBraces = false;
                    if (!withinCodeBraces && c.getBody().getEnd().getWhitespace().equals(" ")) {
                        c = c.withBody(
                                c.getBody().withEnd(
                                        c.getBody().getEnd().withWhitespace("")
                                )
                        );
                    }
                } else {
                    // Defaulted to `false` in IntelliJ's Kotlin formatting.
                    boolean withinCodeBraces = false;
                    if (!withinCodeBraces && c.getBody().getEnd().getWhitespace().equals(" ")) {
                        c = c.withBody(c.getBody().withEnd(c.getBody().getEnd().withWhitespace("")));
                    }
                }
            }
        }
        // Defaulted to `true` in IntelliJ's Kotlin formatting.
        boolean beforeTypeParameterLeftAngleBracket = true;
        if (c.getPadding().getTypeParameters() != null) {
            c = c.getPadding().withTypeParameters(
                    spaceBefore(c.getPadding().getTypeParameters(),
                            beforeTypeParameterLeftAngleBracket, true)
            );
        }
        if (c.getPadding().getTypeParameters() != null) {
            // Defaulted to `false` in IntelliJ's Kotlin formatting.
            boolean spaceWithinAngleBrackets = false;
            int typeParametersSize = c.getPadding().getTypeParameters().getElements().size();
            c = c.getPadding().withTypeParameters(
                    c.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(c.getPadding().getTypeParameters().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index == 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), spaceWithinAngleBrackets)
                                            );
                                        } else {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(),
                                                            style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index == typeParametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, spaceWithinAngleBrackets);
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return c;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);

        // Defaulted to `false` in IntelliJ's Kotlin formatting.
        boolean beforeParenthesesOfMethodDeclaration = false;
        m = m.getPadding().withParameters(
                spaceBefore(m.getPadding().getParameters(), beforeParenthesesOfMethodDeclaration, false));

        // handle space before comma
        JContainer<Statement> jc = m.getPadding().getParameters();
        List<JRightPadded<Statement>> rps = jc.getPadding().getElements();
        if (rps.size() > 1) {
            int range = rps.size() - 1;
            rps = ListUtils.map(rps, (index, rp) -> (index < range) ? spaceAfter(rp, style.getOther().getBeforeComma()) : rp);
            m = m.getPadding().withParameters(jc.getPadding().withElements(rps));
        }

        // handle space after comma
        m = m.withParameters(ListUtils.map(
                m.getParameters(), (index, param) ->
                        index == 0 ? param : spaceBefore(param, style.getOther().getAfterComma())
        ));

        if (m.getBody() != null) {
            m = m.withBody(spaceBefore(m.getBody(), beforeLeftBrace));
        }
        if (m.getParameters().isEmpty() || m.getParameters().iterator().next() instanceof J.Empty) {
            m = m.getPadding().withParameters(
                    m.getPadding().getParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getParameters().getPadding().getElements(),
                                    param -> param.withElement(spaceBefore(param.getElement(), withinParentheses))
                            )
                    )
            );
        }
        if (m.getAnnotations().getTypeParameters() != null) {
            // Defaulted to `false` in IntelliJ's Kotlin formatting.
            boolean spaceWithinAngleBrackets = false;
            int typeParametersSize = m.getAnnotations().getTypeParameters().getTypeParameters().size();
            m = m.getAnnotations().withTypeParameters(
                    m.getAnnotations().getTypeParameters().getPadding().withTypeParameters(
                            ListUtils.map(m.getAnnotations().getTypeParameters().getPadding().getTypeParameters(),
                                    (index, elemContainer) -> {
                                        if (index == 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), spaceWithinAngleBrackets)
                                            );
                                        } else {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(),
                                                            style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index == typeParametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, spaceWithinAngleBrackets);
                                        }
                                        return elemContainer;
                                    })
                    )
            );
        }
        return m;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);

        boolean noParens = m.getMarkers().findFirst(OmitParentheses.class).isPresent();
        if (noParens && m.getPadding().getSelect() != null) {
            m = m.getPadding().withSelect(
                    spaceAfter(m.getPadding().getSelect(), true)
            );
        }
        // Defaulted to `false` if parens exist and to `true` if parens are omitted in Kotlin's formatting.
        m = m.getPadding().withArguments(spaceBefore(m.getPadding().getArguments(), noParens, false));
        if (m.getArguments().isEmpty() || m.getArguments().iterator().next() instanceof J.Empty) {
            // Defaulted to `false` in IntelliJ's Kotlin formatting.
            boolean withInEmptyMethodCallParentheses = false;
            m = m.getPadding().withArguments(
                    m.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(m.getPadding().getArguments().getPadding().getElements(),
                                    arg -> arg.withElement(spaceBefore(arg.getElement(), withInEmptyMethodCallParentheses))
                            )
                    )
            );
        } else {
            final int argsSize = m.getArguments().size();

            // Defaulted to `false` in IntelliJ's Kotlin formatting IFF parens exist.
            m = m.getPadding().withArguments(
                    m.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(m.getPadding().getArguments().getPadding().getElements(),
                                    (index, arg) -> {
                                        if (index == 0) {
                                            arg = arg.withElement(spaceBefore(arg.getElement(), false));
                                        } else {
                                            arg = arg.withElement(
                                                    spaceBefore(arg.getElement(), style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index == argsSize - 1) {
                                            arg = spaceAfter(arg, false);
                                        } else {
                                            arg = spaceAfter(arg, style.getOther().getBeforeComma());
                                        }
                                        return arg;
                                    }
                            )
                    )
            );
        }

        if (m.getPadding().getTypeParameters() != null) {
            // Defaulted to `false` in IntelliJ's Kotlin formatting.
            boolean typeArgumentsBeforeOpeningAngleBracket = false;
            m = m.getPadding().withTypeParameters(
                    spaceBefore(m.getPadding().getTypeParameters(),
                            typeArgumentsBeforeOpeningAngleBracket, true)
            );
            // Defaulted to `false` in IntelliJ's Kotlin formatting.
            boolean typeArgumentsAfterOpeningAngleBracket = false;
            m = m.withName(spaceBefore(m.getName(), typeArgumentsAfterOpeningAngleBracket));
        }
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(
                    m.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getTypeParameters().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index != 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(),
                                                            style.getOther().getAfterComma())
                                            );
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return m;
    }

    @Override
    public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, P p) {
        J.MultiCatch mc = super.visitMultiCatch(multiCatch, p);
        final int argsSize = mc.getAlternatives().size();

        // Defaulted to `true` in IntelliJ's Kotlin formatting.
        boolean aroundOperatorsBitwise = true;
        mc = mc.getPadding().withAlternatives(
                ListUtils.map(mc.getPadding().getAlternatives(),
                        (index, arg) -> {
                            if (index > 0) {
                                arg = arg.withElement(
                                        spaceBefore(arg.getElement(), aroundOperatorsBitwise)
                                );
                            }
                            if (index != argsSize - 1) {
                                arg = spaceAfter(arg, aroundOperatorsBitwise);
                            }
                            return arg;
                        }
                )
        );
        return mc;
    }

    @Override
    public J.If visitIf(J.If iff, P p) {
        J.If i = super.visitIf(iff, p);
        i = i.withIfCondition(spaceBefore(i.getIfCondition(), style.getBeforeParentheses().getIfParentheses()));

        i = i.getPadding().withThenPart(spaceBeforeRightPaddedElement(i.getPadding().getThenPart(), beforeLeftBrace));

        // Defaulted to `false` in IntelliJ's Kotlin formatting.
        boolean useSpaceWithinIfParentheses = false;
        i = i.withIfCondition(
                i.getIfCondition().getPadding().withTree(
                        spaceAfter(
                                i.getIfCondition().getPadding().getTree().withElement(
                                        spaceBefore(i.getIfCondition().getPadding().getTree().getElement(), useSpaceWithinIfParentheses
                                        )
                                ),
                                useSpaceWithinIfParentheses
                        )
                )
        );
        return i;
    }

    @Override
    public J.If.Else visitElse(J.If.Else else_, P p) {
        J.If.Else e = super.visitElse(else_, p);

        e = e.getPadding().withBody(spaceBeforeRightPaddedElement(e.getPadding().getBody(), beforeLeftBrace));

        // Defaulted to `true` in IntelliJ's Kotlin formatting.
        boolean beforeKeywordsElseKeyword = true;
        e = spaceBefore(e, beforeKeywordsElseKeyword);
        return e;
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop f = super.visitForEachLoop(forLoop, p);
        f = f.withControl(spaceBefore(f.getControl(), style.getBeforeParentheses().getForParentheses()));
        f = f.getPadding().withBody(spaceBeforeRightPaddedElement(f.getPadding().getBody(), beforeLeftBrace));
        boolean spaceWithinForParens = withinParentheses;
        f = f.withControl(
                f.getControl().withVariable(
                        spaceBefore(f.getControl().getVariable(), spaceWithinForParens)
                )
        );
        f = f.withControl(
                f.getControl().getPadding().withIterable(
                        spaceAfter(f.getControl().getPadding().getIterable(), spaceWithinForParens)
                )
        );
        boolean otherBeforeColonInForLoop = true;
        f = f.withControl(
                f.getControl().getPadding().withVariable(
                        spaceAfter(f.getControl().getPadding().getVariable(), otherBeforeColonInForLoop)
                )
        );
        return f;
    }

    @Override
    public K.When visitWhen(K.When when, P p) {
        K.When w = super.visitWhen(when, p);
        if (w.getSelector() != null) {
            w = w.withSelector(spaceBefore(w.getSelector(), style.getBeforeParentheses().getWhenParentheses()));
        }
        return w;
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = super.visitWhileLoop(whileLoop, p);
        w = w.withCondition(spaceBefore(w.getCondition(), style.getBeforeParentheses().getWhileParentheses()));
        w = w.getPadding().withBody(spaceBeforeRightPaddedElement(w.getPadding().getBody(), beforeLeftBrace));
        w = w.withCondition(
                w.getCondition().withTree(
                        spaceBefore(w.getCondition().getTree(), withinParentheses)
                )
        );
        w = w.withCondition(
                w.getCondition().getPadding().withTree(
                        spaceAfter(w.getCondition().getPadding().getTree(), withinParentheses)
                )
        );
        return w;
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, p);
        d = d.getPadding().withWhileCondition(spaceBefore(d.getPadding().getWhileCondition(), beforeKeywords));
        d = d.getPadding().withWhileCondition(spaceBeforeLeftPaddedElement(d.getPadding().getWhileCondition(), style.getBeforeParentheses().getWhileParentheses()));
        d = d.getPadding().withBody(spaceBeforeRightPaddedElement(d.getPadding().getBody(), beforeLeftBrace));
        d = d.withWhileCondition(
                d.getWhileCondition().withTree(
                        spaceBefore(d.getWhileCondition().getTree(), withinParentheses)
                )
        );
        d = d.withWhileCondition(
                d.getWhileCondition().getPadding().withTree(
                        spaceAfter(d.getWhileCondition().getPadding().getTree(), withinParentheses)
                )
        );
        return d;
    }

    @Override
    public J.Try visitTry(J.Try _try, P p) {
        J.Try t = super.visitTry(_try, p);
        t = t.withBody(spaceBefore(t.getBody(), beforeLeftBrace));
        if (t.getPadding().getFinally() != null) {
            JLeftPadded<J.Block> f = spaceBefore(t.getPadding().getFinally(), beforeKeywords);
            f = spaceBeforeLeftPaddedElement(f, beforeLeftBrace);
            t = t.getPadding().withFinally(f);
        }
        if (t.getResources() != null) {
            t = t.withResources(ListUtils.mapFirst(t.getResources(), res -> spaceBefore(res, withinParentheses)));
        }
        if (t.getPadding().getResources() != null) {
            t = t.getPadding().withResources(
                    t.getPadding().getResources().getPadding().withElements(
                            ListUtils.mapLast(t.getPadding().getResources().getPadding().getElements(),
                                    res -> spaceAfter(res, withinParentheses)
                            )
                    )
            );
        }
        return t;
    }

    @Override
    public J.Try.Catch visitCatch(J.Try.Catch _catch, P p) {
        J.Try.Catch c = super.visitCatch(_catch, p);
        c = spaceBefore(c, beforeKeywords);
        c = c.withParameter(spaceBefore(c.getParameter(), style.getBeforeParentheses().getCatchParentheses()));
        c = c.withBody(spaceBefore(c.getBody(), beforeLeftBrace));
        c = c.withParameter(
                c.getParameter().withTree(
                        spaceBefore(c.getParameter().getTree(), withinParentheses)
                )
        );
        c = c.withParameter(
                c.getParameter().getPadding().withTree(
                        spaceAfter(c.getParameter().getPadding().getTree(), withinParentheses)
                )
        );
        return c;
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, P p) {
        J.Annotation a = super.visitAnnotation(annotation, p);
        if (a.getPadding().getArguments() != null) {
            // Defaulted to `false` in IntelliJ's Kotlin formatting.
            boolean beforeParenthesesOfAnnotation = false;
            a = a.getPadding().withArguments(spaceBefore(a.getPadding().getArguments(),
                    beforeParenthesesOfAnnotation, true));
        }
        if (a.getPadding().getArguments() != null) {
            int argsSize = a.getPadding().getArguments().getElements().size();
            a = a.getPadding().withArguments(
                    a.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(a.getPadding().getArguments().getPadding().getElements(),
                                    (index, arg) -> {
                                        if (index == 0) {
                                            // don't overwrite changes made by before left brace annotation array
                                            // initializer setting when space within annotation parens is false
                                            if (withinParentheses || !beforeLeftBrace) {
                                                arg = arg.withElement(
                                                        spaceBefore(arg.getElement(), withinParentheses)
                                                );
                                            }
                                        } else {
                                            arg = arg.withElement(spaceBefore(arg.getElement(), style.getOther().getAfterComma()));
                                        }
                                        if (index == argsSize - 1) {
                                            arg = spaceAfter(arg, withinParentheses);
                                        }
                                        return arg;
                                    }
                            )
                    )
            );
        }
        return a;
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, P p) {
        J.Assignment a = super.visitAssignment(assignment, p);
        a = a.getPadding().withAssignment(spaceBefore(a.getPadding().getAssignment(), style.getAroundOperators().getAssignment()));
        a = a.getPadding().withAssignment(
                a.getPadding().getAssignment().withElement(
                        spaceBefore(a.getPadding().getAssignment().getElement(), style.getAroundOperators().getAssignment())
                )
        );
        return a;
    }

    @Override
    public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, p);
        J.AssignmentOperation.Padding padding = a.getPadding();
        JLeftPadded<J.AssignmentOperation.Type> operator = padding.getOperator();
        String operatorBeforeWhitespace = operator.getBefore().getWhitespace();
        if (style.getAroundOperators().getAssignment() && StringUtils.isNullOrEmpty(operatorBeforeWhitespace)) {
            a = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace(" ")
                    )
            );
        } else if (!style.getAroundOperators().getAssignment() && " ".equals(operatorBeforeWhitespace)) {
            a = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace("")
                    )
            );
        }
        a = a.withAssignment(spaceBefore(a.getAssignment(), style.getAroundOperators().getAssignment()));
        return a;
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, p);
        if (v.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(spaceBefore(v.getPadding().getInitializer(), style.getAroundOperators().getAssignment()));
        }
        if (v.getPadding().getInitializer() != null) {
            if (v.getPadding().getInitializer().getElement() != null) {
                v = v.getPadding().withInitializer(
                        v.getPadding().getInitializer().withElement(
                                spaceBefore(v.getPadding().getInitializer().getElement(), style.getAroundOperators().getAssignment())
                        )
                );
            }
        }
        return v;
    }

    @Override
    public J.Binary visitBinary(J.Binary binary, P p) {
        J.Binary b = super.visitBinary(binary, p);
        J.Binary.Type operator = b.getOperator();
        switch (operator) {
            case And:
            case Or:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getLogical());
                break;
            case Equal:
            case NotEqual:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getEquality());
                break;
            case LessThan:
            case LessThanOrEqual:
            case GreaterThan:
            case GreaterThanOrEqual:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getRelational());
                break;
            case Addition:
            case Subtraction:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getAdditive());
                break;
            case Multiplication:
            case Division:
            case Modulo:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getMultiplicative());
                break;
            case BitAnd:
            case BitOr:
            case BitXor:
            case LeftShift:
            case RightShift:
            case UnsignedRightShift:
                // around operators bitwise and shift are defaulted to true by IntelliJ formatting;
                b = applyBinarySpaceAround(b, true);
                break;
        }
        return b;
    }

    @Override
    public K.Binary visitBinary(K.Binary binary, P p) {
        K.Binary b = super.visitBinary(binary, p);
        K.Binary.Type operator = b.getOperator();
        switch (operator) {
            case Contains:
            case Get:
                break;
            case IdentityEquals:
            case IdentityNotEquals:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getEquality());
                break;
            case RangeTo:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getRange());
                break;
        }
        return b;
    }

    private J.Binary applyBinarySpaceAround(J.Binary binary, boolean useSpaceAround) {
        J.Binary.Padding padding = binary.getPadding();
        JLeftPadded<J.Binary.Type> operator = padding.getOperator();
        if (useSpaceAround) {
            if (StringUtils.isNullOrEmpty(operator.getBefore().getWhitespace())) {
                binary = padding.withOperator(
                        operator.withBefore(
                                operator.getBefore().withWhitespace(" ")
                        )
                );
            }
            if (StringUtils.isNullOrEmpty(binary.getRight().getPrefix().getWhitespace())) {
                binary = binary.withRight(
                        binary.getRight().withPrefix(
                                binary.getRight().getPrefix().withWhitespace(" ")
                        )
                );
            }
        } else {
            if (operator.getBefore().getWhitespace().equals(" ")) {
                binary = padding.withOperator(
                        operator.withBefore(
                                operator.getBefore().withWhitespace("")
                        )
                );
            }
            if (binary.getRight().getPrefix().getWhitespace().equals(" ")) {
                binary = binary.withRight(
                        binary.getRight().withPrefix(
                                binary.getRight().getPrefix().withWhitespace("")
                        )
                );
            }
        }
        return binary;
    }

    private K.Binary applyBinarySpaceAround(K.Binary binary, boolean useSpaceAround) {
        K.Binary.Padding padding = binary.getPadding();
        JLeftPadded<K.Binary.Type> operator = padding.getOperator();
        if (useSpaceAround) {
            if (StringUtils.isNullOrEmpty(operator.getBefore().getWhitespace())) {
                binary = padding.withOperator(
                        operator.withBefore(
                                operator.getBefore().withWhitespace(" ")
                        )
                );
            }
            if (StringUtils.isNullOrEmpty(binary.getRight().getPrefix().getWhitespace())) {
                binary = binary.withRight(
                        binary.getRight().withPrefix(
                                binary.getRight().getPrefix().withWhitespace(" ")
                        )
                );
            }
        } else {
            if (operator.getBefore().getWhitespace().equals(" ")) {
                binary = padding.withOperator(
                        operator.withBefore(
                                operator.getBefore().withWhitespace("")
                        )
                );
            }
            if (binary.getRight().getPrefix().getWhitespace().equals(" ")) {
                binary = binary.withRight(
                        binary.getRight().withPrefix(
                                binary.getRight().getPrefix().withWhitespace("")
                        )
                );
            }
        }
        return binary;
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, P p) {
        J.Unary u = super.visitUnary(unary, p);
        switch (u.getOperator()) {
            case PostIncrement:
            case PostDecrement:
                u = applyUnaryOperatorBeforeSpace(u, style.getAroundOperators().getUnary());
                break;
            case PreIncrement:
            case PreDecrement:
            case Negative:
            case Positive:
            case Not:
            case Complement:
                u = applyUnaryOperatorBeforeSpace(u, style.getAroundOperators().getUnary());
                u = applyUnaryOperatorExprSpace(u, style.getAroundOperators().getUnary());
                break;
        }
        return u;
    }

    private J.Unary applyUnaryOperatorExprSpace(J.Unary unary, boolean useAroundUnaryOperatorSpace) {
        if (useAroundUnaryOperatorSpace && StringUtils.isNullOrEmpty(unary.getExpression().getPrefix().getWhitespace())) {
            unary = unary.withExpression(
                    unary.getExpression().withPrefix(
                            unary.getExpression().getPrefix().withWhitespace(" ")
                    )
            );
        } else if (!useAroundUnaryOperatorSpace && unary.getExpression().getPrefix().getWhitespace().equals(" ")) {
            unary = unary.withExpression(
                    unary.getExpression().withPrefix(
                            unary.getExpression().getPrefix().withWhitespace("")
                    )
            );
        }
        return unary;
    }

    private J.Unary applyUnaryOperatorBeforeSpace(J.Unary u, boolean useAroundUnaryOperatorSpace) {
        J.Unary.Padding padding = u.getPadding();
        JLeftPadded<J.Unary.Type> operator = padding.getOperator();
        if (useAroundUnaryOperatorSpace && StringUtils.isNullOrEmpty(operator.getBefore().getWhitespace())) {
            u = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace(" ")
                    )
            );
        } else if (!useAroundUnaryOperatorSpace && operator.getBefore().getWhitespace().equals(" ")) {
            u = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace("")
                    )
            );
        }
        return u;
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = super.visitLambda(lambda, p);
        // FIXME. FunctionType wraps a lambda and has different settings. IntelliJ default only changes the before.
        boolean useSpaceAroundLambdaArrow = true; // style.getAroundOperators().getLambdaArrow();
        if (useSpaceAroundLambdaArrow && StringUtils.isNullOrEmpty(l.getArrow().getWhitespace())) {
            l = l.withArrow(
                    l.getArrow().withWhitespace(" ")
            );
        } else if (!useSpaceAroundLambdaArrow && l.getArrow().getWhitespace().equals(" ")) {
            l = l.withArrow(
                    l.getArrow().withWhitespace("")
            );
        }
        // FIXME. FunctionType wraps a lambda and has different settings. IntelliJ default only changes the before.
        l = l.withBody(spaceBefore(l.getBody(), useSpaceAroundLambdaArrow));
        if (!(l.getParameters().getParameters().isEmpty() || l.getParameters().getParameters().iterator().next() instanceof J.Empty)) {
            int parametersSize = l.getParameters().getParameters().size();
            l = l.withParameters(
                    l.getParameters().getPadding().withParams(
                            ListUtils.map(l.getParameters().getPadding().getParams(),
                                    (index, elemContainer) -> {
                                        if (index != 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index != parametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, style.getOther().getBeforeComma());
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return l;
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberRef, P p) {
        J.MemberReference m = super.visitMemberReference(memberRef, p);
        // Defaulted to `false` in IntelliJ's Kotlin formatting.
        boolean aroundOperatorsAfterMethodReferenceDoubleColon = false;
        m = m.getPadding().withContaining(
                spaceAfter(m.getPadding().getContaining(), aroundOperatorsAfterMethodReferenceDoubleColon)
        );

        // Defaulted to `false` in IntelliJ's Kotlin formatting.
        boolean aroundOperatorsBeforeMethodReferenceDoubleColon = false;
        if (m.getPadding().getTypeParameters() != null) {
            m.getPadding().withTypeParameters(spaceBefore(m.getPadding().getTypeParameters(), aroundOperatorsBeforeMethodReferenceDoubleColon, true));
        } else {
            m = m.getPadding().withReference(
                    spaceBefore(m.getPadding().getReference(), aroundOperatorsBeforeMethodReferenceDoubleColon)
            );
        }
        return m;
    }

    @Override
    public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        J.ArrayAccess a = super.visitArrayAccess(arrayAccess, p);
        if (a.getDimension().getPadding().getIndex().getElement().getPrefix().getWhitespace().equals(" ")) {
            a = a.withDimension(
                    a.getDimension().getPadding().withIndex(
                            a.getDimension().getPadding().getIndex().withElement(
                                    a.getDimension().getPadding().getIndex().getElement().withPrefix(
                                            a.getDimension().getPadding().getIndex().getElement().getPrefix().withWhitespace("")
                                    )
                            )
                    )
            );
        }
        if (a.getDimension().getPadding().getIndex().getAfter().getWhitespace().equals(" ")) {
            a = a.withDimension(
                    a.getDimension().getPadding().withIndex(
                            a.getDimension().getPadding().getIndex().withAfter(
                                    a.getDimension().getPadding().getIndex().getAfter().withWhitespace("")
                            )
                    )
            );
        }
        return a;
    }

    @Override
    public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, P p) {
        J.Parentheses<T> p2 = super.visitParentheses(parens, p);

        if (p2.getPadding().getTree().getElement().getPrefix().getWhitespace().equals(" ")) {
            p2 = p2.getPadding().withTree(
                    p2.getPadding().getTree().withElement(
                            p2.getPadding().getTree().getElement().withPrefix(
                                    p2.getPadding().getTree().getElement().getPrefix().withWhitespace("")
                            )
                    )
            );
        }
        if (p2.getPadding().getTree().getAfter().getWhitespace().equals(" ")) {
            p2 = p2.getPadding().withTree(
                    p2.getPadding().getTree().withAfter(
                            p2.getPadding().getTree().getAfter().withWhitespace("")
                    )
            );
        }
        return p2;
    }

    @Override
    public J.TypeCast visitTypeCast(J.TypeCast typeCast, P p) {
        J.TypeCast tc = super.visitTypeCast(typeCast, p);
        tc = tc.withClazz(
                tc.getClazz().withTree(
                        spaceBefore(tc.getClazz().getTree(), withinParentheses)
                )
        );
        tc = tc.withClazz(
                tc.getClazz().getPadding().withTree(
                        spaceAfter(tc.getClazz().getPadding().getTree(), withinParentheses)
                )
        );

        // TODO
//        tc = tc.withExpression(spaceBefore(tc.getExpression(), style.getOther().getAfterTypeCast()));
        return tc;
    }

//    @Override
//    public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, P p) {
//        J.ParameterizedType pt = super.visitParameterizedType(type, p);
//        boolean spaceWithinAngleBrackets = style.getWithin().getAngleBrackets();
//        if (pt.getPadding().getTypeParameters() != null) {
//            pt = pt.getPadding().withTypeParameters(
//                    spaceBefore(pt.getPadding().getTypeParameters(),
//                            style.getTypeArguments().getBeforeOpeningAngleBracket())
//            );
//        }
//        if (pt.getPadding().getTypeParameters() != null &&
//                !(pt.getPadding().getTypeParameters().getElements().isEmpty() || pt.getPadding().getTypeParameters().getElements().iterator().next() instanceof J.Empty)) {
//            int typeParametersSize = pt.getPadding().getTypeParameters().getElements().size();
//            pt = pt.getPadding().withTypeParameters(
//                    pt.getPadding().getTypeParameters().getPadding().withElements(
//                            ListUtils.map(pt.getPadding().getTypeParameters().getPadding().getElements(),
//                                    (index, elemContainer) -> {
//                                        if (index == 0) {
//                                            elemContainer = elemContainer.withElement(
//                                                    spaceBefore(elemContainer.getElement(), spaceWithinAngleBrackets)
//                                            );
//                                        } else {
//                                            elemContainer = elemContainer.withElement(
//                                                    spaceBefore(elemContainer.getElement(),
//                                                            style.getTypeArguments().getAfterComma())
//                                            );
//                                        }
//                                        if (index == typeParametersSize - 1) {
//                                            elemContainer = spaceAfter(elemContainer, spaceWithinAngleBrackets);
//                                        }
//                                        return elemContainer;
//                                    }
//                            )
//                    )
//            );
//        }
//        return pt;
//    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P p) {
        J.NewClass nc = super.visitNewClass(newClass, p);
        if (nc.getPadding().getArguments() != null) {
            nc = nc.getPadding().withArguments(spaceBefore(nc.getPadding().getArguments(), false, true));
            int argsSize = nc.getPadding().getArguments().getElements().size();
            nc = nc.getPadding().withArguments(
                    nc.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(nc.getPadding().getArguments().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index != 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index != argsSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, style.getOther().getBeforeComma());
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return nc;
    }

    @Override
    public J.EnumValue visitEnumValue(J.EnumValue _enum, P p) {
        J.EnumValue e = super.visitEnumValue(_enum, p);
        if (e.getInitializer() != null && e.getInitializer().getPadding().getArguments() != null) {
            int initializerArgumentsSize = e.getInitializer().getPadding().getArguments().getPadding().getElements().size();
            e = e.withInitializer(
                    e.getInitializer().getPadding().withArguments(
                            e.getInitializer().getPadding().getArguments().getPadding().withElements(
                                    ListUtils.map(e.getInitializer().getPadding().getArguments().getPadding().getElements(),
                                            (index, elemContainer) -> {
                                                if (index != 0) {
                                                    elemContainer = elemContainer.withElement(
                                                            spaceBefore(elemContainer.getElement(),
                                                                    style.getOther().getAfterComma())
                                                    );
                                                }
                                                if (index != initializerArgumentsSize - 1) {
                                                    elemContainer = spaceAfter(elemContainer,
                                                            style.getOther().getBeforeComma());
                                                }
                                                return elemContainer;
                                            }
                                    )
                            )
                    )
            );
        }
        return e;
    }

//    @Override
//    public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, P p) {
//        J.TypeParameter tp = super.visitTypeParameter(typeParam, p);
//        if (tp.getPadding().getBounds() != null) {
//            boolean spaceAroundTypeBounds = style.getTypeParameters().getAroundTypeBounds();
//            int typeBoundsSize = tp.getPadding().getBounds().getPadding().getElements().size();
//            tp = tp.getPadding().withBounds(
//                    tp.getPadding().getBounds().getPadding().withElements(
//                            ListUtils.map(tp.getPadding().getBounds().getPadding().getElements(),
//                                    (index, elemContainer) -> {
//                                        if (index != 0) {
//                                            elemContainer = elemContainer.withElement(spaceBefore(elemContainer.getElement(), spaceAroundTypeBounds));
//                                        }
//                                        if (index != typeBoundsSize - 1) {
//                                            elemContainer = spaceAfter(elemContainer, spaceAroundTypeBounds);
//                                        }
//                                        return elemContainer;
//                                    }
//                            )
//                    )
//            );
//        }
//        return tp;
//    }

    @Nullable
    @Override
    public J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().getRoot().putMessage("stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Nullable
    @Override
    public J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        return super.visit(tree, p);
    }
}
