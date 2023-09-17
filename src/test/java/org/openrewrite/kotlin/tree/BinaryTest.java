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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"KotlinConstantConditions", "ControlFlowWithEmptyBody"})
class BinaryTest implements RewriteTest {

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void equals() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val n = 0
                val b = n == 0
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void notEquals() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val n = 0
                val b = n != 0
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void greaterThan() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val n = 0
                val b = n > 0
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void greaterThanOrEqual() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val n = 0
                val b = n >= 0
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void lessThan() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val n = 0
                val b = n < 0
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void lessThanOrEqual() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val n = 0
                val b = n <= 0
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void endOfLineBreaks() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  val b : Boolean = 1 == 1 // c1
                              && 2 == 2 // c2
                              && 3 == 3
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void bitwiseAnd() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val a = 0
                val b = a and 1
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void bitwiseOr() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val a = 0
                val b = a or 1
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void bitwiseXOr() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val a = 0
                val b = a xor 1
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void inversion() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val a = 0
                val b = a . inv ( )
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void shiftLeft() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val a = 0
                val b = a shl 1
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void shiftRight() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val a = 0
                val b = a shr 1
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void unsignedShiftRight() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val a = 0
                val b = a ushr 1
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void identityOperation() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val a = 0 === 0
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void notIdentityOperation() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val a = 0 !== 0
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/8")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void parenthesized() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val n = 0
                val b = n == ( 1 - 1 )
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void doubleLogicParenthesized() {
        rewriteRun(
          kotlin(
            """
              val b : Boolean = true
              val x = ((b || b) && (b || b))
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void rem() {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                val n = 0
                val b = n % 2 == 0
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @ParameterizedTest
    @ValueSource(strings = {
      "1 == 1 == true",
      "1==1==true"
    })
    void multipleEquals(String arg) {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                  if ( %s ) {
                  }
              }
              """.formatted(arg)
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void notIn() {
        rewriteRun(
          kotlin(
            """
              val x = "x" !in arrayOf("x")
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/149")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void explicitReceiver() {
        rewriteRun(
          kotlin(
            """
              data class Foo ( val aliases : List < String > ) {
                  fun names ( ) = listOf ( "canonicalName" ) + aliases
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/139")
    @Disabled("FIXME, to be supported by PSI parser")
    @ParameterizedTest
    @ValueSource(strings = {
      "-- n",
      "n --"
    })
    void unaryOp(String op) {
        rewriteRun(
          kotlin(
            """
              fun method ( ) {
                var n = 0
                val a = %s == -1
                val b = -1 == %s
              }
              """.formatted(op, op)
          )
        );
    }
}
