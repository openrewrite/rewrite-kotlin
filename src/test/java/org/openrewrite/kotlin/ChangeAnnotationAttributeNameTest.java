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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.java.ChangeAnnotationAttributeName;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;

class ChangeAnnotationAttributeNameTest implements RewriteTest {

    @ExpectedToFail
    @Test
    public void runKotlin() {
        rewriteRun(
          spec -> spec.recipe(new ChangeAnnotationAttributeName(
              "org.junit.jupiter.api.Tag",
              "value",
              "newValue"
            ))
            .parser(KotlinParser.builder().classpath("junit-jupiter-api")),
          kotlin(
            """
              package sample
               
              import org.junit.jupiter.api.Tag
              import org.junit.jupiter.api.Tags
               
              class SampleTest {
               
                  @Tags(
                      value = [
                          Tag(value = "Sample01"),
                          Tag(value = "Sample02"),
                      ]
                  )
                  fun run() {
                  }
              }
              """,
            """
              package sample
               
              import org.junit.jupiter.api.Tag
               import org.junit.jupiter.api.Tags
               
              class SampleTest {
               
                  @Tags(
                      value = [
                          Tag(newValue = "Sample01"),
                          Tag(newValue = "Sample02"),
                      ]
                  )
                  fun run() {
                  }
              }
              """
          )
        );
    }

    // working well
    @Test
    public void runJava() {
        rewriteRun(
          spec -> spec.recipe(new ChangeAnnotationAttributeName(
              "org.junit.jupiter.api.Tag",
              "value",
              "newValue"
            ))
            .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api")),
          java(
            """
                    package sample;
                                                    
                    import org.junit.jupiter.api.Tag;
                    import org.junit.jupiter.api.Tags;
                                                    
                    public class SampleJavaTest {
                                                    
                        @Tags(value = {@Tag(value = "Sample"), @Tag(value = "Sample03")})
                        public void runTest() {
                        }
                    }
                    """,
            """
                    package sample;
                                                    
                    import org.junit.jupiter.api.Tag;
                    import org.junit.jupiter.api.Tags;
                                                    
                    public class SampleJavaTest {
                                                    
                        @Tags(value = {@Tag(newValue = "Sample"), @Tag(newValue = "Sample03")})
                        public void runTest() {
                        }
                    }
                    """
          )
        );
    }
}
