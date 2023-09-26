package org.openrewrite.kotlin;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.internal.DesugarVisitor;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("All")
class DesugarVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(DesugarVisitor::new));
    }

    @Test
    void rangeTo() {
        rewriteRun(
          kotlin(
            "val a = 1 .. 10",
            spec -> spec.after(a -> a).afterRecipe(cu -> {
                new KotlinIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer o) {
                        if ("rangeTo".equals(method.getSimpleName())) {
                            assertThat(method.getMethodType().toString()).isEqualTo("kotlin.Int{name=rangeTo,return=kotlin.ranges.IntRange,parameters=[kotlin.Int]}");
                        }
                        return super.visitMethodInvocation(method, o);
                    }
                }.visit(cu, 0);
            })
          )
        );
    }

    @Disabled
    @Test
    void desugarInt() {
        rewriteRun(
          kotlin(
            """
              val a = 2 !in 1 .. 10
              """,
            spec -> spec.afterRecipe(
              cu -> {
                  new KotlinVisitor<ExecutionContext>() {
                      @Override
                      public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                          if (method.getSimpleName().equals("rangeTo")) {
                              JavaType.Method type1 = method.getMethodType();
                              assertThat(type1.toString()).isEqualTo("kotlin.Int{name=rangeTo,return=kotlin.ranges.IntRange,parameters=[kotlin.Int]}");
                          } else if (method.getSimpleName().equals("contains")) {
                              JavaType.Method type2 = method.getMethodType();
                              assertThat(type2.toString()).isEqualTo("kotlin.ranges.IntRange{name=contains,return=kotlin.Boolean,parameters=[kotlin.Int]}");
                          } else if (method.getSimpleName().equals("not")) {
                              JavaType.Method type3 = method.getMethodType();
                              assertThat(type3.toString()).isEqualTo("kotlin.Boolean{name=not,return=kotlin.Boolean,parameters=[]}");
                          }
                          return super.visitMethodInvocation(method, ctx);
                      }
                  }.visit(cu, new InMemoryExecutionContext());
              }
            )
          )
        );
    }

    @Disabled
    @Test
    void desugarDouble() {
        rewriteRun(
          kotlin(
            """
              val a = 0.2 !in 0.1 .. 0.9
              """,
            spec -> spec.afterRecipe(
              cu -> {
                  new KotlinVisitor<ExecutionContext>() {
                      @Override
                      public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                          if (method.getSimpleName().equals("rangeTo")) {
                              JavaType.Method type1 = method.getMethodType();
                              assertThat(type1.toString()).isEqualTo("kotlin.ranges.RangesKt{name=rangeTo,return=kotlin.ranges.ClosedFloatingPointRange<kotlin.Double>,parameters=[kotlin.Double,kotlin.Double]}");
                          } else if (method.getSimpleName().equals("contains")) {
                              JavaType.Method type2 = method.getMethodType();
                              assertThat(type2.toString()).isEqualTo("kotlin.ranges.ClosedFloatingPointRange<Generic{Tkotlin.Comparable<Generic{T}>}>{name=contains,return=kotlin.Boolean,parameters=[kotlin.Double]}");
                          } else if (method.getSimpleName().equals("not")) {
                              JavaType.Method type3 = method.getMethodType();
                              assertThat(type3.toString()).isEqualTo("kotlin.Boolean{name=not,return=kotlin.Boolean,parameters=[]}");
                          }
                          return super.visitMethodInvocation(method, ctx);
                      }
                  }.visit(cu, new InMemoryExecutionContext());
              }
            )
          )
        );
    }

    @Disabled
    @Test
    void yikes() {
        rewriteRun(
          kotlin("""
              val b = !((1.plus(2)+2) !in 1 ..  3).not( )
              """
            ,
            """
              val b = !( 1.rangeTo(3).contains((1.plus(2)+2)).not()).not( )
              """
          )
        );
    }
}
