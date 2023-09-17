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

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void resetIndentationAfterParametersList() {
        rewriteRun(
          tabsAndIndents(style -> style.withFunctionDeclarationParameters(new TabsAndIndentsStyle.FunctionDeclarationParameters(false))),
          kotlin(
            """
              data class A(
                      val a: Boolean,
                      val b: Boolean,
                      val c: Boolean,
                      val d: Boolean
              ) {
                  fun foo(
                  ) = ""
              }
              """
          )
        );
    }

    @SuppressWarnings("SuspiciousIndentAfterControlStatement")
    @Issue("https://github.com/openrewrite/rewrite/issues/2251")
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @ExpectedToFail("expected kotlin.Any but kotlin.Array<Generic{ extends kotlin.Any}>")
    @Issue("https://github.com/openrewrite/rewrite/issues/636")
    @Disabled("FIXME, to be supported by PSI parser")
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

    @ExpectedToFail("expected kotlin.Any but kotlin.Array<Generic{ extends kotlin.Any}>")
    @Disabled("FIXME, to be supported by PSI parser")
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

    @ExpectedToFail("vararg")
    @Issue("https://github.com/openrewrite/rewrite/issues/636")
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @ExpectedToFail("expected kotlin.Any but kotlin.Array<Generic{ extends kotlin.Any}>")
    @Issue("https://github.com/openrewrite/rewrite/issues/660")
    @Disabled("FIXME, to be supported by PSI parser")
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

    @ExpectedToFail("expected kotlin.Any but kotlin.Array<Generic{ extends kotlin.Any}>")
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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
    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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
    // @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
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

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void methodWithAnnotation() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno

              class Test {
                  @Anno
                  @Suppress("all")
                 fun getOnError(): String {
                      return "uh oh";
                  }
              }
              """,
            """
              annotation class Anno

              class Test {
                  @Anno
                  @Suppress("all")
                  fun getOnError(): String {
                      return "uh oh";
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void methodInvocations() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  fun method(n: Int): Test {
                      return method(n)
                              .method(n)
                              .method(n);
                  }

                  fun method2(): Test {
                      return method2().
                              method2().
                              method2();
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void newClassAsArgument() {
        rewriteRun(
          kotlin(
            """
              import java.io.File
              
              class Test {
                  fun method(m: Int, f: File, f2: File) {
                      method(m, File(
                                      "test"
                              ), 
                              File("test", 
                                      "test"
                              ))
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void variableWithAnnotation() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  @Suppress("All")
                  val scope: String = "a"

                  @Suppress("All")
                  val  classifier: String = "b"
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void lambdaMethodParameter2() {
        rewriteRun(
          kotlin(
            """
              import java.util.function.Function
              
              abstract class Test {
                  abstract fun a(f: Function<Test, Test>): Test
                  abstract fun b(f: Function<Test, Test>): Test
                  abstract fun c(f: Function<Test, Test>): Test
              
                  fun method(f: Function<Test, Test>): Test {
                      return a(f)
                              .b { 
                                      t ->
                                  c(f)
                              }
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void lambdaMethodParameter() {
        rewriteRun(
          kotlin(
            """
              import java.util.function.Function
              
              abstract class Test {
                  abstract fun a(f: Function<Test, Test>): Test
                  abstract fun b(f: Function<Test, Test>): Test
                  abstract fun c(f: Function<Test, Test>): Test
              
                  fun method(f: Function<Test, Test>): Test {
                      return a(f)
                              .b {t ->
                                  c(f)
                              }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("DuplicateCondition")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void methodInvocationsNotContinuationIndentedWhenPartOfBinaryExpression() {
        rewriteRun(
          kotlin(
            """
              import java.util.stream.Stream
              
              class Test {
                  var b: Boolean = false
                  fun method(): Stream<Test> {
                      if (b && method()
                              .anyMatch { t -> b || 
                                      b
                              }) {
                          // do nothing
                      }
                      return Stream.of(this)
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void newClass() {
        rewriteRun(
          kotlin(
            """
              class Test {
                  constructor(t: Test)
                  constructor()
              
                  fun method(t: Test) {
                      method(
                          Test(
                              Test()
                          )
                      )
                  }
              }
              """,
            """
              class Test {
                  constructor(t: Test)
                  constructor()
              
                  fun method(t: Test) {
                      method(
                              Test(
                                      Test()
                              )
                      )
                  }
              }
              """
          )
        );
    }

    @Disabled("Parsing error")
    @Issue("https://github.com/openrewrite/rewrite/issues/642")
    // @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void alignLineComments() {
        rewriteRun(
          kotlin(
            """
                      // shift left.
              package org.openrewrite; // trailing comment.

                      // shift left.
                      public class A { // trailing comment at class.
                // shift right.
                      // shift left.
                              public fun method(value: Int): Int { // trailing comment at method.
                  // shift right.
                          // shift left.
                  if (value == 1) { // trailing comment at if.
                // suffix contains new lines with whitespace.


                      // shift right.
                                   // shift left.
                              var value = 10 // trailing comment.
                      // shift right at end of block.
                              // shift left at end of block.
                                      } else {
                          var value = 30
                      // shift right at end of block.
                              // shift left at end of block.
                 }

                              if (value == 11)
                      // shift right.
                              // shift left.
                          method(1)

                  return 1
                  // shift right at end of block.
                          // shift left at end of block.
                          }
                // shift right at end of block.
                      // shift left at end of block.
                          }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void alignMultipleBlockCommentsOnOneLine() {
        rewriteRun(
          kotlin(
            """
              public class A {
                  public fun method() {
                              /* comment 1 */ /* comment 2 */ /* comment 3 */
                  }
              }
              """,
            """
              public class A {
                  public fun method() {
                      /* comment 1 */ /* comment 2 */ /* comment 3 */
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void alignMultipleBlockComments() {
        rewriteRun(
          kotlin(
            """
              public class A {
              /* Preserve whitespace
                 alignment */

                     /* Shift next blank line left

                      * This line should be aligned
                      */

              /* This comment
               * should be aligned */
              public fun method() {}
              }
              """,
            """
              public class A {
                  /* Preserve whitespace
                     alignment */

                  /* Shift next blank line left

                   * This line should be aligned
                   */

                  /* This comment
                   * should be aligned */
                  public fun method() {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/641")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void alignTryCatchFinally() {
        rewriteRun(
          kotlin(
            """
              public class Test {
                  public fun method() {
                      // inline try, catch, finally.
                      try {

                      } catch (ex: Exception) {

                      } finally {

                      }

                      // new line try, catch, finally.
                      try {

                      }
                      catch (ex: Exception) {

                      }
                      finally {

                      }
                  }
              }
              """
          )
        );
    }
//
//    @Issue("https://github.com/openrewrite/rewrite/issues/663")
//    @Test
//    void alignBlockPrefixes() {
//        rewriteRun(
//          spec -> spec.recipe(new AutoFormat()),
//          java(
//            """
//              public class Test {
//
//                  public void practiceA()
//                  {
//                      for (int i = 0; i < 10; ++i)
//                      {
//                          if (i % 2 == 0)
//                          {
//                              try
//                              {
//                                  Integer value = Integer.valueOf("100");
//                              }
//                              catch (Exception ex)
//                              {
//                                  throw new RuntimeException();
//                              }
//                              finally
//                              {
//                                  System.out.println("out");
//                              }
//                          }
//                      }
//                  }
//
//                  public void practiceB() {
//                      for (int i = 0; i < 10; ++i) {
//                          if (i % 2 == 0) {
//                              try {
//                                  Integer value = Integer.valueOf("100");
//                              } catch (Exception ex) {
//                                  throw new RuntimeException();
//                              } finally {
//                                  System.out.println("out");
//                              }
//                          }
//                      }
//                  }
//              }
//              """
//          )
//        );
//    }


    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void alignInlineBlockComments() {
        rewriteRun(
          kotlin(
            """
              public class WhitespaceIsHard {
              /* align comment */ public fun method() { /* tricky */
              /* align comment */ var x = 10; /* tricky */
              // align comment and end paren.
              }
              }
              """,
            """
              public class WhitespaceIsHard {
                  /* align comment */ public fun method() { /* tricky */
                      /* align comment */ var x = 10; /* tricky */
                      // align comment and end paren.
                  }
              }
              """
          )
        );
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void trailingMultilineString() {
        rewriteRun(
          kotlin(
            """
              public class WhitespaceIsHard {
                  public fun method() { /* tricky */
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1076")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void javaDocsWithMultipleLeadingAsterisks() {
        rewriteRun(
          kotlin(
            """
                  /******** Align JavaDoc with multiple leading '*' in margin left.
                   **** Align left
                   */
              public class Test {
              /******** Align JavaDoc with multiple leading '*' in margin right.
               **** Align right
               */
                  fun method() {
                  }
              }
              """,
            """
              /******** Align JavaDoc with multiple leading '*' in margin left.
                **** Align left
                */
              public class Test {
                  /******** Align JavaDoc with multiple leading '*' in margin right.
                   **** Align right
                   */
                  fun method() {
                  }
              }
              """
          )
        );
    }

    @Disabled("java doc is not parsed")
    @Issue("https://github.com/openrewrite/rewrite/pull/659")
    // @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void alignJavaDocs() {
        rewriteRun(
          kotlin(
            """
                      /**
                       * Align JavaDoc left that starts on 2nd line.
                       */
              public class A {
              /** Align JavaDoc right that starts on 1st line.
                * @param value test value.
                * @return value + 1 */
                      public fun methodOne(value: Int) : Int {
                          return value + 1
                      }

                              /** Edge case formatting test.
                 @param value test value.
                               @return value + 1
                               */
                      public fun methodTwo(value: Int): Int {
                          return value + 1
                      }
              }
              """,
            """
              /**
               * Align JavaDoc left that starts on 2nd line.
               */
              public class A {
                  /** Align JavaDoc right that starts on 1st line.
                   * @param value test value.
                   * @return value + 1 */
                  public fun methodOne(value: Int) : Int {
                      return value + 1
                  }

                  /** Edge case formatting test.
                   @param value test value.
                   @return value + 1
                   */
                  public fun methodTwo(value: Int): Int {
                      return value + 1
                  }
              }
              """
          )
        );
    }

    @Disabled("Parsing error")
    @Issue("https://github.com/openrewrite/rewrite/issues/709")
    // @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void useContinuationIndentExtendsOnNewLine() {
        rewriteRun(
          kotlin("""
            package org.a

            open class A {}
            """),
          kotlin(
            """
              package org.b;
              import org.a.A;
              class B
                  : A() {
              }
              """
          )
        );
    }

//
    @Issue("https://github.com/openrewrite/rewrite/issues/1526")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void doNotFormatSingleLineCommentAtCol0() {
        rewriteRun(
          kotlin(
            """
              class A {
              // DO NOT shift the whitespace of `Space` and the suffix of comment 1.
              // DOES shift the suffix of comment 2.
              fun shiftRight() {}
              }
              """,
            """
              class A {
              // DO NOT shift the whitespace of `Space` and the suffix of comment 1.
              // DOES shift the suffix of comment 2.
                  fun shiftRight() {}
              }
              """
          )
        );
    }

    @Disabled("Weird alignment")
    @Issue("https://github.com/openrewrite/rewrite/issues/3089")
    // @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void enumConstants() {
        rewriteRun(
          kotlin(
            """
              public enum class WorkflowStatus {
                  @SuppressWarnings("value1")
                  VALUE1,
                  @SuppressWarnings("value2")
                  VALUE2,
                  @SuppressWarnings("value3")
                  VALUE3,
                  @SuppressWarnings("value4")
                  VALUE4
              }
              """
          )
        );
    }
}
