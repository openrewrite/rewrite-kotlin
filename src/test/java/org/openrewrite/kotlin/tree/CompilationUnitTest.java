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
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.kotlin.Assertions.kotlin;

class CompilationUnitTest implements RewriteTest {

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void emptyFile() {
        rewriteRun(
          kotlin("")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void packageDecl() {
        rewriteRun(
          kotlin("package kotlin")
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void imports() {
        rewriteRun(
          kotlin(
            """
              import java.util.List
              import java.io.*
              class A
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void packageAndComments() {
        rewriteRun(
          kotlin(
            """
              /* Comment */
              package a
              import java.util.List
              
              class A
              // comment
              """,
            SourceSpec::noTrim
          )
        );
    }
}
