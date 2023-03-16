package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.AddImport;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class AddImportTest  implements RewriteTest {
    @Test
    void addImportBeforeImportWithSameInsertIndex() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new AddImport<>("org.junit.jupiter.api.Assertions", "assertFalse", false))),
          kotlin(
            """
              import org.junit.jupiter.api.Assertions.assertTrue
              import org.junit.Test

              class MyTest
              """,
            """
              import org.junit.jupiter.api.Assertions.assertFalse
              import org.junit.jupiter.api.Assertions.assertTrue
              import org.junit.Test

              class MyTest
              """
          )
        );
    }
}
