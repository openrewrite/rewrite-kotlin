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
package org.openrewrite.kotlin.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.internal.KotlinPrinter;
import org.openrewrite.kotlin.marker.Semicolon;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.util.*;


@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveTrailingSemicolon extends Recipe {
    @Override
    public @NotNull String getDisplayName() {
        return "Remove unnecessary trailing semicolon";
    }

    @Override
    public @NotNull String getDescription() {
        return "Some Java programmers may mistakenly add semicolons at the end when writing Kotlin code, but in " +
               "reality, they are not necessary.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {

        return new KotlinIsoVisitor<ExecutionContext>() {
            Set<J> semiColonRemovable;

            @Override
            public K.@NotNull CompilationUnit visitCompilationUnit(@NotNull K.CompilationUnit cu,
                                                                   @NotNull ExecutionContext ctx) {
                semiColonRemovable = new HashSet<>();
                CollectSemicolonRemovableElements.collect(cu.print(getCursor()), cu, semiColonRemovable);
                cu = cu.getPadding().withImports(removeSemiColon(cu.getPadding().getImports()));
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public <T> @NotNull JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right,
                                                                 @NotNull JRightPadded.Location loc,
                                                                 @NotNull ExecutionContext ctx) {
                right = super.visitRightPadded(right, loc, ctx);

                if (right.getElement() instanceof J && !semiColonRemovable.contains(right.getElement())) {
                    return right;
                }

                Markers markers = right.getMarkers();
                markers = markers.withMarkers(ListUtils.map(markers.getMarkers(), marker ->
                        marker instanceof Semicolon ? null : marker
                ));

                return right.withMarkers(markers);
            }

        };
    }


    private static <T> List<JRightPadded<T>> removeSemiColon(List<JRightPadded<T>> rps) {
        return ListUtils.map(rps, RemoveTrailingSemicolon::removeSemiColon);
    }

    private static<T> JRightPadded<T> removeSemiColon(JRightPadded<T> rp) {
        Markers markers = rp.getMarkers();
        markers = markers.withMarkers(ListUtils.map(markers.getMarkers(), marker ->
                marker instanceof Semicolon ? null : marker
        ));
        return rp.withMarkers(markers);
    }

    private static class MyKotlinJavaPrinter extends KotlinPrinter.KotlinJavaPrinter<Set<J>> {
        private final String sourceCode;

        MyKotlinJavaPrinter(KotlinPrinter kp, String sourceCode) {
            super(kp);
            this.sourceCode = sourceCode;
        }

        @Override
        public @NotNull J visitVariableDeclarations(@NotNull J.VariableDeclarations multiVariable,
                                                    @NotNull PrintOutputCapture<Set<J>> p) {
            J mv = super.visitVariableDeclarations(multiVariable, p);
            if (startsWithNewLineAfterOffset(sourceCode, p.out.length())) {
                for (J.VariableDeclarations.NamedVariable v : multiVariable.getVariables()) {
                    p.getContext().add(v);
                }
            }
            return mv;
        }

        @Override
        public @NotNull J visitMethodInvocation(@NotNull J.MethodInvocation method, @NotNull PrintOutputCapture<Set<J>> p) {
            J m = super.visitMethodInvocation(method, p);

            if (startsWithNewLineAfterOffset(sourceCode, p.out.length())) {
                p.getContext().add(m);
            }

            return m;
        }

        @Override
        public @NotNull J visitBlock(@NotNull J.Block block, @NotNull PrintOutputCapture<Set<J>> p) {
            J.Block b = (J.Block) super.visitBlock(block, p);

            b = b.getPadding().withStatements(ListUtils.map(b.getPadding().getStatements(), rp -> {
                if (rp.getElement() instanceof J.If) {
                    p.getContext().add(rp.getElement());
                } else if (rp.getElement() instanceof K.KReturn) {
                    p.getContext().add(rp.getElement());
                }

                return rp;
            }));
            return b;
        }

        public static boolean startsWithNewLineAfterOffset(String str, int offset) {
            if (offset >= str.length()) {
                return false;
            }

            for (int i = offset; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c != ' ' && c != '\t' && c != ';') {
                    return c == '\n';
                }
            }
            return false;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class CollectSemicolonRemovableElements extends KotlinPrinter<Set<J>> {

        public static void collect(String sourceCode, J j, Set<J> SemicolonRemovable) {
            new CollectSemicolonRemovableElements(sourceCode).visit(j, new PrintOutputCapture<>(SemicolonRemovable));
        }

        CollectSemicolonRemovableElements(String sourceCode) {
            super();
            setDelegate(new MyKotlinJavaPrinter(this, sourceCode));
        }
    }
}
