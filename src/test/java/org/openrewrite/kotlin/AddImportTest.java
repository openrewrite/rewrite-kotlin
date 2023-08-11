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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class AddImportTest implements RewriteTest {

    @Test
    void normalClass() {
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("a.b.Target")),
          kotlin(
            """
              package a.b
              class Original
              """),
          kotlin(
            """
              package a.b
              class Target
              """),
          kotlin(
            """
              import a.b.Original
              
              class A {
                  val type : Original = Original()
              }
              """,
            """
              import a.b.Original
              import a.b.Target
              
              class A {
                  val type : Original = Original()
              }
              """
          )
        );
    }

    @Test
    void jvmStaticMember() {
        rewriteRun(
          spec -> spec.recipe(importMemberRecipe("java.lang.Integer", "MAX_VALUE")),
          kotlin(
            """
              import kotlin.Pair

              class A
              """,
            """
              import kotlin.Pair

              import java.lang.Integer.MAX_VALUE
              
              class A
              """
          )
        );
    }

    @Test
    void packageLevelFunction() {
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("a.b.method")),
          kotlin(
            """
              package a.b
              class Original
              """
          ),
          kotlin(
            """
              package a.b
              fun method() {}
              """
          ),
          kotlin(
            """
              import a.b.Original
              
              class A {
                  val type : Original = Original()
              }
              """,
            """
              import a.b.Original
              import a.b.method
              
              class A {
                  val type : Original = Original()
              }
              """
          )
        );
    }

    @Test
    void noImportOfImplicitTypes() {
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("kotlin.Pair")),
          kotlin(
            """
              class A
              """
          )
        );
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("java.lang.Integer")),
          kotlin(
            """
              class A
              """
          )
        );
    }

    static Recipe importTypeRecipe(String type) {
        return toRecipe(() -> new KotlinIsoVisitor<>() {
            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                maybeAddImport(type, null, false);
                return cu;
            }
        });
    }

    static Recipe importMemberRecipe(String type, String member) {
        return toRecipe(() -> new KotlinIsoVisitor<>() {
            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                maybeAddImport(type, member, false);
                return cu;
            }
        });
    }
}
