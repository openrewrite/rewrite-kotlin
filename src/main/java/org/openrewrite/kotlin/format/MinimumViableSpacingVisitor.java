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
package org.openrewrite.kotlin.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.tree.K;

import java.util.List;

public class MinimumViableSpacingVisitor<P> extends KotlinIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @JsonCreator
    public MinimumViableSpacingVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    public MinimumViableSpacingVisitor() {
        this(null);
    }

    @Override
    public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, P p) {
        K.CompilationUnit kcu = super.visitCompilationUnit(cu, p);
        kcu = kcu.withStatements(ListUtils.map(kcu.getStatements(),
                (i, st) -> (i != 0) ? st.withPrefix(st.getPrefix().withWhitespace("\n")) : st));
        return kcu;
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);

        boolean first = c.getLeadingAnnotations().isEmpty();
        boolean hasFinalModifierOnly = (c.getModifiers().size() == 1) &&
                (c.getModifiers().get(0).getType() == J.Modifier.Type.Final);

        if (!hasFinalModifierOnly && !c.getModifiers().isEmpty()) {
            if (!first && Space.firstPrefix(c.getModifiers()).getWhitespace().isEmpty()) {
                c = c.withModifiers(Space.formatFirstPrefix(c.getModifiers(),
                        c.getModifiers().iterator().next().getPrefix().withWhitespace(" ")));
            }
            if (c.getModifiers().size() > 1) {
                c = c.withModifiers(ListUtils.map(c.getModifiers(), (index, modifier) -> {
                    if (index > 0 &&
                            modifier.getPrefix().getWhitespace().isEmpty() &&
                            modifier.getType() != J.Modifier.Type.Final) {
                        return modifier.withPrefix(modifier.getPrefix().withWhitespace(" "));
                    }
                    return modifier;
                }));
            }
            first = false;
        }

        if (c.getPadding().getKind().getPrefix().isEmpty()) {
            if (!first) {
                c = c.getPadding().withKind(c.getPadding().getKind().withPrefix(
                        c.getPadding().getKind().getPrefix().withWhitespace(" ")));
            }
            first = false;
        }

        if (!first && c.getName().getPrefix().getWhitespace().isEmpty()) {
            c = c.withName(c.getName().withPrefix(c.getName().getPrefix().withWhitespace(" ")));
        }

        J.ClassDeclaration.Padding padding = c.getPadding();
        JContainer<J.TypeParameter> typeParameters = padding.getTypeParameters();
        if (typeParameters != null && !typeParameters.getElements().isEmpty()) {
            if (!first && !typeParameters.getBefore().getWhitespace().isEmpty()) {
                c = padding.withTypeParameters(typeParameters.withBefore(typeParameters.getBefore().withWhitespace(" ")));
            }
        }

        if (c.getPadding().getExtends() != null) {
            Space before = c.getPadding().getExtends().getBefore();
            if (before.getWhitespace().isEmpty()) {
                c = c.getPadding().withExtends(c.getPadding().getExtends().withBefore(before.withWhitespace(" ")));
            }
        }

        if (c.getPadding().getImplements() != null) {
            Space before = c.getPadding().getImplements().getBefore();
            if (before.getWhitespace().isEmpty()) {
                c = c.getPadding().withImplements(c.getPadding().getImplements().withBefore(before.withWhitespace(" ")));
                c = c.withImplements(ListUtils.mapFirst(c.getImplements(), anImplements -> {
                    if (anImplements.getPrefix().getWhitespace().isEmpty()) {
                        return anImplements.withPrefix(anImplements.getPrefix().withWhitespace(" "));
                    }
                    return anImplements;
                }));
            }
        }

        c = c.withBody(c.getBody().withStatements(ListUtils.map(c.getBody().getStatements(),
                (i, st) -> (i != 0) ? st.withPrefix(st.getPrefix().withWhitespace("\n")) : st)));

        return c;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);

        boolean first = m.getLeadingAnnotations().isEmpty();
        if (!m.getModifiers().isEmpty()) {
            if (!first && Space.firstPrefix(m.getModifiers()).getWhitespace().isEmpty()) {
                m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(),
                        m.getModifiers().iterator().next().getPrefix().withWhitespace(" ")));
            }

            boolean firstFinal = m.getModifiers().get(0).getType() == J.Modifier.Type.Final;
            int startPosition = firstFinal ? 1 : 0;

            if (m.getModifiers().size() > 1) {
                m = m.withModifiers(ListUtils.map(m.getModifiers(), (index, modifier) -> {
                    if (index > startPosition && modifier.getPrefix().getWhitespace().isEmpty()) {
                        return modifier.withPrefix(modifier.getPrefix().withWhitespace(" "));
                    }
                    return modifier;
                }));
            }
            first = false;
        }

        J.TypeParameters typeParameters = m.getAnnotations().getTypeParameters();
        if (typeParameters != null && !typeParameters.getTypeParameters().isEmpty()) {
            if (!first && typeParameters.getPrefix().getWhitespace().isEmpty()) {
                m = m.getAnnotations().withTypeParameters(
                        typeParameters.withPrefix(
                                typeParameters.getPrefix().withWhitespace(" ")
                        )
                );
            }
            first = false;
        }

        if (m.getReturnTypeExpression() != null && m.getReturnTypeExpression().getPrefix().getWhitespace().isEmpty()) {
            if (!first) {
                TypeTree returnTypeExpression = m.getReturnTypeExpression();
                // If it's a J.AnnotatedType, because the first annotation has its prefix, so don't need to set the
                // prefix for the return type again to avoid two spaces, instead, we need to trim the prefix of the 1st
                // annotation to be single space.
                if (returnTypeExpression instanceof J.AnnotatedType) {
                    J.AnnotatedType annotatedType = (J.AnnotatedType) returnTypeExpression;
                    List<J.Annotation> annotations = ListUtils.mapFirst(annotatedType.getAnnotations(), annotation ->
                            annotation.withPrefix(annotation.getPrefix().withWhitespace(" "))
                    );
                    m = m.withReturnTypeExpression(annotatedType.withAnnotations(annotations));
                }
            }
            first = false;
        }
        if (!first) {
            m = m.withName(m.getName().withPrefix(m.getName().getPrefix().withWhitespace(" ")));
        }

        if (m.getPadding().getThrows() != null) {
            Space before = m.getPadding().getThrows().getBefore();
            if (before.getWhitespace().isEmpty()) {
                m = m.getPadding().withThrows(m.getPadding().getThrows().withBefore(before.withWhitespace(" ")));
            }
        }

        m = m.withBody(m.getBody().withStatements(ListUtils.map(m.getBody().getStatements(),
                (i, st) -> (i != 0) ? st.withPrefix(st.getPrefix().withWhitespace("\n")) : st)));

        return m;
    }

    @Override
    public J.Return visitReturn(J.Return return_, P p) {
        J.Return r = super.visitReturn(return_, p);
        if (r.getExpression() != null && r.getExpression().getPrefix().getWhitespace().isEmpty() &&
                !return_.getMarkers().findFirst(ImplicitReturn.class).isPresent()) {
            r = r.withExpression(r.getExpression().withPrefix(r.getExpression().getPrefix().withWhitespace(" ")));
        }
        return r;
    }

    @Override
    public K.Binary visitBinary(K.Binary binary, P p) {
        K.Binary kb = super.visitBinary(binary, p);
        if (kb.getOperator() ==  K.Binary.Type.Contains) {
            kb = kb.getPadding().withOperator(kb.getPadding().getOperator().withBefore(updateSpace(kb.getPadding().getOperator().getBefore(), true)));
            kb = kb.withRight(spaceBefore(kb.getRight(), true));
        }
        return kb;
    }

    @Override
    public J.If visitIf(J.If iff, P p) {
        J.If updatedIff = super.visitIf(iff, p);

        if (updatedIff.getElsePart() != null) {
            updatedIff = updatedIff.withElsePart(spaceBefore(updatedIff.getElsePart(), true));
            updatedIff = updatedIff.withElsePart(updatedIff.getElsePart().withBody(spaceBefore(updatedIff.getElsePart().getBody(), true)));
        }
        return updatedIff;
    }

    @Override
    public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
        J.ForEachLoop.Control c = super.visitForEachControl(control, p);
        c = c.getPadding().withVariable(c.getPadding().getVariable().withAfter(updateSpace(c.getPadding().getVariable().getAfter(), true)));
        c = c.withIterable(spaceBefore(c.getIterable(), true));
        return c;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, p);

        boolean first = v.getLeadingAnnotations().isEmpty();

        /*
         * We need at least one space between multiple modifiers, otherwise we could get a run-on like "publicstaticfinal".
         * Note, this is applicable anywhere that modifiers can exist, such as class declarations, etc.
         */
        if (!v.getModifiers().isEmpty()) {
            boolean needFirstSpace = !first;
            v = v.withModifiers(
                    ListUtils.map(v.getModifiers(), (index, modifier) -> {
                        if (index != 0 || needFirstSpace) {
                            if (modifier.getPrefix().getWhitespace().isEmpty()) {
                                modifier = modifier.withPrefix(modifier.getPrefix().withWhitespace(" "));
                            }
                        }
                        return modifier;
                    })
            );
        }

        J firstEnclosing = getCursor().getParentOrThrow().firstEnclosing(J.class);
        if (!(firstEnclosing instanceof J.Lambda)) {
            if (Space.firstPrefix(v.getVariables()).getWhitespace().isEmpty() && !v.getModifiers().isEmpty()) {
                v = v.withVariables(Space.formatFirstPrefix(v.getVariables(),
                        v.getVariables().iterator().next().getPrefix().withWhitespace(" ")));
            }
        }

        return v;
    }

    @Nullable
    @Override
    public J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
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

    private static <T extends J> T spaceBefore(T j, boolean spaceBefore) {
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

    private static Space updateSpace(Space s, boolean haveSpace) {
        if (!s.getComments().isEmpty()) {
            return s;
        }

        if (haveSpace && notSingleSpace(s.getWhitespace())) {
            return s.withWhitespace(" ");
        } else if (!haveSpace && onlySpacesAndNotEmpty(s.getWhitespace())) {
            return s.withWhitespace("");
        } else {
            return s;
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
}