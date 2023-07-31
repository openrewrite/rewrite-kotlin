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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"UnusedReceiverParameter", "RedundantSuspendModifier"})
class MethodDeclarationTest implements RewriteTest {

    @Test
    void methodDeclaration() {
        rewriteRun(
          kotlin("fun method ( ) { }")
        );
    }

    @Test
    void parameters() {
        rewriteRun(
          kotlin("fun method ( i : Int ) { }")
        );
    }

    @Test
    void functionTypeReference() {
        rewriteRun(
          kotlin("fun method( input : (  ) -> String ) { }")
        );
    }

    @Test
    void typedFunctionTypeReference() {
        rewriteRun(
          kotlin("fun method( input : ( Int , Int ) -> Boolean ) { }")
        );
    }

    @Test
    void functionTypeWithReceiver() {
        rewriteRun(
          kotlin("fun method ( arg : String . ( ) -> String ) { }")
        );
    }

    @Test
    void assignment() {
        rewriteRun(
          kotlin("fun method ( ) : Boolean = true")
        );
    }

    @Test
    void returnType() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) : Boolean {
                  return true
              }
              """
          )
        );
    }

    @Test
    void methodDeclarationDeclaringType() {
        rewriteRun(
          kotlin(
            """
              class A {
                  fun method ( ) {
                  }
              }
              """
          )
        );
    }

    @Test
    void infix() {
        rewriteRun(
          kotlin(
            """
              class Spec {
                  fun version ( version : String) : Spec {
                      return this
                  }
              }
              """
          ),
          kotlin(
            """
              class A {
                fun method ( ) {
                }
              }
              """
          ),
          kotlin("infix fun Spec . version ( version : String ) : Spec = version ( version )")
        );
    }

    @Test
    void quotedIdentifier() {
        rewriteRun(
          kotlin("fun `some quoted id` ( ) { }")
        );
    }

    @Test
    void defaults() {
        rewriteRun(
          kotlin("fun apply ( plugin : String ? = null ) { }")
        );
    }

    @Test
    void reifiedGeneric() {
        rewriteRun(
          kotlin("inline fun < reified T > method ( value : T ) { }")
        );
    }

    @Test
    void genericTypeParameters() {
        rewriteRun(
          kotlin("fun < T : Number > method ( type : T ) { }")
        );
    }

    @Test
    void receiverType() {
        rewriteRun(
          kotlin("class Test"),
          kotlin("fun Test . method ( ) { }")
        );
    }

    @Test
    void methodInvocationOnReceiverType() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun build ( s : ( ) -> String ) {
                  }
              }
              """
          ),
          kotlin(
            """
              fun Test . method ( ) = build {
                  "42"
              }
              """
          )
        );
    }

    @Test
    void nullableReturnType() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) : Array < Int > ? {
              }
              """
          )
        );
    }

    @Test
    void typeParameterAndTypeReceiver() {
        rewriteRun(
          kotlin(
            """
              fun < T : Any > Array < Int > . method ( t : T ) = Unit
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/56")
    @Test
    void lambdaMethodParameterWithModifier() {
        rewriteRun(
          kotlin(
            """
              suspend fun example (
                title : String ,
                verifyUnique : suspend ( String ) -> Boolean
              ) : String = TODO ( )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/70")
    @Test
    void crossinline() {
        rewriteRun(
          kotlin(
            """
              inline fun example (
                crossinline block : ( ) -> Unit
              ) : Unit = Unit
              """)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/70")
    @Test
    void noinline() {
        rewriteRun(
          kotlin(
            """
              inline fun example (
                noinline block : ( ) -> Unit
              ) : Unit = Unit
              """)
        );
    }

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/205")
    @Test
    void genericTypeConstraint() {
        rewriteRun(
          kotlin(
            """
              fun <T> foo(): Int where T: List<T> {
                  return 0
              }
              """
          )
        );
    }

    @Test
    void hasFinalModifier() {
        rewriteRun(
          kotlin(
            "fun method() {}",
            spec -> spec.afterRecipe(cu -> {
                for (Statement statement : cu.getStatements()) {
                    if (statement instanceof J.MethodDeclaration) {
                        J.Modifier.hasModifier(((J.MethodDeclaration) statement).getModifiers(), J.Modifier.Type.Final);
                        assertThat(J.Modifier.hasModifier(((J.MethodDeclaration) statement).getModifiers(), J.Modifier.Type.Final)).isTrue();
                    }
                }
            }))
        );
    }
}
