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
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

class FieldAccessTest implements RewriteTest {

    @Test
    void thisAccess() {
        rewriteRun(
          kotlin(
            """
            class Test {
                var id : String = ""
                fun setId ( id : String ) {
                    this . id = id
                }
            }
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/18")
    @Test
    void superAccess() {
        rewriteRun(
          kotlin(
            """
            open class Super {
                val id : String = ""
            }
            """
          ),
          kotlin(
            """
            class Test : Super() {
                fun getId ( ) : String {
                    return super . id
                }
            }
            """
          )
        );
    }

    @Test
    void nullSafeDereference() {
        rewriteRun(
          kotlin(
            """
            class Test {
                val property = 42
            }
            """
          ),
          kotlin(
            """
            fun method ( test : Test ? ) {
                val a = test ?. property
            }
            """
          )
        );
    }

    @Test
    void elvisOperator() {
        rewriteRun(
          kotlin(
            """
            class Test {
                val value : Int ? = 42
            }
            """
          ),
          kotlin(
            """
            fun method ( test : Test ) {
                val a = test . value ?: null
            }
            """
          )
        );
    }

    @Test
    void qualifier() {
        rewriteRun(
          kotlin(
            """
            import java.nio.ByteBuffer
            
            private val crlf: ByteBuffer = ByteBuffer.wrap("\\r\\n".toByteArray())
            """
          )
        );
    }
}
