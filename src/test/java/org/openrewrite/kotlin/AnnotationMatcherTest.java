package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

public class AnnotationMatcherTest implements RewriteTest {

    @Test
    void matchAnnotation() {
        rewriteRun(
          kotlin(
            """
              @Deprecated("")
              class A
              """, spec -> spec.afterRecipe(cu -> {
                  AtomicBoolean found = new AtomicBoolean(false);
                AnnotationMatcher matcher = new AnnotationMatcher("@kotlin.Deprecated");
                new KotlinIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, AtomicBoolean atomicBoolean) {
                        if (matcher.matches(annotation)) {
                            found.set(true);
                        }
                        return super.visitAnnotation(annotation, atomicBoolean);
                    }
                }.visit(cu, found);
                assertThat(found.get()).isTrue();
            })
          )
        );
    }
}
