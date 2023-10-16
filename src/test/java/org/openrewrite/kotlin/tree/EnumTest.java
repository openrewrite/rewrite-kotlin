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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class EnumTest implements RewriteTest {

    @Test
    void enumEmptyBody() {
        rewriteRun(
          kotlin("enum class A")
        );
    }

    @Test
    void enumDefinition() {
        rewriteRun(
          kotlin(
            """
              enum class A {
                  B  ,   C    ,
                  D
              }
              """
          )
        );
    }

    @SuppressWarnings("RedundantEnumConstructorInvocation")
    @Test
    void enumWithInit() {
        rewriteRun(
          kotlin(
            """
              enum class A {
                  B , C() ,
                  D
              }
              """
          ),
          kotlin(
            """
              enum class EnumTypeB(val label: String) {
                  FOO("foo")
              }
              """
          )
        );
    }

    @Test
    void innerEnum() {
        rewriteRun(
          kotlin(
            """
              class A {
                  enum class B {
                      C
                  }
              }
              """
          )
        );
    }

    @Test
    void semiColon() {
        rewriteRun(
          kotlin(
            """
              enum class A {
                  B , C ,
                  D ;
              }
              """
          )
        );
    }

    @Test
    void trailingComma() {
        rewriteRun(
          kotlin(
            """
              enum class A {
                  B , C ,
                  D , // trailing comma
              }
              """
          )
        );
    }

    @Test
    void trailingCommaTerminatingSemicolon() {
        rewriteRun(
          kotlin(
            """
              enum class A {
                  B , C ,
                  D , /* trailing comma */ ; /*terminating semicolon*/
              }
              """
          )
        );
    }

    @Test
    void enumImplementingInterface() {
        rewriteRun(
          kotlin(
            """
              enum class Test : java.io.Serializable {
                  FOO {
                      fun foo() = print("foo",)
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/307")
    void enumWithFunction() {
        rewriteRun(
          kotlin(
            """
              private enum class TargetLanguage {
                  JAVA,
                  KOTLIN;
              
                  fun expectedFile(): String = "foo"
              }
              """
          )
        );
    }
}
