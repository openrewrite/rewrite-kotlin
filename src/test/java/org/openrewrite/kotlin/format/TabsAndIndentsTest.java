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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.kotlin.style.TabsAndIndentsStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;


@SuppressWarnings("All")
class TabsAndIndentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new TabsAndIndentsVisitor<>(IntelliJ.tabsAndIndents())));
    }

    private static Consumer<RecipeSpec> tabsAndIndents(UnaryOperator<TabsAndIndentsStyle> with) {
        return spec -> spec.recipe(toRecipe(() -> new TabsAndIndentsVisitor<>(with.apply(IntelliJ.tabsAndIndents()))))
          .parser(KotlinParser.builder().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              singletonList(with.apply(IntelliJ.tabsAndIndents()))
            )
          )));
    }

    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    @Issue("https://github.com/openrewrite/rewrite/issues/2251")
    @Test
    void multilineCommentStartPositionIsIndented() {
        rewriteRun(
          kotlin(
            """
              class A {
                  init {
                      if(true)
                          foo();
                          foo();
                      /*
                   line-one
                 line-two
                 */
                  }
                  fun foo() {}
              }
              """,
            """
              class A {
                  init {
                      if(true)
                          foo();
                      foo();
                      /*
                   line-one
                 line-two
                 */
                  }
                  fun foo() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1913")
    @Test
    void alignMethodDeclarationParamsWhenMultiple() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  private fun firstArgNoPrefix (first: String,
                   second: Int,
                   third: String) {
                  }
                  private fun firstArgOnNewLine(
                          first: String,
                   second: Int,
                   third: String) {
                  }
              }
              """,
            """
              class Test {
                  private fun firstArgNoPrefix (first: String,
                                                second: Int,
                                                third: String) {
                  }
                  private fun firstArgOnNewLine(
                          first: String,
                          second: Int,
                          third: String) {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1913")
    @Test
    void alignMethodDeclarationParamsWhenContinuationIndent() {
        rewriteRun(
          tabsAndIndents(style -> style.withFunctionDeclarationParameters(new TabsAndIndentsStyle.FunctionDeclarationParameters(false))),
          kotlin(
            """
              class Test {
                  private fun firstArgNoPrefix(first: String,
                                               second: Int,
                                               third: String) {
                  }
                  private fun firstArgOnNewLine(
                                                first: String,
                                                second: Int,
                                                third: String) {
                  }
              }
              """,
            """
              class Test {
                  private fun firstArgNoPrefix(first: String,
                          second: Int,
                          third: String) {
                  }
                  private fun firstArgOnNewLine(
                          first: String,
                          second: Int,
                          third: String) {
                  }
              }
              """
          )
        );
    }

    // https://rules.sonarsource.com/java/tag/confusing/RSPEC-3973
    @DocumentExample
    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    @Test
    void rspec3973() {
        rewriteRun(
          kotlin(
            """
              class Test { init {
                  if (true == false)
                  doTheThing();
                            
                  doTheOtherThing();
                  somethingElseEntirely();
                            
                  foo();
              }
                  fun doTheThing() {}
                  fun doTheOtherThing() {}
                  fun somethingElseEntirely() {}
                  fun foo() {}
              }
              """,
            """
              class Test { init {
                  if (true == false)
                      doTheThing();
                            
                  doTheOtherThing();
                  somethingElseEntirely();
                            
                  foo();
              }
                  fun doTheThing() {}
                  fun doTheOtherThing() {}
                  fun somethingElseEntirely() {}
                  fun foo() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/623")
    @Test
    void ifElseWithComments() {
        rewriteRun(
          kotlin(
            """
              class B {
                  fun foo(input: Int) {
                      // First case
                      if (input == 0) {
                          // do things
                      }
                      // Second case
                      else if (input == 1) {
                          // do things
                      }
                      // Otherwise
                      else {
                          // do other things
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void annotatedFiled() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno

              class Test {
                   @Anno
                 val id: String = "1"
              }
              """,
            """
              annotation class Anno

              class Test {
                  @Anno
                  val id: String = "1"
              }
              """
          )
        );
    }

    @Test
    void annotationArguments() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress(
                          "unchecked",
                          "ALL"
                  )
                  val id: String = "1"
              }
              """
          )
        );
    }

    @Test
    void methodChain() {
        rewriteRun(
          tabsAndIndents(style -> style.withContinuationIndent(2)),
          kotlin(
            """
              class Test {
                  fun method(t: Test) {
                      this
                        .method(
                          t
                        );
                  }
              }
              """
          )
        );
    }

    @Test
    void returnExpression() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method(): Int {
                     return 1
                  }
              }
              """,
            """
              class Test {
                  fun method(): Int {
                      return 1
                  }
              }
              """
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/636")
    @Test
    void methodInvocationArgumentOnOpeningLineWithMethodSelect() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun withData(vararg arg0: Any): Test {
                      return this
                  }

                  fun method(t: Test) {
                      var t1 = t.withData(withData()
                                      .withData()
                                      .withData(),
                              withData()
                                      .withData()
                                      .withData()
                      )
                  }
              }
              """
          )
        );
    }

    @Test
    void methodInvocationArgumentOnNewLineWithMethodSelect() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun withData(vararg arg0: Any): Test {
                      return this
                  }

                  fun method(t: Test) {
                      var t1 = t.withData(
                              withData(), withData()
                              .withData()
                              .withData()
                      );
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/636")
    @Test
    void methodInvocationArgumentsWithMethodSelectsOnEachNewLine() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun withData(vararg arg0: Test): Test {
                      return this
                  }

                  fun method(t: Test) {
                      var t1 = t.withData(withData()
                              .withData(t
                                      .
                                              withData()
                              )
                              .withData(
                                      t
                                              .
                                                      withData()
                              )
                      );
                  }
              }
              """
          )
        );
    }

    @ExpectedToFail("https://github.com/openrewrite/rewrite/issues/636")
    @Test
    void methodInvocationArgumentsContinuationIndentsAssorted() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun withData(vararg arg0: Test): Test {
                      return this
                  }

                  fun method(t: Test) {
                      val t1 = t.withData(withData()
                              .withData(
                                      t.withData()
                              ).withData(
                                      t.withData()
                              )
                              .withData(),
                              withData()
                      );
                  }
              }
              """
          )
        );
    }

    @Test
    void ifElse() {
        rewriteRun(
          kotlin(
            """
              fun method(a: String) {
                  if (true) {
                      a
                  } else if (false) {
                      a.toLowerCase()
                  } else {
                      a
                  }
              }
              """
          )
        );
    }

    @Test
    void lambda() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val square = { number: Int ->
                      number * number
                  }
              }
              """
          )
        );
    }

    @Test
    void lambdaWithIfElse() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val square = { number: Int ->
                      if (number > 0) {
                          number * number
                      } else {
                          0
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void whenBranch() {
        rewriteRun(
          kotlin(
            """
              fun foo1(condition: Int) {
                  when (condition) {
                      1 -> {
                          println("1")
                      }

                      2 -> {
                          println("2")
                      }

                      3 -> println("3")
                      4 -> println("4")
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/660")
    @Test
    void methodInvocationLambdaBlockWithClosingBracketOnSameLineIndent() {
        rewriteRun(
          kotlin(
            """
              import java.util.Collection;

              class Test {
                  fun withData(vararg arg0: Any): Test {
                      return this;
                  }

                  fun method(t: Test, c: Collection<String>) {
                      val t1 = t.withData(c.stream().map { a ->
                          if (!a.isEmpty()) {
                              a.toLowerCase();
                          } else {
                              a
                          }
                      })
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/660")
    void methodInvocationLambdaBlockWithClosingBracketOnNewLineIndent() {
        rewriteRun(
          kotlin(
            """
              import java.util.*

              class Test {
                  fun withData(vararg arg0: Any): Test {
                      return this
                  }

                  fun method(t: Test, c: Collection<String>) {
                      val t1 = t.withData(c.stream().map { a ->
                          if (!a.isEmpty()) {
                              a.toLowerCase()
                          } else {
                              a
                          }
                      })
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1173")
    @Test
    void methodInvocationLambdaBlockOnSameLine() {
        rewriteRun(
          kotlin(
            """
              import java.util.function.Predicate;

              class SomeUtility {
                  fun test(property: String, test: Predicate<String>): Boolean {
                      return false;
                  }
              }
              """
          ),
          kotlin(
            """
              class Test {
                  fun method() {
                      SomeUtility().test(
                              "hello", { s ->
                                  true
                              })
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/679")
    @Test
    void lambdaBodyWithNestedMethodInvocationLambdaStatementBodyIndent() {
        rewriteRun(
          kotlin(
            """
              import java.util.*;
              import java.util.stream.Collectors;

              class Test {
                  fun method(c: Collection<List<String>>) {
                      c.stream().map { x ->
                          x.stream().max { r1, r2 ->
                              0
                          }
                      }
                              .collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/679")
    void lambdaBodyWithNestedMethodInvocationLambdaExpressionBodyIndent() {
        rewriteRun(
          kotlin(
            """
              import java.util.*
              import java.util.stream.Collectors

              class Test {
                  fun method(c: Collection<List<String>>) {
                      c.stream()
                              .map { x ->
                                  x.stream().max { r1, r2 ->
                                      0
                                  }
                              }
                              .collect(Collectors.toList())
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/679")
    @Test
    void methodInvocationLambdaArgumentIndent() {
        rewriteRun(
          kotlin(
            """
              import java.util.function.Function

              abstract class Test {
                  abstract fun a(f: Function<String, String>): Test

                  fun method(s: String) {
                      a({ 
                              f -> s.toLowerCase()
                      })
                  }
              }
              """
          )
        );
    }

    /**
     * Slight renaming but structurally the same as IntelliJ's code style view.
     */
    @Test
    void tabsAndIndents() {
        rewriteRun(
          kotlin(
            """
              open class Some {
                  private val f: (Int) -> Int = { a: Int -> a * 2 }
                  fun foo(): Int {
                      val test: Int = 12
                      for (i in 10..42) {
                          println(when {
                              i < test -> -1
                              i > test -> 1
                              else -> 0
                          })
                      }
                      if (true) {
                      }
                      while (true) {
                          break
                      }
                      try {
                          when (test) {
                              12 -> println("foo")
                              in 10..42 -> println("baz")
                              else -> println("bar")
                          }
                      } catch (e: Exception) {
                      } finally {
                      }
                      return test
                  }

                  fun multilineMethod(
                          foo: String,
                          bar: String
                          ) {
                      foo
                              .length
                  }

                  fun expressionBodyMethod() =
                      "abc"
              }

              class AnotherClass<T : Any> : Some()
              """
          )
        );
    }

    @Test
    void tryCatchFinally() {
        rewriteRun(
          kotlin(
            """
              class Test {
              fun test(a: Boolean, x: Int, y: Int) {
              try {
              var someVariable = if (a) x else y;
              } catch (e: Exception) {
              e.printStackTrace()
              } finally {
              var b = false
              }
              }
              }
              """,
            """
              class Test {
                  fun test(a: Boolean, x: Int, y: Int) {
                      try {
                          var someVariable = if (a) x else y;
                      } catch (e: Exception) {
                          e.printStackTrace()
                      } finally {
                          var b = false
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doWhile() {
        rewriteRun(
          kotlin(
            """
              class Test {
              public fun test() {
              do {
              }
              while(true);

              labeled@ do {
              }
              while(false);
              }
              }
              """,
            """
              class Test {
                  public fun test() {
                      do {
                      }
                      while(true);

                      labeled@ do {
                      }
                      while(false);
                  }
              }
              """
          )
        );
    }

    @Test
    void elseBody() {
        rewriteRun(
          kotlin(
            """
              public class Test {
              public fun test(a: Boolean, x: Int, y: Int, z: Int) {
              if (x > 0) {
              } else if (x < 0) {
              var m = z
              }
              }
              }
              """,
            """
              public class Test {
                  public fun test(a: Boolean, x: Int, y: Int, z: Int) {
                      if (x > 0) {
                      } else if (x < 0) {
                          var m = z
                      }
                  }
              }
              """
          )
        );
    }

    @ExpectedToFail
    @Test
    void forLoop() {
        rewriteRun(
          tabsAndIndents(style -> style.withContinuationIndent(2)),
          kotlin(
            """
              class Test {
              fun test() {
              for (
              i in 0..5
              ) {
              }

              for (j
              in 0..5
              ) {
              }

              labeled@ for (i in
              0..5) {
              }
              }
              }
              """,
            """
              class Test {
                  fun test() {
                      for (
                      i in 0..5
                      ) {
                      }

                      for (j
                      in 0..5
                      ) {
                      }

                      labeled@ for (i in
                      0..5) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void methodDeclaration() {
        rewriteRun(
          tabsAndIndents(style -> style.withContinuationIndent(2)),
          kotlin(
            """
              public class Test {
                  fun test(a: Int,
                           b: Int) {
                  }

                  public fun test2(
                    a: Int,
                    b: Int) {
                  }
              }
              """
          )
        );
    }

    @Test
    void lineComment() {
        rewriteRun(
          kotlin(
            """
              public class A {
                // comment at indent 2
              public fun method() {}
              }
              """,
            """
              public class A {
                  // comment at indent 2
                  public fun method() {}
              }
              """
          )
        );
    }

    @Test
    void noIndexOutOfBoundsUsingSpaces() {
        rewriteRun(
          kotlin(
            """
              public class A {
                // length = 1 from new line.
                    val valA = 10 // text.length = 1 + shift -2 == -1.
              }
              """,
            """
              public class A {
                  // length = 1 from new line.
                  val valA = 10 // text.length = 1 + shift -2 == -1.
              }
              """
          )
        );
    }

    @Test
    void noIndexOutOfBoundsUsingTabs() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true).withTabSize(1).withIndentSize(1)),
          kotlin(
            """
              class Test {
              	fun test() {
              		System.out.println() // comment
              	}
              }
              """
          )
        );
    }

    @Test
    void blockComment() {
        rewriteRun(
          kotlin(
            """
              public class A {
              /*a
                b*/
              public fun method() {}
              }
              """,
            """
              public class A {
                  /*a
                    b*/
                  public fun method() {}
              }
              """
          )
        );
    }

    @SuppressWarnings("TextBlockMigration")
    @Test
    void blockCommentCRLF() {
        rewriteRun(
          kotlin(
            "public class A {\r\n" +
              "/*a\r\n" +
              "  b*/\r\n" +
              "public fun method() {}\r\n" +
              "}",
            "public class A {\r\n" +
              "    /*a\r\n" +
              "      b*/\r\n" +
              "    public fun method() {}\r\n" +
              "}"
          )
        );
    }

    @SuppressWarnings("EmptyClassInitializer")
    @Test
    void initBlocks() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  init {
                      System.out.println("hi")
                  }

                  init {
                  }
              }
              """
          )
        );
    }

    @Test
    void moreAnnotations() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              
              class Test {
                  @Suppress(
                          "unchecked"
                  )
                  @Anno
                  var id: Int = 0
              }
              """
          )
        );
    }

    @Test
    void annotations() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              
              @Suppress("A")
              @Anno
                 class A {
              @Anno
              @Suppress("ALL")
                 class B {
              }
              }
              """,
            """
              annotation class Anno
              
              @Suppress("A")
              @Anno
              class A {
                  @Anno
                  @Suppress("ALL")
                  class B {
                  }
              }
              """
          )
        );
    }

    @Disabled("java doc is not parsed")
    @Test
    void javadoc() {
        rewriteRun(
          kotlin(
            """
              class A {
              /**
                      * This is a javadoc
                          */
                  fun method() {}
              }
              """,
            """
              class A {
                  /**
                   * This is a javadoc
                   */
                  fun method() {}
              }
              """
          )
        );
    }

    @Test
    void tabs() {
        rewriteRun(
          // TIP: turn on "Show Whitespaces" in the IDE to see this test clearly
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          kotlin(
            """
              public class A {
              	public fun method() {
              	var n = 0
              	}
              }
              """,
            """
              public class A {
              	public fun method() {
              		var n = 0
              	}
              }
              """
          )
        );
    }

    @Test
    void shiftRight() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun test(a: Boolean,  x: Int,  y: Int) {
                      try {
                  var someVariable = if (a) x else y
                      } catch (e: Exception) {
                          e.printStackTrace();
                      } finally {
                          var a = false;
                      }
                  }
              }
              """,
            """
              class Test {
                  fun test(a: Boolean,  x: Int,  y: Int) {
                      try {
                          var someVariable = if (a) x else y
                      } catch (e: Exception) {
                          e.printStackTrace();
                      } finally {
                          var a = false;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void shiftRightTabs() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          kotlin(
            """
              class Test {
              	fun test(a: Boolean,  x: Int,  y: Int) {
              		try {
              	var someVariable = if (a) x else y
              		} catch (e: Exception) {
              			e.printStackTrace();
              		} finally {
              			var a = false;
              		}
              	}
              }
              """,
            """
              class Test {
              	fun test(a: Boolean,  x: Int,  y: Int) {
              		try {
              			var someVariable = if (a) x else y
              		} catch (e: Exception) {
              			e.printStackTrace();
              		} finally {
              			var a = false;
              		}
              	}
              }
              """
          )
        );
    }

    @Test
    void shiftLeft() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun test(a: Boolean,  x: Int,  y: Int) {
                      try {
                                                var someVariable = if (a) x else y
                      } catch (e: Exception) {
                          e.printStackTrace();
                      } finally {
                          var a = false;
                      }
                  }
              }
              """,
            """
              class Test {
                  fun test(a: Boolean,  x: Int,  y: Int) {
                      try {
                          var someVariable = if (a) x else y
                      } catch (e: Exception) {
                          e.printStackTrace();
                      } finally {
                          var a = false;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void shiftLeftTabs() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true)),
          kotlin(
            """
              class Test {
              	fun test(a: Boolean,  x: Int,  y: Int) {
              		try {
              						var someVariable = if (a) x else y
              		} catch (e: Exception) {
              			e.printStackTrace();
              		} finally {
              			var a = false;
              		}
              	}
              }
              """,
            """
              class Test {
              	fun test(a: Boolean,  x: Int,  y: Int) {
              		try {
              			var someVariable = if (a) x else y
              		} catch (e: Exception) {
              			e.printStackTrace();
              		} finally {
              			var a = false;
              		}
              	}
              }
              """
          )
        );
    }

    @Test
    void nestedIfElse() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method() {
                      if (true) { // comment
                          if (true) {
                          } else {
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void annotationOnSameLine() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              class Test {
                  @Anno fun method(): Int {
                      return 1
                  }
              }
              """
          )
        );
    }

    @Test
    void newClassAsMethodArgument() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  constructor(s: String, m: Int) {
                  }

                  fun method(t: Test) {
                      method(Test("hello" +
                              "world",
                              1))
                  }
              }
              """
          )
        );
    }

    @Test
    void methodArgumentsThatDontStartOnNewLine() {
        rewriteRun(
          kotlin(
            """
              import java.io.File;
              class Test {
                  fun method(n: Int,f: File,m: Int,l: Int) {
                      method(n, File(
                                      "test"
                              ),
                              m,
                              l)
                  }

                  fun method2(n: Int,f: File,m: Int) {
                      method(n, File(
                                      "test"
                              ), m,
                              0)
                  }

                  fun method3(n: Int,f: File) {
                      method2(n, File(
                              "test"
                      ), 0)
                  }

                  fun method4(n: Int) {
                      method3(n, File(
                              "test"
                      ))
                  }
              }
              """
          )
        );
    }

    @Test
    void methodArgumentsThatDontStartOnNewLine2() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method5(n: Int, m: Int) {
                      method5(1,
                              2);
                      return method5(method5(method5(method5(3,
                              4),
                              5),
                              6),
                              7);
                  }
              }
              """
          )
        );
    }

    @Test
    void identAndFieldAccess() {
        rewriteRun(
          kotlin(
            """
              import java.util.stream.Stream;

              class Test {
                  var t: Test = this;
                  fun method(n: Stream<*>?,m: Int): Test {
                      this.t.t
                              .method(null, 1)
                              .t
                              .method(null, 2);
                      Stream
                              .of("a");
                      method(Stream
                                      .of("a"),
                              3
                      );
                      return this
                  }
              }
              """
          )
        );
    }

    @Test
    void lambda1() {
        rewriteRun(
          kotlin(
            """
              import java.util.function.Supplier

              class Test {
                  fun method(n: Int) {
                      val ns: Supplier<Int> = Supplier { ->
                              n
                      }
                  }
              }
              """,
            """
              import java.util.function.Supplier

              class Test {
                  fun method(n: Int) {
                      val ns: Supplier<Int> = Supplier { ->
                          n
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void lambdaWithBlock() {
        rewriteRun(
          kotlin(
            """
              import java.util.function.Supplier

              class Test {
                  fun method(s: Supplier<String>,  n: Int) {
                      method({ ->
                                  "hi"
                              },
                              n);
                  }
              }
              """
          )
        );
    }

    @Test
    void enums() {
        rewriteRun(
          kotlin(
            """
              enum class Scope {
                  None,
                  Compile
              }
              """
          )
        );
    }

    @Test
    void twoThrows() {
        rewriteRun(
          kotlin(
            """
              import java.io.IOException

              class Test {
                  @Throws(
                          IOException::class,
                          Exception::class)
                      fun method() {
                      }

                  @Throws(IOException::class, Exception::class)
                      fun method2() {
                      }
              }
              """
          )
        );
    }

    @Test
    void twoTypeParameters() {
        rewriteRun(
          kotlin(
            """
              interface A
              interface B
              class Test<A,
                      B> {
              }
              """
          )
        );
    }

    @Test
    void twoImplements() {
        rewriteRun(
          kotlin(
            """
              interface A
              interface B

              class Test : A, B {
              }
              """
          )
        );
    }

    @Test
    void fieldsWhereClassHasAnnotation() {
        rewriteRun(
          kotlin(
            """
              @Suppress("ALL")
              class Test {
                  val groupId: String = ""
                  val artifactId: String = ""
              }
              """
          )
        );
    }
//
//    @Test
//    void methodWithAnnotation() {
//        rewriteRun(
//          kotlin(
//            """
//              annotation class Anno
//
//              class Test {
//                  @Anno
//                  @Suppress("all")
//                 fun getOnError(): String {
//                      return "uh oh";
//                  }
//              }
//              """,
//            """
//              annotation class Anno
//
//              class Test {
//                  @Anno
//                  @Suppress("all")
//                  fun getOnError(): String {
//                      return "uh oh";
//                  }
//              }
//              """
//          )
//        );
//    }

//
//    @SuppressWarnings({"CStyleArrayDeclaration", "EnhancedSwitchMigration"})
//    @Test
//    void containers() {
//        rewriteRun(
//          java(
//            """
//              import java.io.ByteArrayInputStream;
//              import java.io.InputStream;
//              import java.io.Serializable;
//              import java.lang.annotation.Retention;
//              @Retention
//              (value = "1.0")
//              public
//              class
//              Test
//              <T
//              extends Object>
//              implements
//              Serializable {
//                  Test method
//                  ()
//                  throws Exception {
//                      try
//                      (InputStream is = new ByteArrayInputStream(new byte[0])) {}
//                      int n[] =
//                      {0};
//                      switch (1) {
//                      case 1:
//                      n
//                      [0]++;
//                      }
//                      return new Test
//                      ();
//                  }
//              }
//              """,
//            """
//              import java.io.ByteArrayInputStream;
//              import java.io.InputStream;
//              import java.io.Serializable;
//              import java.lang.annotation.Retention;
//              @Retention
//                      (value = "1.0")
//              public
//              class
//              Test
//                      <T
//                              extends Object>
//                      implements
//                      Serializable {
//                  Test method
//                          ()
//                          throws Exception {
//                      try
//                              (InputStream is = new ByteArrayInputStream(new byte[0])) {}
//                      int n[] =
//                              {0};
//                      switch (1) {
//                          case 1:
//                              n
//                                      [0]++;
//                      }
//                      return new Test
//                              ();
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Test
//    void methodInvocations() {
//        rewriteRun(
//          java(
//            """
//              class Test {
//                  Test method(int n) {
//                      return method(n)
//                              .method(n)
//                              .method(n);
//                  }
//
//                  Test method2() {
//                      return method2().
//                              method2().
//                              method2();
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Test
//    void ternaries() {
//        rewriteRun(
//          java(
//            """
//              public class Test {
//                  public Test method(int n) {
//                      return n > 0 ?
//                          this :
//                          method(n).method(n);
//                  }
//              }
//              """,
//            """
//              public class Test {
//                  public Test method(int n) {
//                      return n > 0 ?
//                              this :
//                              method(n).method(n);
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Test
//    void newClassAsArgument() {
//        rewriteRun(
//          java(
//            """
//              import java.io.File;
//              class Test {
//                  void method(int m, File f, File f2) {
//                      method(m, new File(
//                                      "test"
//                              ),
//                              new File("test",
//                                      "test"
//                              ));
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Test
//    void variableWithAnnotation() {
//        rewriteRun(
//          java(
//            """
//              public class Test {
//                  @Deprecated
//                  final String scope;
//
//                  @Deprecated
//                  String classifier;
//              }
//              """
//          )
//        );
//    }
//
//    @Test
//    void lambdaMethodParameter2() {
//        rewriteRun(
//          java(
//            """
//              import java.util.function.Function;
//
//              abstract class Test {
//                  abstract Test a(Function<Test, Test> f);
//                  abstract Test b(Function<Test, Test> f);
//                  abstract Test c(Function<Test, Test> f);
//
//                  Test method(Function<Test, Test> f) {
//                      return a(f)
//                              .b(
//                                      t ->
//                                              c(f)
//                              );
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Test
//    void lambdaMethodParameter() {
//        rewriteRun(
//          java(
//            """
//              import java.util.function.Function;
//              abstract class Test {
//                  abstract Test a(Function<Test, Test> f);
//                  abstract Test b(Function<Test, Test> f);
//                  abstract Test c(Function<Test, Test> f);
//
//                  Test method(Function<Test, Test> f) {
//                      return a(f)
//                              .b(t ->
//                                      c(f)
//                              );
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Test
//    void failure1() {
//        rewriteRun(
//          java(
//            """
//              public class Test {
//                  public static DefaultRepositorySystemSession getRepositorySystemSession(RepositorySystem system, // comments here
//                                                                                          @Nullable File localRepositoryDir) {
//                      DefaultRepositorySystemSession repositorySystemSession = org.apache.maven.repository.internal.MavenRepositorySystemUtils
//                              .newSession();
//                      repositorySystemSession.setDependencySelector(
//                              new AndDependencySelector(
//                                      new ExclusionDependencySelector(), // some comments
//                                      new ScopeDependencySelector(emptyList(), Arrays.asList("provided", "test")),
//                                      // more comments
//                                      new OptionalDependencySelector()
//                              )
//                      );
//                      return repositorySystemSession;
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @SuppressWarnings("DuplicateCondition")
//    @Test
//    void methodInvocationsNotContinuationIndentedWhenPartOfBinaryExpression() {
//        rewriteRun(
//          java(
//            """
//              import java.util.stream.Stream;
//              public class Test {
//                  boolean b;
//                  public Stream<Test> method() {
//                      if (b && method()
//                              .anyMatch(t -> b ||
//                                      b
//                              )) {
//                          // do nothing
//                      }
//                      return Stream.of(this);
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @SuppressWarnings("CStyleArrayDeclaration")
//    @Test
//    void punctuation() {
//        rewriteRun(
//          tabsAndIndents(style -> style.withContinuationIndent(2)),
//          java(
//            """
//              import java.util.function.Function;
//              public class Test {
//              int X[];
//              public int plus(int x) {
//                  return 0;
//              }
//              public void test(boolean a, int x, int y) {
//              Function<Integer, Integer> op = this
//              ::
//              plus;
//              if (x
//              >
//              0) {
//              int someVariable = a ?
//              x :
//              y;
//              int anotherVariable = a
//              ?
//              x
//              :
//              y;
//              }
//              x
//              ++;
//              X
//              [
//              1
//              ]
//              =
//              0;
//              }
//              }
//              """,
//            """
//              import java.util.function.Function;
//              public class Test {
//                  int X[];
//                  public int plus(int x) {
//                      return 0;
//                  }
//                  public void test(boolean a, int x, int y) {
//                      Function<Integer, Integer> op = this
//                        ::
//                        plus;
//                      if (x
//                        >
//                        0) {
//                          int someVariable = a ?
//                            x :
//                            y;
//                          int anotherVariable = a
//                            ?
//                            x
//                            :
//                            y;
//                      }
//                      x
//                        ++;
//                      X
//                        [
//                        1
//                        ]
//                        =
//                        0;
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Test
//    void newClass() {
//        rewriteRun(
//          java(
//            """
//              class Test {
//                  Test(Test t) {}
//                  Test() {}
//                  void method(Test t) {
//                      method(
//                          new Test(
//                              new Test()
//                          )
//                      );
//                  }
//              }
//              """,
//            """
//              class Test {
//                  Test(Test t) {}
//                  Test() {}
//                  void method(Test t) {
//                      method(
//                              new Test(
//                                      new Test()
//                              )
//                      );
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/issues/642")
//    @Test
//    void alignLineComments() {
//        rewriteRun(
//          java(
//            """
//                      // shift left.
//              package org.openrewrite; // trailing comment.
//
//                      // shift left.
//                      public class A { // trailing comment at class.
//                // shift right.
//                      // shift left.
//                              public int method(int value) { // trailing comment at method.
//                  // shift right.
//                          // shift left.
//                  if (value == 1) { // trailing comment at if.
//                // suffix contains new lines with whitespace.
//
//
//                      // shift right.
//                                   // shift left.
//                              value += 10; // trailing comment.
//                      // shift right at end of block.
//                              // shift left at end of block.
//                                      } else {
//                          value += 30;
//                      // shift right at end of block.
//                              // shift left at end of block.
//                 }
//
//                              if (value == 11)
//                      // shift right.
//                              // shift left.
//                          value += 1;
//
//                  return value;
//                  // shift right at end of block.
//                          // shift left at end of block.
//                          }
//                // shift right at end of block.
//                      // shift left at end of block.
//                          }
//              """,
//            """
//              // shift left.
//              package org.openrewrite; // trailing comment.
//
//              // shift left.
//              public class A { // trailing comment at class.
//                  // shift right.
//                  // shift left.
//                  public int method(int value) { // trailing comment at method.
//                      // shift right.
//                      // shift left.
//                      if (value == 1) { // trailing comment at if.
//                          // suffix contains new lines with whitespace.
//
//
//                          // shift right.
//                          // shift left.
//                          value += 10; // trailing comment.
//                          // shift right at end of block.
//                          // shift left at end of block.
//                      } else {
//                          value += 30;
//                          // shift right at end of block.
//                          // shift left at end of block.
//                      }
//
//                      if (value == 11)
//                          // shift right.
//                          // shift left.
//                          value += 1;
//
//                      return value;
//                      // shift right at end of block.
//                      // shift left at end of block.
//                  }
//                  // shift right at end of block.
//                  // shift left at end of block.
//              }
//              """
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/pull/659")
//    @Test
//    void alignMultipleBlockCommentsOnOneLine() {
//        rewriteRun(
//          java(
//            """
//              public class A {
//                  public void method() {
//                              /* comment 1 */ /* comment 2 */ /* comment 3 */
//                  }
//              }
//              """,
//            """
//              public class A {
//                  public void method() {
//                      /* comment 1 */ /* comment 2 */ /* comment 3 */
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/pull/659")
//    @Test
//    void alignMultipleBlockComments() {
//        rewriteRun(
//          java(
//            """
//              public class A {
//              /* Preserve whitespace
//                 alignment */
//
//                     /* Shift next blank line left
//
//                      * This line should be aligned
//                      */
//
//              /* This comment
//               * should be aligned */
//              public void method() {}
//              }
//              """,
//            """
//              public class A {
//                  /* Preserve whitespace
//                     alignment */
//
//                  /* Shift next blank line left
//
//                   * This line should be aligned
//                   */
//
//                  /* This comment
//                   * should be aligned */
//                  public void method() {}
//              }
//              """
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/issues/641")
//    @Test
//    void alignTryCatchFinally() {
//        rewriteRun(
//          java(
//            """
//              public class Test {
//                  public void method() {
//                      // inline try, catch, finally.
//                      try {
//
//                      } catch (Exception ex) {
//
//                      } finally {
//
//                      }
//
//                      // new line try, catch, finally.
//                      try {
//
//                      }
//                      catch (Exception ex) {
//
//                      }
//                      finally {
//
//                      }
//                  }
//              }
//              """
//          )
//        );
//    }
//
////    @Issue("https://github.com/openrewrite/rewrite/issues/663")
////    @Test
////    void alignBlockPrefixes() {
////        rewriteRun(
////          spec -> spec.recipe(new AutoFormat()),
////          java(
////            """
////              public class Test {
////
////                  public void practiceA()
////                  {
////                      for (int i = 0; i < 10; ++i)
////                      {
////                          if (i % 2 == 0)
////                          {
////                              try
////                              {
////                                  Integer value = Integer.valueOf("100");
////                              }
////                              catch (Exception ex)
////                              {
////                                  throw new RuntimeException();
////                              }
////                              finally
////                              {
////                                  System.out.println("out");
////                              }
////                          }
////                      }
////                  }
////
////                  public void practiceB() {
////                      for (int i = 0; i < 10; ++i) {
////                          if (i % 2 == 0) {
////                              try {
////                                  Integer value = Integer.valueOf("100");
////                              } catch (Exception ex) {
////                                  throw new RuntimeException();
////                              } finally {
////                                  System.out.println("out");
////                              }
////                          }
////                      }
////                  }
////              }
////              """
////          )
////        );
////    }
//
//    @Test
//    void alignInlineBlockComments() {
//        rewriteRun(
//          java(
//            """
//              public class WhitespaceIsHard {
//              /* align comment */ public void method() { /* tricky */
//              /* align comment */ int var = 10; /* tricky */
//              // align comment and end paren.
//              }
//              }
//              """,
//            """
//              public class WhitespaceIsHard {
//                  /* align comment */ public void method() { /* tricky */
//                      /* align comment */ int var = 10; /* tricky */
//                      // align comment and end paren.
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Test
//    void trailingMultilineString() {
//        rewriteRun(
//          java(
//            """
//              public class WhitespaceIsHard {
//                  public void method() { /* tricky */
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/issues/1076")
//    @Test
//    void javaDocsWithMultipleLeadingAsterisks() {
//        rewriteRun(
//          java(
//            """
//                  /******** Align JavaDoc with multiple leading '*' in margin left.
//                   **** Align left
//                   */
//              public class Test {
//              /******** Align JavaDoc with multiple leading '*' in margin right.
//               **** Align right
//               */
//                  void method() {
//                  }
//              }
//              """,
//            """
//              /******** Align JavaDoc with multiple leading '*' in margin left.
//               **** Align left
//               */
//              public class Test {
//                  /******** Align JavaDoc with multiple leading '*' in margin right.
//                   **** Align right
//                   */
//                  void method() {
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @SuppressWarnings("TextBlockMigration")
//    @Issue("https://github.com/openrewrite/rewrite/issues/980")
//    @Test
//    void alignJavaDocsWithCRLF() {
//        rewriteRun(
//          java(
//            "        /**\r\n" +
//              "         * Align JavaDoc left that starts on 2nd line.\r\n" +
//              "         */\r\n" +
//              "public class A {\r\n" +
//              "/** Align JavaDoc right that starts on 1st line.\r\n" +
//              "  * @param value test value.\r\n" +
//              "  * @return value + 1 */\r\n" +
//              "        public int methodOne(int value) {\r\n" +
//              "            return value + 1;\r\n" +
//              "        }\r\n" +
//              "\r\n" +
//              "                /** Edge case formatting test.\r\n" +
//              "   @param value test value.\r\n" +
//              "                 @return value + 1\r\n" +
//              "                 */\r\n" +
//              "        public int methodTwo(int value) {\r\n" +
//              "            return value + 1;\r\n" +
//              "        }\r\n" +
//              "}"
//            ,
//            "/**\r\n" +
//              " * Align JavaDoc left that starts on 2nd line.\r\n" +
//              " */\r\n" +
//              "public class A {\r\n" +
//              "    /** Align JavaDoc right that starts on 1st line.\r\n" +
//              "     * @param value test value.\r\n" +
//              "     * @return value + 1 */\r\n" +
//              "    public int methodOne(int value) {\r\n" +
//              "        return value + 1;\r\n" +
//              "    }\r\n" +
//              "\r\n" +
//              "    /** Edge case formatting test.\r\n" +
//              "     @param value test value.\r\n" +
//              "     @return value + 1\r\n" +
//              "     */\r\n" +
//              "    public int methodTwo(int value) {\r\n" +
//              "        return value + 1;\r\n" +
//              "    }\r\n" +
//              "}"
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/pull/659")
//    @Test
//    void alignJavaDocs() {
//        rewriteRun(
//          java(
//            """
//                      /**
//                       * Align JavaDoc left that starts on 2nd line.
//                       */
//              public class A {
//              /** Align JavaDoc right that starts on 1st line.
//                * @param value test value.
//                * @return value + 1 */
//                      public int methodOne(int value) {
//                          return value + 1;
//                      }
//
//                              /** Edge case formatting test.
//                 @param value test value.
//                               @return value + 1
//                               */
//                      public int methodTwo(int value) {
//                          return value + 1;
//                      }
//              }
//              """,
//            """
//              /**
//               * Align JavaDoc left that starts on 2nd line.
//               */
//              public class A {
//                  /** Align JavaDoc right that starts on 1st line.
//                   * @param value test value.
//                   * @return value + 1 */
//                  public int methodOne(int value) {
//                      return value + 1;
//                  }
//
//                  /** Edge case formatting test.
//                   @param value test value.
//                   @return value + 1
//                   */
//                  public int methodTwo(int value) {
//                      return value + 1;
//                  }
//              }
//              """
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/issues/709")
//    @Test
//    void useContinuationIndentExtendsOnNewLine() {
//        rewriteRun(
//          java("package org.a; public class A {}"),
//          java(
//            """
//              package org.b;
//              import org.a.A;
//              class B
//                      extends A {
//              }
//              """
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/issues/883")
//    @Test
//    void alignIdentifierOnNewLine() {
//        rewriteRun(
//          java("package org.a; public class A {}"),
//          java(
//            """
//              package org.b;
//              import org.a.A;
//              class B extends
//                      A {
//              }
//              """
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/issues/1526")
//    @Test
//    void doNotFormatSingleLineCommentAtCol0() {
//        rewriteRun(
//          java(
//            """
//              class A {
//              // DO NOT shift the whitespace of `Space` and the suffix of comment 1.
//              // DOES shift the suffix of comment 2.
//              void shiftRight() {}
//              }
//              """,
//            """
//              class A {
//              // DO NOT shift the whitespace of `Space` and the suffix of comment 1.
//              // DOES shift the suffix of comment 2.
//                  void shiftRight() {}
//              }
//              """
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/issues/2968")
//    @Test
//    void recordComponents() {
//        rewriteRun(
//          java(
//            """
//              public record RenameRequest(
//                  @NotBlank
//                  @JsonProperty("name") String name) {
//              }
//              """
//          )
//        );
//    }
//
//    @Issue("https://github.com/openrewrite/rewrite/issues/3089")
//    @Test
//    void enumConstants() {
//        rewriteRun(
//          java(
//            """
//              public enum WorkflowStatus {
//                  @SuppressWarnings("value1")
//                  VALUE1,
//                  @SuppressWarnings("value2")
//                  VALUE2,
//                  @SuppressWarnings("value3")
//                  VALUE3,
//                  @SuppressWarnings("value4")
//                  VALUE4
//              }
//              """
//          )
//        );
//    }
}