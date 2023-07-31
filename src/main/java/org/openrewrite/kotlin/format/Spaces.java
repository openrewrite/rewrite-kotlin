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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.kotlin.style.SpacesStyle;

import static java.util.Objects.requireNonNull;

public class Spaces extends Recipe {

    @Override
    public String getDisplayName() {
        return "Spaces";
    }

    @Override
    public String getDescription() {
        return "Format whitespace in Kotlin code.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SpacesFromCompilationUnitStyle();
    }

    private static class SpacesFromCompilationUnitStyle extends KotlinIsoVisitor<ExecutionContext> {
        @Override
        public @Nullable J visit(@Nullable Tree tree, ExecutionContext executionContext) {
            if (!(tree instanceof K.CompilationUnit)) {
                return tree;
            }
            K.CompilationUnit cu = (K.CompilationUnit) tree;
            SpacesStyle style = cu.getStyle(SpacesStyle.class, IntelliJ.spaces());
            return new SpacesVisitor<>(style).visitNonNull(cu, getCursor().fork());
        }
    }

    public static <J2 extends J> J2 formatSpaces(J j, Cursor cursor) {
        SourceFile cu = cursor.firstEnclosingOrThrow(SourceFile.class);
        SpacesStyle style = cu.getStyle(SpacesStyle.class);
        //noinspection unchecked
        return (J2) new SpacesVisitor<>(style == null ? IntelliJ.spaces() : style).visitNonNull(j, 0, cursor);
    }
}
