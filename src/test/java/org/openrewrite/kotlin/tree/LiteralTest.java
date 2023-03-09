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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

class LiteralTest implements RewriteTest {

    @Test
    void intentionallyBadUnicodeCharacter() {
        rewriteRun(
          kotlin(
            """
            val s1 = "\\\\u{U1}"
            val s2 = "\\\\u1234"
            val s3 = "\\\\u{00AUF}"
            """
          )
        );
    }

    @Test
    void literalField() {
        rewriteRun(
          kotlin("val n : Int = 0 ")
        );
    }

    @Test
    void literalCharacter() {
        rewriteRun(
          kotlin("val c : Character = 'c' ")
        );
    }

    @Test
    void literalNumerics() {
        rewriteRun(
          kotlin(
            """
            val d : Double = 1.0
            val f : Float = 1.0F
            val l1 : Long = 1
            val l2 : Long = 1L
            """
          )
        );
    }

    @Test
    void literalBinary() {
        rewriteRun(
          kotlin(
            """
            val l : Long = 0b10L
            val b : Byte = 0b10
            val s : Short = 0b10
            val i : Int = 0b10
            """
          )
        );
    }

    @Test
    void literalHex() {
        rewriteRun(
          kotlin(
            """
            val l : Long = 0xA0L
            val s : Short = 0xA0
            val i : Int = 0xA0
            """
          )
        );
    }

    @Test
    void unmatchedSurrogatePair() {
        rewriteRun(
          kotlin(
            """
            val c1 : Character = '\uD800'
            val c2 : Character = '\uDfFf'
            """
          )
        );
    }

    @Test
    void unmatchedSurrogatePairInString() {
        rewriteRun(
          kotlin(
            """
            val s1 : String = "\uD800"
            val s2 : String = "\uDfFf"
            """
          )
        );
    }
}
