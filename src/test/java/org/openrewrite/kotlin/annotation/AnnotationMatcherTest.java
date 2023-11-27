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
package org.openrewrite.kotlin.annotation;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class AnnotationMatcherTest implements RewriteTest {

    @interface AnnotationTest {
        String defaultValue();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {

            private static final AnnotationMatcher matcher = new AnnotationMatcher("@org.openrewrite.kotlin.annotation.AnnotationTest");

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {
                if (!matcher.matches(annotation)) {
                    return annotation;
                }

                return annotation.withArguments(ListUtils.map(annotation.getArguments(), arg -> {
                    if (arg instanceof J.Assignment) {
                        J.Assignment assignment = (J.Assignment) arg;
                        J.Identifier variable = (J.Identifier) assignment.getVariable();
                        return assignment.withVariable(variable.withSimpleName("testing"));
                    }
                    return arg;
                }));
            }
        }));
    }

    @Test
    void testForAnnotationMatcher() {
        rewriteRun(
          kotlin(
            """
              import org.openrewrite.kotlin.annotation.AnnotationMatcherTest.AnnotationTest
              @AnnotationTest(defaultValue = "123")
              class A {}
              """,
            """
                import org.openrewrite.kotlin.annotation.AnnotationMatcherTest.AnnotationTest
                @AnnotationTest(testing = "123")
                class A {}
                """
          )
        );
    }
}
