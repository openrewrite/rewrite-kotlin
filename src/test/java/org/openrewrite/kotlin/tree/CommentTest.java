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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class CommentTest implements RewriteTest {

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void backToBackMultilineComments() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  /*
                   * C1
                   */
                  /*
                   * C2
                   */
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void multilineNestedInsideSingleLine() {
        rewriteRun(
          kotlin(
            """
              class Test { // /*
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void leadingComments() {
        rewriteRun(
          kotlin(
            """
              // C1
              
              // C2
              open class Test {
                  // C3
                  
                  // C4
                  internal val n = 0

                  // C5
                  
                  // C6
                  internal fun method() {
                  }
              }
              """
          )
        );
    }
}
