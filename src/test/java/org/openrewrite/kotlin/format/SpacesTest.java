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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.style.IntelliJ;
import org.openrewrite.kotlin.style.SpacesStyle;
import org.openrewrite.kotlin.tree.KSpace;
import org.openrewrite.marker.Markers;
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
class SpacesTest implements RewriteTest {

    private static Consumer<RecipeSpec> spaces() {
        return spaces(style -> style);
    }

    private static Consumer<RecipeSpec> spaces(UnaryOperator<SpacesStyle> with) {
        return spec -> spec.recipe(toRecipe(SpacesFromCompilationUnitStyle::new))
          .parser(KotlinParser.builder().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              singletonList(with.apply(IntelliJ.spaces()))
            )
          )));
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void spaceAfterAsKeyword() {
        rewriteRun(
          spaces(),
          kotlin(
            """
              fun parseValue(input: Any) {
                  val split = (input as String).split("-")
              }
              """
          )
        );
    }

    @Nested
    class beforeParensTest {
        @DocumentExample
        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensMethodDeclaration() {
            rewriteRun(
              spaces(),
              kotlin(
                """
                  fun method1 ( ) {
                  }
                  fun method2	( ) {
                  }
                  fun method3() {
                  }
                  """,
                """
                  fun method1() {
                  }
                  fun method2() {
                  }
                  fun method3() {
                  }
                  """
              )
            );
        }

        @SuppressWarnings("TrailingWhitespacesInTextBlock")
        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensMethodDeclarationFalseWithLineBreakIgnored() {
            rewriteRun(
              spaces(),
              kotlin(
                """
                  fun method 
                  () {
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensMethodDeclarationWithComment() {
            rewriteRun(
              spaces(),
              kotlin(
                """
                  fun method   /* C */  () {
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeClassBody() {
            rewriteRun(
              spaces(),
              kotlin(
                """
                  class Test{
                      val s: String = ""
                  }
                  """,
                """
                  class Test {
                      val s: String = ""
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensMethodCall() {
            rewriteRun(
              spaces(),
              kotlin(
                """
                  fun foo() {
                      foo ()
                      val test = Test ()
                  }
                  """,
                """
                  fun foo() {
                      foo()
                      val test = Test()
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensIfParenthesesFalse() {
            rewriteRun(
              spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withIfParentheses(false))),
              kotlin(
                """
                  fun foo() {
                      if (true) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      if(true) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensIfParenthesesTrue() {
            rewriteRun(
              spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withIfParentheses(true))),
              kotlin(
                """
                  fun foo() {
                      if(true) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      if (true) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensForParenthesesFalse() {
            rewriteRun(
              spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withForParentheses(false))),
              kotlin(
                """
                  fun foo() {
                      for (i in 1..10) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      for(i in 1..10) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensForParenthesesTrue() {
            rewriteRun(
              spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withForParentheses(true))),
              kotlin(
                """
                  fun foo() {
                      for(i in 1..10) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      for (i in 1..10) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensWhileParenthesesFalse() {
            rewriteRun(
              spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withWhileParentheses(false))),
              kotlin(
                """
                  fun foo() {
                      while (true) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      while(true) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensWhileParenthesesTrue() {
            rewriteRun(
              spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withWhileParentheses(true))),
              kotlin(
                """
                  fun foo() {
                      while(true) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      while (true) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensCatchParenthesesFalse() {
            rewriteRun(
              spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withCatchParentheses(false))),
              kotlin(
                """
                  fun foo() {
                      try {
                      } catch (e: Exception) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      try {
                      } catch(e: Exception) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensCatchParenthesesTrue() {
            rewriteRun(
              spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withCatchParentheses(true))),
              kotlin(
                """
                  fun foo() {
                      try {
                      } catch(e: Exception) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      try {
                      } catch (e: Exception) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensAnnotationParameters() {
            rewriteRun(
              spaces(),
              kotlin(
                """
                  annotation class Ann(val s: String)
                  @Ann ("")
                  class Test
                  """,
                """
                  annotation class Ann(val s: String)
                  @Ann("")
                  class Test
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensWhenParenthesesTrue() {
            rewriteRun(
              spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withWhenParentheses(true))),
              kotlin(
                """
                  fun foo() {
                      when(42) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      when (42) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void beforeParensWhenParenthesesFalse() {
            rewriteRun(
              spaces(style -> style.withBeforeParentheses(style.getBeforeParentheses().withWhenParentheses(false))),
              kotlin(
                """
                  fun foo() {
                      when   (42) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      when(42) {
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class aroundOperatorsTest {
        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsAssignmentFalse() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withAssignment(false))),
              kotlin(
                """
                  fun method() {
                      var x = 0
                      x += 1
                  }
                  """,
                """
                  fun method() {
                      var x=0
                      x+=1
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsAssignmentTrue() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withAssignment(true))),
              kotlin(
                """
                  fun method() {
                      var x=0
                      x+=1
                  }
                  """,
                """
                  fun method() {
                      var x = 0
                      x += 1
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsLogicalFalse() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withLogical(false))),
              kotlin(
                """
                  fun foo() {
                      val x = true && false
                      val y = true || false
                  }
                  """,
                """
                  fun foo() {
                      val x = true&&false
                      val y = true||false
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsLogicalTrue() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withLogical(true))),
              kotlin(
                """
                  fun foo() {
                      val x = true&&false
                      val y = true||false
                  }
                  """,
                """
                  fun foo() {
                      val x = true && false
                      val y = true || false
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsEqualityFalse() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withEquality(false))),
              kotlin(
                """
                  fun foo() {
                      val x = 0 == 1
                      val y = 0 != 1
                      val a = 0 === 1
                      val b = 0 !== 1
                  }
                  """,
                """
                  fun foo() {
                      val x = 0==1
                      val y = 0!=1
                      val a = 0===1
                      val b = 0!==1
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsEqualityTrue() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withEquality(true))),
              kotlin(
                """
                  fun foo() {
                      val x = 0==1
                      val y = 0!=1
                      val a = 0===1
                      val b = 0!==1
                  }
                  """,
                """
                  fun foo() {
                      val x = 0 == 1
                      val y = 0 != 1
                      val a = 0 === 1
                      val b = 0 !== 1
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsRelationalFalse() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withRelational(false))),
              kotlin(
                """
                  fun foo() {
                      val a = 0 < 1
                      val b = 0 <= 1
                      val c = 0 >= 1
                      val d = 0 >= 1
                  }
                  """,
                """
                  fun foo() {
                      val a = 0<1
                      val b = 0<=1
                      val c = 0>=1
                      val d = 0>=1
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsRelationalTrue() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withRelational(true))),
              kotlin(
                """
                  fun foo() {
                      val a = 0<1
                      val b = 0<=1
                      val c = 0>=1
                      val d = 0>=1
                  }
                  """,
                """
                  fun foo() {
                      val a = 0 < 1
                      val b = 0 <= 1
                      val c = 0 >= 1
                      val d = 0 >= 1
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsBitwise() {
            rewriteRun(
              spaces(),
              kotlin(
                """
                  fun foo() {
                      val a = 1  and  2
                      val b = 1  or  2
                      val c = 1  xor  2
                      val d = a  shr  1
                      val e = a  shl  1
                      val f = 1   ushr   2
                  }
                  """,
                """
                  fun foo() {
                      val a = 1 and 2
                      val b = 1 or 2
                      val c = 1 xor 2
                      val d = a shr 1
                      val e = a shl 1
                      val f = 1 ushr 2
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsAdditiveFalse() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withAdditive(false))),
              kotlin(
                """
                  fun foo() {
                      val x = 1 + 2
                      val y = 1 - 2
                  }
                  """,
                """
                  fun foo() {
                      val x = 1+2
                      val y = 1-2
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsAdditiveTrue() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withAdditive(true))),
              kotlin(
                """
                  fun foo() {
                      val x = 1+2
                      val y = 1-2
                  }
                  """,
                """
                  fun foo() {
                      val x = 1 + 2
                      val y = 1 - 2
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsMultiplicativeFalse() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withMultiplicative(false))),
              kotlin(
                """
                  fun foo() {
                      val a = 1 * 2
                      val b = 1 / 2
                      val c = 1 % 2
                  }
                  """,
                """
                  fun foo() {
                      val a = 1*2
                      val b = 1/2
                      val c = 1%2
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsMultiplicativeTrue() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withMultiplicative(true))),
              kotlin(
                """
                  fun foo() {
                      val a = 1*2
                      val b = 1/2
                      val c = 1%2
                  }
                  """,
                """
                  fun foo() {
                      val a = 1 * 2
                      val b = 1 / 2
                      val c = 1 % 2
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsUnaryFalse() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withUnary(false))),
              kotlin(
                """
                  fun foo() {
                      var x = 0
                      x ++
                      x --
                      -- x
                      ++ x
                      x = - x
                      x = + x
                      var y = false
                      y = ! y
                  }
                  """,
                """
                  fun foo() {
                      var x = 0
                      x++
                      x--
                      --x
                      ++x
                      x = -x
                      x = +x
                      var y = false
                      y = !y
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsUnaryTrue() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withUnary(true))),
              kotlin(
                """
                  fun foo() {
                      var x = 0
                      x++
                      x--
                      --x
                      ++x
                      x = -x
                      x = +x
                      var y = false
                      y = !y
                  }
                  """,
                """
                  fun foo() {
                      var x = 0
                      x ++
                      x --
                      -- x
                      ++ x
                      x = - x
                      x = + x
                      var y = false
                      y = ! y
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsRangeOperatorsFalse() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withRange(false))),
              kotlin(
                """
                  fun foo() {
                      var r = 1 .. 5
                      for (i in 10 .. 42) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      var r = 1..5
                      for (i in 10..42) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsRangeOperatorsTrue() {
            rewriteRun(
              spaces(style -> style.withAroundOperators(style.getAroundOperators().withRange(true))),
              kotlin(
                """
                  fun foo() {
                      var r = 1..5
                      for (i in 10..42) {
                      }
                  }
                  """,
                """
                  fun foo() {
                      var r = 1 .. 5
                      for (i in 10 .. 42) {
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsLambda() {
            rewriteRun(
              spaces(),
              kotlin(
                """
                  class Test {
                      fun foo() {
                          val r: () -> Unit   =   {}
                      }
                  }
                  """,
                """
                  class Test {
                      fun foo() {
                          val r: () -> Unit = {}
                      }
                  }
                  """
              )
            );
        }

        @Disabled("FIXME, to be supported by PSI parser")
    @Test
        void aroundOperatorsMethodReferenceDoubleColon() {
            rewriteRun(
              spaces(),
              kotlin(
                """
                  class Test {
                      fun foo() {
                          val r1: () -> Unit = this   ::   foo
                      }
                  }
                  """,
                """
                  class Test {
                      fun foo() {
                          val r1: () -> Unit = this::foo
                      }
                  }
                  """
              )
            );
        }
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/192")
    @SuppressWarnings("RedundantNullableReturnType")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void visitsMarkerLocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
              @Override
              public Space visitSpace(Space space, KSpace.Location loc, ExecutionContext executionContext) {
                  if (!space.getComments().isEmpty()) {
                      return space;
                  }
                  if (loc == KSpace.Location.TYPE_REFERENCE_PREFIX || loc == KSpace.Location.IS_NULLABLE_PREFIX || loc == KSpace.Location.CHECK_NOT_NULL_PREFIX) {
                      return space.withComments(ListUtils.concat(new TextComment(true, loc.name(), "", Markers.EMPTY), space.getComments()));
                  }
                  return super.visitSpace(space, loc, executionContext);
              }
          })),
          kotlin(
            """
              class A {
                  fun method ( ) : Int ? {
                      return 1
                  }
              }
              """,
            """
              class A {
                  fun method ( ) /*TYPE_REFERENCE_PREFIX*/: Int /*IS_NULLABLE_PREFIX*/? {
                      return 1
                  }
              }
              """
          ),
          kotlin(
            """
              val a = A ( )
              val b = a . method ( ) !!
              val c = b !!
              """,
            """
              val a = A ( )
              val b = a . method ( ) /*CHECK_NOT_NULL_PREFIX*/!!
              val c = b /*CHECK_NOT_NULL_PREFIX*/!!
              """
          )
        );
    }

    @Nested
    class OtherTest {
        // Space before/after comma
        // In Kotlin, comma ',' can appear in the below locations
        // 1. Method parameters
        // 2. Array
        // 3. Destructuring Declaration
        // FIXME. add more

        @Nested
        class otherBeforeComma {
            // 1. Method parameters
            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeCommaFalseMethodParameters() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeComma(false))),
                  kotlin(
                    """
                      fun method(
                              foo: String ,
                              bar: String   ,
                              baz: String
                      ) {
                      }
                      """,
                    """
                      fun method(
                              foo: String,
                              bar: String,
                              baz: String
                      ) {
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeCommaTrueMethodParameters() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeComma(true))),
                  kotlin(
                    """
                      fun method(
                              foo: String,
                              bar: String,
                              baz: String
                      ) {
                      }
                      """,
                    """
                      fun method(
                              foo: String ,
                              bar: String ,
                              baz: String
                      ) {
                      }
                      """
                  )
                );
            }

            // 2. Array
            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeCommaFalseArray() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeComma(false))),
                  kotlin(
                    """
                      val numbers = arrayOf(1 , 2  , 3   , 4)
                      val list = listOf("apple" , "banana"   , "orange")
                      """,
                    """
                      val numbers = arrayOf(1, 2, 3, 4)
                      val list = listOf("apple", "banana", "orange")
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeCommaTrueArray() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeComma(true))),
                  kotlin(
                    """
                      val numbers = arrayOf(1, 2, 3, 4)
                      val list = listOf("apple", "banana", "orange")
                      """,
                    """
                      val numbers = arrayOf(1 , 2 , 3 , 4)
                      val list = listOf("apple" , "banana" , "orange")
                      """
                  )
                );
            }

            // 3. Destructuring Declaration
            @ExpectedToFail("destruct type")
            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeCommaFalseDestruct() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeComma(false))),
                  kotlin(
                    """
                      data class Person(val name: String , val age: Int)

                      fun method() {
                          val (name, age) = Person("John" , 30)
                      }
                      """,
                    """
                      data class Person(val name: String, val age: Int)

                      fun method() {
                          val (name, age) = Person("John", 30)
                      }
                      """
                  )
                );
            }

            @ExpectedToFail("destruct type")
            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeCommaTrueDestruct() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeComma(true))),
                  kotlin(
                    """
                      data class Person(val name: String, val age: Int)

                      fun method() {
                          val (name, age) = Person("John", 30)
                      }
                      """,
                    """
                      data class Person(val name: String , val age: Int)

                      fun method() {
                          val (name, age) = Person("John" , 30)
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class otherAfterComma {
            // 1. Method parameters
            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterCommaTrueMethodParameters() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterComma(true))),
                  kotlin(
                    """
                      fun method(foo: String,bar: String,baz: String) {
                      }
                      """,
                    """
                      fun method(foo: String, bar: String, baz: String) {
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterCommaFalseMethodParameters() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterComma(false))),
                  kotlin(
                    """
                      fun method(foo: String, bar: String,   baz: String) {
                      }
                      """,
                    """
                      fun method(foo: String,bar: String,baz: String) {
                      }
                      """
                  )
                );
            }

            // 2. Array
            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterCommaTrueArray() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterComma(true))),
                  kotlin(
                    """
                      val numbers = arrayOf(1,2,3,4)
                      val list = listOf("apple","banana","orange")
                      """,
                    """
                      val numbers = arrayOf(1, 2, 3, 4)
                      val list = listOf("apple", "banana", "orange")
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterCommaFalseArray() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterComma(false))),
                  kotlin(
                    """
                      val numbers = arrayOf(1, 2,  3,   4)
                      val list = listOf("apple", "banana",   "orange")
                      """,
                    """
                      val numbers = arrayOf(1,2,3,4)
                      val list = listOf("apple","banana","orange")
                      """
                  )
                );
            }

            // 3. Destructuring Declaration
            @ExpectedToFail("name, expect Person{name=component1,return=kotlin.String,parameters=[]} but {undefined}{name=name,type=kotlin.String}")
            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterCommaTrueDestruct() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterComma(true))),
                  kotlin(
                    """
                      data class Person(val name: String,val age: Int)

                      fun method() {
                          val (name, age) = Person("John",30)
                      }
                      """,
                    """
                      data class Person(val name: String, val age: Int)

                      fun method() {
                          val (name, age) = Person("John", 30)
                      }
                      """
                  )
                );
            }

            @ExpectedToFail("name, expect Person{name=component1,return=kotlin.String,parameters=[]} but {undefined}{name=name,type=kotlin.String}")
            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterCommaFalseDestruct() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterComma(false))),
                  kotlin(
                    """
                      data class Person(val name: String, val age: Int)

                      fun method() {
                          val (name, age) = Person("John",   30)
                      }
                      """,
                    """
                      data class Person(val name: String,val age: Int)

                      fun method() {
                          val (name, age) = Person("John",30)
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class otherBeforeColonAfterDeclarationName {
            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonAfterDeclarationNameFalseVariableDeclaration() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonAfterDeclarationName(false))),
                  kotlin(
                    """
                      class some {
                          private val f   : (Int) -> Int = { a : Int -> a * 2 }
                          val test : Int = 12
                      }
                      """,
                    """
                      class some {
                          private val f: (Int) -> Int = { a: Int -> a * 2 }
                          val test: Int = 12
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonAfterDeclarationNameTrueVariableDeclaration() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonAfterDeclarationName(true))),
                  kotlin(
                    """
                      class some {
                          private val f: (Int) -> Int = { a: Int -> a * 2 }
                          val test: Int = 12
                      }
                      """,
                    """
                      class some {
                          private val f : (Int) -> Int = { a : Int -> a * 2 }
                          val test : Int = 12
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonAfterDeclarationNameFalseFunctionTypeParameter() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonAfterDeclarationName(false))),
                  kotlin(
                    """
                      val ft : (a  :  Int) -> Int = { 2 }
                      """,
                    """
                      val ft: (a: Int) -> Int = { 2 }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonAfterDeclarationNameTrueFunctionTypeParameter() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonAfterDeclarationName(true))),
                  kotlin(
                    """
                      val ft : (a  :  Int) -> Int = { 2 }
                      """,
                    """
                      val ft : (a : Int) -> Int = { 2 }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonAfterDeclarationNameFalseMethodDeclaration() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonAfterDeclarationName(false))),
                  kotlin(
                    """
                      fun foo()   : Int {
                          return 1
                      }
                      """,
                    """
                      fun foo(): Int {
                          return 1
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonAfterDeclarationNameTrueMethodDeclaration() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonAfterDeclarationName(true))),
                  kotlin(
                    """
                      fun foo(): Int {
                          return 1
                      }
                      """,
                    """
                      fun foo() : Int {
                          return 1
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonAfterDeclarationNameFalseTryCatch() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonAfterDeclarationName(false))),
                  kotlin(
                    """
                      fun foo() {
                          try {
                          } catch (e   : Exception) {
                          }
                      }
                      """,
                    """
                      fun foo() {
                          try {
                          } catch (e: Exception) {
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonAfterDeclarationNameTrueTryCatch() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonAfterDeclarationName(true))),
                  kotlin(
                    """
                      fun foo() {
                          try {
                          } catch (e: Exception) {
                          }
                      }
                      """,
                    """
                      fun foo() {
                          try {
                          } catch (e : Exception) {
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonAfterDeclarationNameFalseMethodDeclarationParameters() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonAfterDeclarationName(false))),
                  kotlin(
                    """
                      fun method(
                              foo : String,
                              bar   : String) {
                      }
                      """,
                    """
                      fun method(
                              foo: String,
                              bar: String) {
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonAfterDeclarationNameTrueMethodDeclarationParameters() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonAfterDeclarationName(true))),
                  kotlin(
                    """
                      fun method(
                              foo: String,
                              bar: String) {
                      }
                      """,
                    """
                      fun method(
                              foo : String,
                              bar : String) {
                      }
                      """
                  )
                );
            }

        }

        @Nested
        class otherAfterColonBeforeDeclarationType {
            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterColonBeforeDeclarationTypeTrueVariableDeclaration() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterColonBeforeDeclarationType(true))),
                  kotlin(
                    """
                      class some {
                          private val f:(Int) -> Int = { a:Int -> a * 2 }
                          val test:   Int = 12
                      }
                      """,
                    """
                      class some {
                          private val f: (Int) -> Int = { a: Int -> a * 2 }
                          val test: Int = 12
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterColonBeforeDeclarationTypeFalseVariableDeclaration() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterColonBeforeDeclarationType(false))),
                  kotlin(
                    """
                      class some {
                          private val f: (Int) -> Int = { a:   Int -> a * 2 }
                          val test:   Int = 12
                      }
                      """,
                    """
                      class some {
                          private val f:(Int) -> Int = { a:Int -> a * 2 }
                          val test:Int = 12
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterColonBeforeDeclarationTypeTrueMethodDeclaration() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterColonBeforeDeclarationType(true))),
                  kotlin(
                    """
                      fun foo():Int {
                          return 1
                      }
                      """,
                    """
                      fun foo(): Int {
                          return 1
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterColonBeforeDeclarationTypeFalseMethodDeclaration() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterColonBeforeDeclarationType(false))),
                  kotlin(
                    """
                      fun foo():   Int {
                          return 1
                      }
                      """,
                    """
                      fun foo():Int {
                          return 1
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterColonBeforeDeclarationTypeTrueTryCatch() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterColonBeforeDeclarationType(true))),
                  kotlin(
                    """
                      fun foo() {
                          try {
                          } catch (e:Exception) {
                          }
                      }
                      """,
                    """
                      fun foo() {
                          try {
                          } catch (e: Exception) {
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterColonBeforeDeclarationTypeFalseTryCatch() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterColonBeforeDeclarationType(false))),
                  kotlin(
                    """
                      fun foo() {
                          try {
                          } catch (e:   Exception) {
                          }
                      }
                      """,
                    """
                      fun foo() {
                          try {
                          } catch (e:Exception) {
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterColonBeforeDeclarationTypeTrueMethodDeclarationParameters() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterColonBeforeDeclarationType(true))),
                  kotlin(
                    """
                      fun method(
                              foo:String,
                              bar:String) {
                      }
                      """,
                    """
                      fun method(
                              foo: String,
                              bar: String) {
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterColonBeforeDeclarationTypeFalseMethodDeclarationParameters() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAfterColonBeforeDeclarationType(false))),
                  kotlin(
                    """
                      fun method(
                              foo: String,
                              bar:   String) {
                      }
                      """,
                    """
                      fun method(
                              foo:String,
                              bar:String) {
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class otherBeforeColonInNewTypeDefinition {

            @Disabled("FIXME after parsing error fixed https://github.com/openrewrite/rewrite-kotlin/issues/205")
            // @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonInNewTypeDefinitionTrue() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonInNewTypeDefinition(true))),
                  kotlin(
                    """
                      fun <T> foo(): Int where T: List<T> {
                          return 0
                      }
                      """,
                    """
                      fun <T> foo(): Int where T : List<T> {
                          return 0
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME after parsing error fixed https://github.com/openrewrite/rewrite-kotlin/issues/205")
            // @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeColonInNewTypeDefinitionFalse() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonInNewTypeDefinition(false))),
                  kotlin(
                    """
                      fun <T> foo(): Int where T : List<T> {
                          return 0
                      }
                      """,
                    """
                      fun <T> foo(): Int where T: List<T> {
                          return 0
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class otherAfterColonInNewTypeDefinition {

            @Disabled("FIXME after parsing error fixed https://github.com/openrewrite/rewrite-kotlin/issues/205")
            // @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterColonInNewTypeDefinitionTrue() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonInNewTypeDefinition(true))),
                  kotlin(
                    """
                      fun <T> foo(): Int where T :List<T> {
                          return 0
                      }
                      """,
                    """
                      fun <T> foo(): Int where T : List<T> {
                          return 0
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME after parsing error fixed https://github.com/openrewrite/rewrite-kotlin/issues/205")
            // @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAfterColonInNewTypeDefinitionFalse() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeColonInNewTypeDefinition(false))),
                  kotlin(
                    """
                      fun <T> foo(): Int where T : List<T> {
                          return 0
                      }
                      """,
                    """
                      fun <T> foo(): Int where T :List<T> {
                          return 0
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class otherInSimpleOneLineMethods {

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherInSimpleOneLineMethodsTrue() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withInSimpleOneLineMethods(true))),
                  kotlin(
                    """
                      private val f: (Int) -> Int = {a: Int -> a * 2}
                      """,
                    """
                      private val f: (Int) -> Int = { a: Int -> a * 2 }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherInSimpleOneLineMethodsFalse() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withInSimpleOneLineMethods(false))),
                  kotlin(
                    """
                      private val f: (Int) -> Int = {   a: Int -> a * 2   }
                      """,
                    """
                      private val f: (Int) -> Int = {a: Int -> a * 2}
                      """
                  )
                );
            }
        }

        @Nested
        class otherAroundArrowInFunctionType {

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAroundArrowInFunctionTypeTrue() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAroundArrowInFunctionTypes(true)
                    .withBeforeLambdaArrow(false))),
                  kotlin(
                    """
                      private val f: (Int)->Int = { a: Int->a * 2 }
                      """,
                    """
                      private val f: (Int) -> Int = { a: Int->a * 2 }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAroundArrowInFunctionTypeFalse() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAroundArrowInFunctionTypes(false))),
                  kotlin(
                    """
                      private val f: (Int)   ->   Int = { a: Int -> a * 2 }
                      """,
                    """
                      private val f: (Int)->Int = { a: Int -> a * 2 }
                      """
                  )
                );
            }
        }

        @Nested
        class otherAroundArrowInWhenClause {

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAroundArrowInWhenClauseTrueArrowToConstant() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAroundArrowInWhenClause(true))),
                  kotlin(
                    """
                      fun method() {
                          val test: Int = 12
                          var i = 0
                          when {
                              i < test->-1
                              i > test->    1
                              else  ->0
                          }
                      }
                      """,
                    """
                      fun method() {
                          val test: Int = 12
                          var i = 0
                          when {
                              i < test -> -1
                              i > test -> 1
                              else -> 0
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAroundArrowInWhenClauseFalseArrowToConstant() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAroundArrowInWhenClause(false))),
                  kotlin(
                    """
                      fun method() {
                          val test: Int = 12
                          var i = 0
                          when {
                              i < test   ->   -1
                              i > test -> 1
                              else  -> 0
                          }
                      }
                      """,
                    """
                      fun method() {
                          val test: Int = 12
                          var i = 0
                          when {
                              i < test->-1
                              i > test->1
                              else->0
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAroundArrowInWhenClauseTrueArrowToMethodInvocation() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAroundArrowInWhenClause(true))),
                  kotlin(
                    """
                      fun method() {
                          val test: Int = 12

                          when (test) {
                              12->println("foo")
                              in 10..42->    {
                                  println("baz")
                              }
                              else   ->   println("bar")
                          }
                      }
                      """,
                    """
                      fun method() {
                          val test: Int = 12

                          when (test) {
                              12 -> println("foo")
                              in 10..42 -> {
                                  println("baz")
                              }
                              else -> println("bar")
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherAroundArrowInWhenClauseFalseArrowToMethodInvocation() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withAroundArrowInWhenClause(false))),
                  kotlin(
                    """
                      fun method() {
                          val test: Int = 12

                          when (test) {
                              12   ->   println("foo")
                              in 10..42 ->    {
                                  println("baz")
                              }
                              else   ->   println("bar")
                          }
                      }
                      """,
                    """
                      fun method() {
                          val test: Int = 12

                          when (test) {
                              12->println("foo")
                              in 10..42->{
                                  println("baz")
                              }
                              else->println("bar")
                          }
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class otherBeforeLambdaArrow {

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeLambdaArrowTrue() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeLambdaArrow(true))),
                  kotlin(
                    """
                      private val f: (Int) -> Int = { a: Int-> a * 2 }
                      """,
                    """
                      private val f: (Int) -> Int = { a: Int -> a * 2 }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherBeforeLambdaArrowFalse() {
                rewriteRun(
                  spaces(style -> style.withOther(style.getOther().withBeforeLambdaArrow(false))),
                  kotlin(
                    """
                      private val f: (Int) -> Int = { a: Int -> a * 2 }
                      """,
                    """
                      private val f: (Int) -> Int = { a: Int-> a * 2 }
                      """
                  )
                );
            }
        }


        @Nested
        class otherDefaults {

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeParensTryParentheses() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      fun foo() {
                          try{
                          } catch (e: Exception) {
                          }
                      }
                      """,
                    """
                      fun foo() {
                          try {
                          } catch (e: Exception) {
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceClassLeftBrace() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test{
                      }
                      """,
                    """
                      class Test {
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceMethodLeftBrace() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test{
                          fun foo(){
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                          }
                      }
                      """
                  )
                );
            }


            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceIfLeftBraceFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test{
                          fun foo() {
                              if (true){
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              if (true) {
                              }
                          }
                      }
                      """
                  )
                );
            }


            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceElseLeftBraceFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test{
                          fun foo() {
                              if (true) {
                              } else{
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              if (true) {
                              } else {
                              }
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceForLeftBraceFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      fun foo() {
                          for (i in 0..10){
                          }
                      }
                      """,
                    """
                      fun foo() {
                          for (i in 0..10) {
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceWhileLeftBraceFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      fun foo() {
                          while (true != false){
                          }
                      }
                      """,
                    """
                      fun foo() {
                          while (true != false) {
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceDoLeftBraceFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              do{
                              } while (true != false);
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              do {
                              } while (true != false);
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceTryLeftBraceFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              try{
                              } catch (e: Exception) {
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              try {
                              } catch (e: Exception) {
                              }
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceCatchLeftBraceFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              try {
                              } catch (e: Exception){
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              try {
                              } catch (e: Exception) {
                              }
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceFinallyLeftBraceFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              try {
                              } catch (e: Exception) {
                              } finally{
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              try {
                              } catch (e: Exception) {
                              } finally {
                              }
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeLeftBraceAnnotationArrayInitializerLeftBraceTrue() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      annotation class MyAnno(
                          val names: Array<String>,
                          val counts: IntArray
                      )

                      @MyAnno(names =["a","b"], counts = [1,2])
                      class Test {
                      }
                      """,
                    """
                      annotation class MyAnno(
                          val names: Array<String>,
                          val counts: IntArray
                      )

                      @MyAnno(names = ["a","b"], counts = [1,2])
                      class Test {
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeKeywordsElseKeywordTrue() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              if (true) {
                              }else {
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              if (true) {
                              } else {
                              }
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeKeywordsWhileKeywordTrue() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              do {
                              }while (true)
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              do {
                              } while (true)
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeKeywordsCatchKeywordTrue() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              try {
                              }catch (e: Exception) {
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              try {
                              } catch (e: Exception) {
                              }
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void beforeKeywordsFinallyKeywordTrue() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              try {
                              } catch (e: Exception) {
                              }finally {
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              try {
                              } catch (e: Exception) {
                              } finally {
                              }
                          }
                      }
                      """
                  )
                );
            }


            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinCodeBracesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test { }
                      interface ITest { }
                      """,
                    """
                      class Test {}
                      interface ITest {}
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinBracketsFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo(a: IntArray) {
                              var x = a[   0   ]
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo(a: IntArray) {
                              var x = a[0]
                          }
                      }
                      """
                  )
                );
            }


            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinArrayInitializerBracesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              val x = intArrayOf( 1,2,3 )
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              val x = intArrayOf(1, 2, 3)
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinGroupingParenthesesTrue() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo(x: Int) {
                              var y = ( x + 1 )
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo(x: Int) {
                              var y = (x + 1)
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinMethodDeclarationParenthesesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo(    x: Int   ,   y: Int   ) {
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo(x: Int, y: Int) {
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinEmptyMethodDeclarationParenthesesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo( ) {
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinMethodCallParenthesesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun bar(x: Int) {
                          }
                          fun foo() {
                              bar( 1 )
                          }
                      }
                      """,
                    """
                      class Test {
                          fun bar(x: Int) {
                          }
                          fun foo() {
                              bar(1)
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinEmptyMethodCallParenthesesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun bar() {
                          }
                          fun foo() {
                              bar( );
                          }
                      }
                      """,
                    """
                      class Test {
                          fun bar() {
                          }
                          fun foo() {
                              bar();
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinIfParenthesesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              if ( true ) {
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              if (true) {
                              }
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinForParenthesesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              for (  i in 0..10   ) {
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              for (i in 0..10) {
                              }
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void withinWhileParenthesesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              while ( true ) {
                              }
                              do {
                              } while ( true )
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              while (true) {
                              }
                              do {
                              } while (true)
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
            @Test
            void withinCatchParenthesesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test {
                          fun foo() {
                              try {
                              } catch (  e: Exception ) {
                              }
                          }
                      }
                      """,
                    """
                      class Test {
                          fun foo() {
                              try {
                              } catch (e: Exception) {
                              }
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
            @Test
            void withinAngleBracketsFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      import java.util.ArrayList
                       
                      class Test< T, U > {
                          fun < T2 : T > foo(): T2? {
                              val myList: List<T2> = ArrayList()
                              return null
                          }
                      }
                      """,
                    """
                      import java.util.ArrayList

                      class Test <T, U> {
                          fun <T2 : T> foo(): T2? {
                              val myList: List<T2> = ArrayList()
                              return null
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
            @Test
            void typeArgumentsAfterComma() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      import java.util.HashMap

                      class Test {
                          fun foo() {
                              val m: Map<String,String> = HashMap()
                              Test.bar<String,Int>()
                          }

                          companion object {
                              fun <A,B> bar() {
                              }
                          }
                      }
                      """,
                    """
                      import java.util.HashMap

                      class Test {
                          fun foo() {
                              val m: Map<String,String> = HashMap()
                              Test.bar<String, Int>()
                          }

                          companion object {
                              fun <A, B> bar() {
                              }
                          }
                      }
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void otherInsideOneLineEnumBracesFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      enum class Test { }
                      """,
                    """
                      enum class Test {}
                      """
                  )
                );
            }

            @Disabled("FIXME, to be supported by PSI parser")
    @Test
            void typeParametersBeforeOpeningAngleBracketFalse() {
                rewriteRun(
                  spaces(),
                  kotlin(
                    """
                      class Test   <T> {
                      }
                      """,
                    """
                      class Test <T> {
                      }
                      """
                  )
                );
            }
        }
    }
}
