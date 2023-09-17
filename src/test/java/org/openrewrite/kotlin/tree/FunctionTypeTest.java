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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class FunctionTypeTest implements RewriteTest {
    @Test
    void nested() {
        rewriteRun(
          kotlin(
            """
              val f: ((Int) -> Boolean) -> Boolean = { true }
              """
          )
        );
    }
    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/310")
    void generic() {
        rewriteRun(
          kotlin(
            """
              val f: Function<() -> Boolean> = { { true } }
              """
          )
        );
    }
    @Test
    void namedParameter() {
        rewriteRun(
          kotlin(
            """
              val f: (p  : Any) -> Boolean = { true }
              """
          )
        );
    }
    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/275")
    void parenthesizedNullableType() {
        rewriteRun(
          kotlin(
            """
              val v: ((Any) -> Any)? = null
              """
          )
        );
    }
    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/292")
    @ExpectedToFail
    void functionTypeParentheses() {
        rewriteRun(
          kotlin(
            """
              fun readMetadata(lookup: ((Class<Metadata>) -> Metadata?)): Metadata {
                  return null!!
              }
              """
          )
        );
    }
}
