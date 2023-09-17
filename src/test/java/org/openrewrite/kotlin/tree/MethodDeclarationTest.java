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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"UnusedReceiverParameter", "RedundantSuspendModifier"})
class MethodDeclarationTest implements RewriteTest {

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void methodDeclaration() {
        rewriteRun(
          kotlin("fun method ( ) { }")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void parameters() {
        rewriteRun(
          kotlin("fun method ( i : Int ) { }")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void functionTypeReference() {
        rewriteRun(
          kotlin("fun method( input : (  ) -> String ) { }")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void typedFunctionTypeReference() {
        rewriteRun(
          kotlin("fun method( input : ( Int , Int ) -> Boolean ) { }")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void functionTypeWithReceiver() {
        rewriteRun(
          kotlin("fun method ( arg : String . ( ) -> String ) { }")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void assignment() {
        rewriteRun(
          kotlin("fun method ( ) : Boolean = true")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void constructor() {
        rewriteRun(
          kotlin(
            """
              class A(i : Int) {
                  constructor() : this  (1)
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void quotedIdentifier() {
        rewriteRun(
          kotlin("fun `some quoted id` ( ) { }")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void defaults() {
        rewriteRun(
          kotlin("fun apply ( plugin : String ? = null ) { }")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void reifiedGeneric() {
        rewriteRun(
          kotlin("inline fun < reified T > method ( value : T ) { }")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void genericTypeParameters() {
        rewriteRun(
          kotlin("fun < T : Number > method ( type : T ) { }")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void receiverType() {
        rewriteRun(
          kotlin("class Test"),
          kotlin("fun Test . method ( ) { }")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
    @ParameterizedTest
    @ValueSource(strings = {
      "out Number",
      "in String"
    })
    void variance(String param) {
        rewriteRun(
          kotlin("interface PT < T >"),
          kotlin("fun generic ( n : PT < %s > ) { }".formatted(param))
        );
    }

    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/205")
    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/271")
    void negativeSingleExpression() {
        rewriteRun(
          kotlin(
            """
              fun size(): Int = -1
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void parenthesizedSingleExpression() {
        rewriteRun(
          kotlin(
            """
              fun size(): Int = (-1)
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void multiplatformExpectDeclaration() {
        rewriteRun(
          kotlin(
            """
              expect suspend fun Any.executeAsync(): Any
              """
          )
        );
    }
}
