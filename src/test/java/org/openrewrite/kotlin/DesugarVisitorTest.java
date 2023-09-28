package org.openrewrite.kotlin;

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
    void desugarInt() {
        rewriteRun(
          kotlin(
            """
              val a = 2 !in 1 .. 10
              """,
            """
              val a = 1.rangeTo(10).contains(2).not()
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

    @Test
    void desugarDouble() {
        rewriteRun(
          kotlin(
            """
              val a = 0.2 !in 0.1 .. 0.9
              """,
            """
              val a = 0.1.rangeTo(0.9).contains(0.2).not()
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
