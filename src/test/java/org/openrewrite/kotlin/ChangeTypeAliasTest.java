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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class ChangeTypeAliasTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeTypeAlias("TestAlias", "NewAlias", "Test"));
    }

    @Test
    void typeAlias() {
        rewriteRun(
          kotlin(
            """
            class Test
            """
          ),
          kotlin(
            """
            typealias TestAlias = Test
            val a : TestAlias = Test ( )
            """,
            """
            typealias NewAlias = Test
            val a : NewAlias = Test ( )
            """
          )
        );
    }

    @Test
    void fieldAccessTypeAlias() {
        rewriteRun(
          kotlin(
            """
            class Test<T>
            """
          ),
          kotlin(
            """
            typealias TestAlias<T> = Test<T>
            val a: TestAlias<String> = Test<String>()
            """,
            """
            typealias NewAlias<T> = Test<T>
            val a: NewAlias<String> = Test<String>()
            """
          )
        );
    }
}
