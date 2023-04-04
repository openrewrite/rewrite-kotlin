package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

public class MethodMatcherTest implements RewriteTest {

    @Test
    void matchesTopLevelFunction() {
        rewriteRun(
          kotlin(
            """
              class File
              
              fun function() {}
              """,
            spec -> spec.afterRecipe(cu -> assertThat(FindMethods.find(cu, "FileKt function()")).isNotEmpty())
          )
        );
    }
}
