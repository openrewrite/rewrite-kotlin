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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class AnnotationTest implements RewriteTest {

    @Test
    void fileScope() {
        rewriteRun(
          kotlin(
            """
              @file : Suppress ( "DEPRECATION_ERROR" , "RedundantUnitReturnType" )

              class A
              """
          )
        );
    }

    @Test
    void multipleFileScope() {
        rewriteRun(
          kotlin(
            """
              @file : A ( "1" )
              @file : A ( "2" )
              @file : A ( "3" )

              @Repeatable
              annotation class A ( val s : String )
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
    void arrayArgument() {
        rewriteRun(
          kotlin(
            """
              @Target ( AnnotationTarget . LOCAL_VARIABLE )
              @Retention ( AnnotationRetention . SOURCE )
              annotation class Test ( val values : Array < String > ) {
              }
              """
          ),
          kotlin(
            """
              @Test( values = [ "a" , "b" , "c" ] )
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
    void annotationUseSiteTarget() {
        rewriteRun(
          kotlin(
            """
              annotation class Ann
              class Test {
                  @set : Ann
                  @get : Ann
                  var name: String = ""
              }
              """
          )
        );
    }

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/156")
    @Test
    void multipleAnnotationUseSite() {
        rewriteRun(
          kotlin(
            """
              import org.simpleframework.xml.Attribute
              import org.simpleframework.xml.Namespace
              import org.simpleframework.xml.Root

              class LibraryPom {
                  @set:Attribute(name = "schemaLocation", required = false)
                  @get:Attribute(name = "schemaLocation", required = false)
                  @Namespace(reference = "http://www.w3.org/2001/XMLSchema-instance", prefix = "xsi")
                  var mSchemaLocation: String = ""
              }
              """
          )
        );
    }
}
