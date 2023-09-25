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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class TypeMappingTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/199")
    @Test
    void parameterizedTypeMapping() {
        rewriteRun(
          kotlin(
            """
              import org.openrewrite.Maintainer
              import org.openrewrite.test.AdHocRecipe

              fun method(): List<Maintainer> {
                  val adHocRecipe = AdHocRecipe(null, null, null, null, null, null, null)
                  return adHocRecipe.maintainers
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(
      value = {
        "unaryPlus:+",
        "unaryMinus:-",
        "not:!",
      }, delimiter = ':'
    )
    void unaryTypeOverloads(String name, String op) {
        rewriteRun(
          kotlin(
            """
              interface A {
                operator fun %s() { /* Do something */ }
              }
              class B : A {
                override fun %s() { /* Do something else */ }
              }
              val checkType = %s B()
              """.formatted(name, name, op), spec -> spec.afterRecipe(cu -> {
                // Add type check
            })
          )
        );
    }

    @ParameterizedTest
    @CsvSource(
      value = {
        "inc:++",
        "dec:--",
      }, delimiter = ':'
    )
    void postFixTypeOverloads(String name, String op) {
        rewriteRun(
          kotlin(
            """
              interface A {
                operator fun %s() { /* Do something */ }
              }
              class B : A {
                override fun %s() { /* Do something else */ }
              }
              val checkType = B()%s
              """.formatted(name, name, op), spec -> spec.afterRecipe(cu -> {
                // Add type check
            })
          )
        );
    }

    @ParameterizedTest
    @CsvSource(
      value = {
        "plus:+",
        "minus:-",
        "times:*",
        "div:/",
        "rem:%",
        "rangeTo:..",
        "rangeUntil:..<",
      }, delimiter = ':'
    )
    void binaryTypeOverloads(String name, String op) {
        rewriteRun(
          kotlin(
            """
              interface A {
                operator fun %s(other: Int) { /* Do something */ }
              }
              class B : A {
                override fun %s(other: Int) { /* Do something else */ }
              }
              val checkType = B() %s 1
              """.formatted(name, name, op), spec -> spec.afterRecipe(cu -> {
                // Add type check
            })
          )
        );
    }

    @Test
    void inOperators() {
        rewriteRun(
          kotlin(
            """
              interface A {
                operator fun contains(other: Int): Boolean {
                    return false
                }
                operator fun not(): Boolean {
                    return false
                }
              }
              class B : A {
                override fun contains(other: Int): Boolean {
                    return true
                }
                override fun not(): Boolean {
                    return true
                }
              }
              val checkTypeA = 1 in B()
              val checkTypeB = 1 !in B()
              """, spec -> spec.afterRecipe(cu -> {
                // Add type check
            })
          )
        );
    }

    @Test
    void invokeOperators() {
        rewriteRun(
          kotlin(
            """
              interface A {
                operator fun invoke() {}
                operator fun invoke(other: Int) {}
              }
              class B : A {
                override fun invoke() {}
                override fun invoke(other: Int) {}
              }
              val b = B()
              val checkTypeA = b()
              val checkTypeB = b(1)
              """, spec -> spec.afterRecipe(cu -> {
                // Add type check
            })
          )
        );
    }

    @ParameterizedTest
    @CsvSource(
      value = {
        "equals:+=",
        "minusAssign:-=",
        "timesAssign:*=",
        "divAssign:/=",
        "remAssign:%=",
      }, delimiter = ':'
    )
    void augmentedAssignments(String name, String op) {
        rewriteRun(
          kotlin(
            """
              interface A {
                operator fun %s(other: Int) {}
              }
              class B : A {
                override fun %s(other: Int) {}
              }
              val checkType = B() %s 1
              """.formatted(name, name, op), spec -> spec.afterRecipe(cu -> {
                // Add type check
            })
          )
        );
    }

    @ParameterizedTest
    @ValueSource(
      strings = {
        ">",
        "<",
        ">=",
        "<=",
      }
    )
    void comparisonOps(String op) {
        rewriteRun(
          kotlin(
            """
              interface A {
                operator fun compareTo(other: Int): Int {
                    return 24
                }
              }
              class B : A {
                override fun compareTo(other: Int) {
                    return 42
                }
              }
              val checkType = B() %s 1
              """.formatted(op), spec -> spec.afterRecipe(cu -> {
                  // Add type check
            })
          )
        );
    }
}
