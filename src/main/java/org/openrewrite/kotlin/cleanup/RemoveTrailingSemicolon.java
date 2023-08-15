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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.internal.KotlinPrinter;
import org.openrewrite.kotlin.marker.Semicolon;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Marker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
            Set<Marker> semiColonRemovable;

            @Override
            public K.@NotNull CompilationUnit visitCompilationUnit(@NotNull K.CompilationUnit cu, @NotNull ExecutionContext ctx) {
                semiColonRemovable = CollectSemicolonRemovableElements.collect(cu);
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public <M extends Marker> M visitMarker(Marker marker, ExecutionContext ctx) {
                return semiColonRemovable.remove(marker) ? null : super.visitMarker(marker, ctx);
            }
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class CollectSemicolonRemovableElements extends KotlinPrinter<Set<Marker>> {

        Pattern WS = Pattern.compile("^\\s+");

        private class MyKotlinJavaPrinter extends KotlinPrinter.KotlinJavaPrinter<Set<Marker>> {

            private Integer mark;
            private Marker element;

            MyKotlinJavaPrinter(KotlinPrinter kp) {
                super(kp);
            }

            @Override
            public @Nullable J postVisit(J tree, PrintOutputCapture<Set<Marker>> p) {
                if (mark != null && getCursor().pollMessage("marked") != null) {
                    checkMark(p);
                }
                return tree;
            }

            private void mark(Marker element, @NotNull PrintOutputCapture<Set<Marker>> p) {
                if (getCursor().getParent() != null) {
                    getCursor().getParentTreeCursor().putMessage("marked", true);
                }
                mark = p.out.length();
                this.element = element;
            }

            @Override
            protected void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location, PrintOutputCapture<Set<Marker>> p) {
                if (paddedStat != null) {
                    visit(paddedStat.getElement(), p);
                    visitSpace(paddedStat.getAfter(), location.getAfterLocation(), p);
                    visitMarkers(paddedStat.getMarkers(), p);
                    paddedStat.getMarkers().getMarkers().stream().filter(m -> m instanceof Semicolon).findFirst().ifPresent(m -> mark(m, p));
                }
            }

            @Override
            public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<Set<Marker>> p) {
                Marker m = super.visitMarker(marker, p);
                if (marker instanceof Semicolon) {
                    mark(marker, p);
                }
                return (M) m;
            }

            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<Set<Marker>> p) {
                J vd = super.visitVariableDeclarations(multiVariable, p);
                if (!multiVariable.getVariables().isEmpty()) {
                    List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = multiVariable.getPadding().getVariables();
                    variables.get(variables.size() - 1).getMarkers().getMarkers().stream().filter(m -> m instanceof Semicolon).findFirst().ifPresent(m -> mark(m, p));
                }
                return vd;
            }

            @Override
            public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<Set<Marker>> p) {
                if (mark != null) {
                    checkMark(p);
                }
                return super.visitSpace(space, loc, p);
            }

            private void checkMark(PrintOutputCapture<Set<Marker>> p) {
                String substring = p.out.substring(mark);
                Matcher matcher = WS.matcher(substring);
                if (matcher.find()) {
                    if (matcher.group().indexOf('\n') != -1) {
                        p.getContext().add(element);
                    }
                    mark = null;
                }
            }
        }

        public static Set<Marker> collect(J j) {
            Set<Marker> removable = new HashSet<>();
            new CollectSemicolonRemovableElements().visit(j, new PrintOutputCapture<>(removable));
            return removable;
        }

        CollectSemicolonRemovableElements() {
            setDelegate(new MyKotlinJavaPrinter(this));
        }
    }
}
