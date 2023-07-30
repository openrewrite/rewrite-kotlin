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
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ControlFlowWithEmptyBody", "ConstantConditionIf", "CatchMayIgnoreException", "KotlinConstantConditions", "SimplifyBooleanWithConstants"})
class SpacesTest implements RewriteTest {

    private static Consumer<RecipeSpec> spaces() {
        return spaces(style -> style);
    }

    private static Consumer<RecipeSpec> spaces(UnaryOperator<SpacesStyle> with) {
        return spec -> spec.recipe(new Spaces())
          .parser(KotlinParser.builder().styles(singletonList(
            new NamedStyles(
              Tree.randomId(), "test", "test", "test", emptySet(),
              singletonList(with.apply(IntelliJ.spaces()))
            )
          )));
    }

    @Nested
    class beforeParensTest {
        @DocumentExample
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

    }
}
