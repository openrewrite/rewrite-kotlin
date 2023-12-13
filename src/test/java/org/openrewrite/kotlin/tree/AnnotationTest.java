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
package org.openrewrite.kotlin.tree;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"RedundantSuppression", "RedundantNullableReturnType", "RedundantVisibilityModifier", "UnusedReceiverParameter", "SortModifiers", "TrailingComma"})
class AnnotationTest implements RewriteTest {

    @Language("kotlin")
    private static final String ANNOTATION =
            """
              @Target(
                  AnnotationTarget.CLASS,
                  AnnotationTarget.ANNOTATION_CLASS,
                  AnnotationTarget.TYPE_PARAMETER,
                  AnnotationTarget.PROPERTY,
                  AnnotationTarget.FIELD,
                  AnnotationTarget.LOCAL_VARIABLE,
                  AnnotationTarget.VALUE_PARAMETER,
                  AnnotationTarget.CONSTRUCTOR,
                  AnnotationTarget.FUNCTION,
                  AnnotationTarget.PROPERTY_GETTER,
                  AnnotationTarget.PROPERTY_SETTER,
                  AnnotationTarget.TYPE,
                  AnnotationTarget.EXPRESSION,
                  AnnotationTarget.FILE,
                  AnnotationTarget.TYPEALIAS
              )
              @Retention(AnnotationRetention.SOURCE)
              annotation class Ann
              """;

    @Test
    void fileScope() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpath()),
          kotlin(
            """
              @file : Suppress  (   "DEPRECATION_ERROR" )

              class A
              """
          )
        );
    }

    @Test
    void multipleFileScope() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              @file : Ann
              @file : Ann
              @file : Ann
              """
          )
        );
    }

    @Test
    void annotationOnEnumEntry() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpath()),
          kotlin(ANNOTATION),
          kotlin(
            """
              enum class EnumTypeA {
                  @Ann
                  FOO
              }
              """
          )
        );
    }

    @Test
    void annotationWithDefaultArgument() {
        rewriteRun(
          kotlin(
            """
              @SuppressWarnings ( "ConstantConditions" , "unchecked" )
              class A
              """
          )
        );
    }

    @Test
    void leadingAnnotations() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno
              annotation class Anno2
              class Test {
                  @Anno
                  @Anno2
                  val id: String = "1"
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.VariableDeclarations v = (J.VariableDeclarations) ((J.ClassDeclaration) cu.getStatements().get(2)).getBody().getStatements().get(0);
                assertThat(v.getLeadingAnnotations().size()).isEqualTo(2);
            })
          )
        );
    }

    @Test
    void arrayArgument() {
        rewriteRun(
          kotlin(
            """
              @Target (  AnnotationTarget . LOCAL_VARIABLE   )
              @Retention  ( AnnotationRetention . SOURCE )
              annotation class Test ( val values : Array <  String > )
              """
          ),
          kotlin(
            """
              @Test( values =  [   "a"    ,     "b" ,  "c"   ]    )
              val a = 42
              """
          )
        );
    }

    @Test
    void fullyQualifiedAnnotation() {
        rewriteRun(
          kotlin(
            """
              @java.lang.Deprecated
              class A
              """
          )
        );
    }

    @Test
    void trailingComma() {
        rewriteRun(
          kotlin(
            """
              annotation class Test ( val values :   Array < String > )
              @Test( values = [ "a" , "b" ,  /* trailing comma */ ] )
              val a = 42
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/80")
    @Test
    void jvmNameAnnotation() {
        rewriteRun(
          kotlin(
            """
              import kotlin.jvm.JvmName
              @get : JvmName ( "getCount" )
              val count : Int ?
                  get ( ) = 1
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/156")
    @Test
    void annotationUseSiteTargetAnnotationOnly() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class TestA {
                  @get : Ann
                  @set : Ann
                  var name : String = ""
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/156")
    @Test
    void annotationUseSiteTarget() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class TestA {
                  @get : Ann
                  public
                  @set : Ann
                  var name: String = ""
              }
              """
          ),
          kotlin(
            """
              class TestB {
                  public
                  @get  : Ann
                  @set :  Ann
                  var name : String = ""
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/173")
    @Test
    void constructorParameterWithAnnotation() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Example(
                  @get : Ann
                  val bar : String
                  )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/173")
    @Test
    void getUseSiteOnConstructorParams() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Example ( /**/  /**/ @get : Ann /**/ /**/ @set : Ann /**/ /**/ var foo: String , @get : Ann val bar: String )
              """
          )
        );
    }

    @Test
    void annotationOnExplicitGetter() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Test {
                  public var stringRepresentation : String = ""
                      @Ann
                      // comment
                      get ( ) = field

                      @Ann
                      set ( value ) {
                          field = value
                      }
              }
              """
          )
        );
    }

    @Test
    void paramAnnotation() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Example  (   @param    :     Ann val  quux   :     String )
              """
          )
        );
    }

    @Test
    void fieldAnnotation() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Example ( @field : Ann val foo : String )
              """
          )
        );
    }

    @Test
    void receiverAnnotationUseSiteTarget() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              fun @receiver  :   Ann    String     . myExtension  (   )    {      }
              """
          )
        );
    }

    @Test
    void setParamAnnotationUseSiteTarget() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              class Example {
                  @setparam : Ann
                  var name: String = ""
              }
              """
          )
        );
    }

    @Test
    void destructuringVariableDeclaration() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              fun example ( ) {
                val (  @Ann   a , @Ann b , @Ann c ) = Triple ( 1 , 2 , 3 )
              }
              """
          )
        );
    }

    @Test
    void annotationsInManyLocations() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              @Ann
              open class Test < @Ann in Number > ( @Ann val s : String ) {
                  @Ann var n : Int = 42
                      @Ann get ( ) = 42
                      @Ann set ( @Ann value ) {
                          field = value
                      }
              
                  @Ann inline fun < @Ann reified T > m ( @Ann s : @Ann String ) : String {
                      @Ann return (@Ann s)
                  }
              }
              @Ann typealias Other =   @Ann  String
              """
          )
        );
    }

    @Test
    void lastAnnotations() {
        rewriteRun(
          kotlin(
            """
              annotation class A
              annotation class B
              annotation class C
              annotation class LAST

              @A final  @B   internal    @C @LAST  class Foo
              """,
            spec -> spec.afterRecipe(cu -> {
                J.ClassDeclaration last = (J.ClassDeclaration) cu.getStatements().get(cu.getStatements().size() - 1);
                List<J.Annotation> annotationList = last.getPadding().getKind().getAnnotations();
                assertThat(annotationList.size()).isEqualTo(2);
                assertThat(annotationList.get(0).getSimpleName()).isEqualTo("C");
                assertThat(annotationList.get(1).getSimpleName()).isEqualTo("LAST");
            })
          )
        );
    }

    @Test
    void lambdaExpression() {
        rewriteRun(
          kotlin(ANNOTATION),
          kotlin(
            """
              fun method ( ) {
                  val list = listOf ( 1 , 2 , 3 )
                  list  .  filterIndexed { index  ,   _    -> @Ann  index   %    2 == 0 }
              }
              """
            )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/267")
    void expressionAnnotationInsideLambda() {
        rewriteRun(
          kotlin(
            """
              val s = java.util.function.Supplier<String> {
                  @Suppress("UNCHECKED_CAST")
                  requireNotNull("x")
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/284")
    @Test
    void annotationWithEmptyArguments() {
        rewriteRun(
          kotlin(
            """
              annotation class Ann

              @Suppress( )
              @Ann
              class A
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "@A @B object C",
      "@A internal @B object C"
    })
    void objectDeclaration(String input) {
        rewriteRun(
          kotlin(
            """
              annotation class A
              annotation class B

              %s
              """.formatted(input)
          )
        );
    }

    @Test
    void annotationEntryTrailingComma() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpath("jackson-annotations")),
          kotlin(
            """
              package org.openrewrite.kotlin

              import com.fasterxml.jackson.`annotation`.JsonTypeInfo
              import kotlin.Suppress
              import kotlin.collections.List
              import kotlin.jvm.JvmName

              @JsonTypeInfo(
                  use = JsonTypeInfo.Id.NAME,
                  include = JsonTypeInfo.As.PROPERTY,
                  property =  "__typename"   , // Trailing comma HERE
              )
              public sealed interface Fruit {
                  @Suppress("INAPPLICABLE_JVM_NAME")
                  @get :  JvmName("getSeeds")
                  public val seeds: List<Int?>?
              }
              """
          )
        );
    }

    @Test
    void collectionLiteralExpression() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpath("jackson-annotations")),
          kotlin(
            """
              import com.fasterxml.jackson.`annotation`.JsonSubTypes

              @JsonSubTypes(value = [
                JsonSubTypes .  Type(value = Employee::class, name = "Employee")
              ])
              public sealed interface Person
              class Employee
              """
          )
        );
    }

    @Test
    void commentBeforeGetter() {
        rewriteRun(
          kotlin(
            """
              public class Movie(
                  title: () -> String? = { throw IllegalStateException("Field `title` was not requested") }
              ) {
                  private val _title: () -> String? = title

                  /**
                   * The original, non localized title with some specials characters : %!({[*$,.:;.
                   */
                  @get:JvmName("getTitle")
                  public val title: String?
                      get() = _title.invoke()
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/377")
    @Test
    void annotatedTypeParameter() {
        rewriteRun(
          kotlin(
            """
              val releaseDates: List< /*C0*/ @Suppress  /*C1*/ String> = emptyList()
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/398")
    @Test
    void annotatedFunction() {
        rewriteRun(
          kotlin(
            """
              annotation class Ann
              class Foo(
                private val option:  @Ann   () -> Unit
              )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/408")
    @ParameterizedTest
    @ValueSource(strings = {
      "String",
      "Map<*, *>"
    })
    void annotatedTypeParameter(String input) {
        rewriteRun(
          kotlin(
            """
              @Target(AnnotationTarget.TYPE)
              annotation class Ann
              class Test(
                val map: Map< @Ann  %s   ,    @Ann %s  > = emptyMap()
              )
              """.formatted(input, input)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/397")
    @Test
    void fieldUseSiteWithMultipleAnnotations() {
        rewriteRun(
          kotlin(
            """
              import javax.inject.Inject
              import javax.inject.Named

              class Test {
                  @field :  [   Inject    Named (  "numberfield "   )    ]
                  var field: Long = 0
              }
              """
          )
        );
    }

    @Test
    void emptyNameUseSiteAnnotation() {
        rewriteRun(
          kotlin(
            """
              annotation class Anno1
              annotation class Anno2

              @[ Anno1 Anno2 ]
              val x = 42
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/397")
    @Test
    void fieldUseSiteWithSingleAnnotationInBracket() {
        rewriteRun(
          kotlin(
            """
              import javax.inject.Inject

              annotation class Anno
              class A {
                  @field: [ Inject ]
                  var field: Long = 0
              }
              """
          )
        );
    }

    @Test
    void fieldUseSiteWithSingleAnnotationImplicitBracket() {
        rewriteRun(
          kotlin(
            """
              import javax.inject.Inject

              annotation class Anno
              class A {
                  @field :  Inject
                  var field: Long = 0
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/453")
    @Test
    void arrayOfCallWithInAnnotation() {
        rewriteRun(
          kotlin(
            """
              annotation class Ann(
                  val test: Test
              )
              annotation class Test ( val arg: Array<String> )
              @Ann(test = Test(
                  arg = arrayOf("")
              ))
              fun use() {
              }
              """
          )
        );
    }

    @Test
    void annotatedIntersectionType() {
        rewriteRun(
          kotlin(
            """
              import java.util.*

              @Target(AnnotationTarget.TYPE)
              annotation class Anno

              fun < T : Any ? > test( n : Optional < @Anno  T   &   @Anno Any > = Optional.empty < T > ( ) ) { }
              """
          )
        );
    }
}
