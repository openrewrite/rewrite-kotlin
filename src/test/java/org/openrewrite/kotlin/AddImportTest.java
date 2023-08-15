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
              import java.lang.Integer
              import java.lang.Long

              class A
              """,
            """
              import java.lang.Integer
              import java.lang.Integer.MAX_VALUE
              import java.lang.Long
              
              class A
              """
          )
        );
    }

    @Test
    void starFoldPackageTypes() {
        rewriteRun(
          spec -> spec.recipe(importTypeRecipe("java.io.OutputStream")),
          kotlin(
            """
              import java.io.Closeable
              import java.io.File
              import java.io.FileInputStream
              import java.io.FileOutputStream

              class A
              """,
            """
              import java.io.*
              
              class A
              """
          )
        );
    }

    @Test
    void noStarFoldTypeMembers() {
        rewriteRun(
          spec -> spec.recipe(importMemberRecipe("java.util.regex.Pattern", "MULTILINE")),
          kotlin(
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE

              class A
              """,
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE
              import java.util.regex.Pattern.MULTILINE

              class A
              """
          )
        );
    }

    @Test
    void starFoldTypeMembers() {
        rewriteRun(
          spec -> spec.recipe(importMemberRecipe("java.util.regex.Pattern", "MULTILINE")),
          kotlin(
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE
              import java.util.regex.Pattern.COMMENTS

              class A
              """,
            """
              import java.util.regex.Pattern.*
              
              class A
              """
          )
        );
    }

    @Test
    void importAlias() {
        rewriteRun(
          spec -> spec.recipe(importMemberRecipe("java.util.regex.Pattern", "MULTILINE")),
          kotlin(
            """
              import java.util.regex.Pattern.CASE_INSENSITIVE as i
              import java.util.regex.Pattern.COMMENTS as x

              class A
              """,
            """
              import java.util.regex.Pattern.MULTILINE

              import java.util.regex.Pattern.CASE_INSENSITIVE as i
              import java.util.regex.Pattern.COMMENTS as x
              
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
