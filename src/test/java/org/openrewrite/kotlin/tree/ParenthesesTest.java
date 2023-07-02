package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class ParenthesesTest implements RewriteTest {


    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/138")
    @Test
    void inParens() {
        rewriteRun(
          kotlin(
            """
              fun method(a: Any) {
                  val any = (if (a is Boolean) "true" else "false")
              }
              """
          )
        );
    }

    @Test
    void nestedParentheses() {
        rewriteRun(
          kotlin("""
            fun method(a: Any?) {
                ((((if (((a)) == ((null))) return))))
                val r = a
            }
            """
          )
        );
    }
}
