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
package org.openrewrite.kotlin.format;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.kotlin.style.SpacesStyle;
import org.openrewrite.kotlin.style.WrappingAndBracesStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("All")
class WrappingAndBracesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new WrappingAndBracesVisitor<>(IntelliJ.wrappingAndBraces())));
    }

    private static Consumer<RecipeSpec> wrappingAndBraces(UnaryOperator<SpacesStyle> spaces,
                                                          UnaryOperator<WrappingAndBracesStyle> wrapping) {
        return spec -> spec
          .recipes(
            toRecipe(() -> new WrappingAndBracesVisitor<>(wrapping.apply(IntelliJ.wrappingAndBraces()))),
            toRecipe(() -> new TabsAndIndentsVisitor<>(IntelliJ.tabsAndIndents())),
            toRecipe(() -> new SpacesVisitor<>(spaces.apply(IntelliJ.spaces())))
          )
          .parser(KotlinParser.builder().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              Arrays.asList(
                spaces.apply(IntelliJ.spaces()),
                wrapping.apply(IntelliJ.wrappingAndBraces())
              )
            )
          )));
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void classConstructor() {
        rewriteRun(
          kotlin(
            """
              class A   (
                      val type: Int = 1
              ) {
                   var a = 2
              }
              """
          )
        );
    }

    @SuppressWarnings({"ClassInitializerMayBeStatic", "ReassignedVariable", "UnusedAssignment"})
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void blockLevelStatements() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  init {        var n: Int = 0
                      n++
                  }
              }
              """,
            """
              class Test {
                  init {
                      var n: Int = 0
                      n++
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void blockEndOnOwnLine() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  val n: Int = 0}
              """,
            """
              class Test {
                  val n: Int = 0
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedMethod() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress("ALL")   fun method(): Any {
                      return Any()
                  }
              }
              """,
            """
              class Test {
                  @Suppress("ALL")
                 fun method(): Any {
                      return Any()
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void leadingAnnotationNewLine() {
        rewriteRun(
          kotlin(
            """
              class Test {   @Suppress("ALL")   fun method(): Any {
                      return Any()
                  }
              }
              """,
            """
              class Test {
                 @Suppress("ALL")
                 fun method(): Any {
                      return Any()
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedMethodWithPublicModifier() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress("ALL")   public fun method(): Any {
                      return Any()
                  }
              }
              """,
            """
              class Test {
                  @Suppress("ALL")
                 public fun method(): Any {
                      return Any()
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedMethodWithFinalModifier() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress("ALL")   final fun method(): Any {
                      return Any()
                  }
              }
              """,
            """
              class Test {
                  @Suppress("ALL")
                 final fun method(): Any {
                      return Any()
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedMethodWithModifiers() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress("ALL")   public final fun method(): Any {
                      return Any()
                  }
              }
              """,
            """
              class Test {
                  @Suppress("ALL")
                 public final fun method(): Any {
                      return Any()
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedMethodWithTypeParameter() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress("ALL")   fun <T> method(): T? {
                      return null
                  }
              }
              """,
            """
              class Test {
                  @Suppress("ALL")
                 fun <T> method(): T? {
                      return null
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void multipleAnnotatedMethod() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress("ALL") @Deprecated("", ReplaceWith("Any()"))   fun method(): Any {
                      return Any()
                  }
              }
              """,
            """
              class Test {
                  @Suppress("ALL") 
               @Deprecated("", ReplaceWith("Any()"))
                 fun method(): Any {
                      return Any()
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedConstructor() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress("ALL") @Deprecated("",ReplaceWith("Any()")) constructor() {
                  }
              }
              """,
            """
              class Test {
                  @Suppress("ALL")
               @Deprecated("",ReplaceWith("Any()"))
               constructor() {
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedClassDecl() {
        rewriteRun(
          kotlin(
            """
              @Suppress("ALL")   class Test {
              }
              """,
            """
              @Suppress("ALL")
                 class Test {
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedClassDeclMultiAnnotations() {
        rewriteRun(
          kotlin(
            """
              @Suppress("ALL")   @Deprecated("",ReplaceWith("Any()")) class Test {
              }
              """,
            """
              @Suppress("ALL")
                 @Deprecated("",ReplaceWith("Any()"))
               class Test {
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedClassDeclAlreadyCorrect() {
        rewriteRun(
          kotlin(
            """
              @Suppress("ALL")
              class Test {
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedClassDeclWithModifiers() {
        rewriteRun(
          kotlin(
            """
              @Suppress("ALL") public class Test {
              }
              """,
            """
              @Suppress("ALL")
               public class Test {
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedVariableDecl() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  public fun doSomething() {
                      @Suppress("ALL") var foo: Int
                  }
              }
              """,
            """
              class Test {
                  public fun doSomething() {
                      @Suppress("ALL")
               var foo: Int
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedVariableDeclWithModifier() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress("ALL")   private var foo: Int = 0
              }
              """,
            """
              class Test {
                  @Suppress("ALL")
                 private var foo: Int = 0
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void annotatedVariableDeclInMethodDeclaration() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  public fun doSomething(@Suppress("ALL") foo: Int) {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2469")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void elseOnNewLine() {
        rewriteRun(
          wrappingAndBraces(
            spaces -> spaces,
            wrap -> wrap.withIfStatement(IntelliJ.wrappingAndBraces().getIfStatement().withElseOnNewLine(true))),
          kotlin(
            """
              class Test {
                  fun method(arg0: Int) {
                      if (arg0 == 0) {
                          System.out.println("if");
                      } else if (arg0 == 1) {
                          System.out.println("else if");
                      } else {
                          System.out.println("else");
                      }
                  }
              }
              """,
            """
              class Test {
                  fun method(arg0: Int) {
                      if (arg0 == 0) {
                          System.out.println("if");
                      }
                      else if (arg0 == 1) {
                          System.out.println("else if");
                      }
                      else {
                          System.out.println("else");
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2469")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void elseNotOnNewLine() {
        rewriteRun(
          wrappingAndBraces(
            spaces -> spaces,
            wrap -> wrap.withIfStatement(IntelliJ.wrappingAndBraces().getIfStatement().withElseOnNewLine(false))),
          kotlin(
            """
              class Test {
                  fun method(arg0: Int) {
                      if (arg0 == 0) {
                          System.out.println("if");
                      }
                      else if (arg0 == 1) {
                          System.out.println("else if");
                      }
                      else {
                          System.out.println("else");
                      }
                  }
              }
              """,
            """
              class Test {
                  fun method(arg0: Int) {
                      if (arg0 == 0) {
                          System.out.println("if");
                      } else if (arg0 == 1) {
                          System.out.println("else if");
                      } else {
                          System.out.println("else");
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3191")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void emptyLineBeforeEnumConstants() {
        rewriteRun(
          kotlin(
            """
              enum class Status {
                  NOT_STARTED,
                  STARTED
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void singleStatementFunctionNoNewLines() {
        rewriteRun(
          kotlin(
            """
              class A {
                  fun name(): String =
                          "123"
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void nonSingleStatementFunctionNeedNewLines() {
        // An equivalent code with above test singleStatementFunctionNoNewLines, but not a single statement function
        rewriteRun(
          kotlin(
            """
              class A {
                  fun name(): String {   return "123" }
              }
              """,
            """
              class A {
                  fun name(): String {
                 return "123"
               }
              }
              """
          )
        );
    }


}
