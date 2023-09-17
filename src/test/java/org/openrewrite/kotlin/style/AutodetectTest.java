/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.kotlin.style;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings({"All", "RedundantVisibilityModifier"})
class AutodetectTest implements RewriteTest {
    private static KotlinParser kp() {
        return KotlinParser.builder().build();
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void continuationIndent() {
        var cus = kp().parse(
          """
            class Test {
            	fun eq(): Boolean {
            		return (1 == 1 &&
            				2 == 2 &&
            				3 == 3)
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3552")
    void continuationIndentFromParameters() {
        var cus = kp().parse(
          """
            class Test {
               fun foo(s1: String,
                    s2: String,
                    s3: String) {
               }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(5);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3550")
    void alignParametersWhenMultiple() {
        var cus = kp().parse(
          """
            class Test {
            	fun foo(s1: String,
            	        s2: String,
            	        s3: String) {
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getFunctionDeclarationParameters().getAlignWhenMultiple()).isTrue();
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1221")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void springDemoApp() {
        //language=kotlin
        var cus = kp().parse(
          """
            package com.kmccarpenter.demospring

            import java.util.Collections

            class DemoSpringApplication {
                companion object {
                    @JvmStatic
            		fun main(args: Array<String>) {
            			System.out.print("Hello world")
            		}
                }
            }
            """
        );
        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();

        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void springCloudTabsAndIndents() {
        //language=kotlin
        var cus = kp().parse(
          """
            package org.springframework.cloud.netflix.eureka

            import org.springframework.cloud.netflix.eureka.*

            @SuppressWarnings("ALL")
            class EurekaClientConfigBean {
            	companion object {
            	private const val MINUTES = 60
            	}

            	override fun setOrder(order: Int) {
            	var a = order
            	}

            	override fun equals(o: Any?): Boolean {
            		val that = o as EurekaClientConfigBean
            		return (1 == 1 &&
            				2 == 2 &&
            				3 == 3)
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }

    @SuppressWarnings("InfiniteRecursion")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void spinnakerTabsAndIndents() {
        var cus = kp().parse(
          """
            package com.netflix.kayenta.orca.controllers

            import java.lang.RuntimeException

            @SuppressWarnings("ALL")
            class AdminController {
              var publisher: String = ""

              @Suppress
              fun method(publisher: String) {
                this.publisher = publisher;
              }

              @Suppress(
                  "X")
              fun setInstanceEnabled(enabledWrapper : Map<String, Boolean>) {
                val enabled = enabledWrapper.get("enabled")

                if (enabled == null) {
                  throw RuntimeException("The field 'enabled' must be set.", null);
                }

                setInstanceEnabled(enabledWrapper);
              }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(2);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(4);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void rewriteTabsAndIndents() {
        var cus = kp().parse(
          """
            class Autodetect {
                @Override
                fun visitIdentifier(ident: Int, ctx: String): String {
                    var i = visitIdentifier(ident, ctx)

                    if (true
                            && true)) {
                        i = visitIdentifier(ident, ctx)
                                .visitIdentifier(ident, ctx)
                    }

                    return i
                }

            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(8);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void defaultTabIndentSizeToOne() {
        var cus = kp().parse(
          """
            /**
             *
             */
            public class Test {
            	val publisher: String = "A"
            	fun method() {
            		var value = 0;
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize4() {
        var cus = kp().parse(
          """
            /**
             *
             */
            class Test {
            	private val publisher: String = "1"
            	public fun method() {
            		var value = 0
                	var value1 = 1
            	    var value2 = 2
            		{
            	        var value3 = 2
                	    var value4 = 4
            		}
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    // TabSize 3 is atypical but not unheard of
    @Disabled
    // @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize3() {
        var cus = kp().parse(
          """
            /**
             *
             */
            public class Test {
            	private final ApplicationEventPublisher publisher;
            	public void method() {
            		int value = 0;
            	   int value1 = 1;
            	   int value2 = 2;
            		{
            	      int value3 = 2;
            	      int value4 = 4;
            		}
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(org.openrewrite.java.style.TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isTrue();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(3);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(3);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize4AndUseTabIsFalse() {
        var cus = kp().parse(
          """
            /**
             *
             */
            class Test {
            	private final publisher: String = "A"
                public fun method() {
            	    val value = 0;
            	    val value1 = 1;
            	    val value2 = 2;
            	    {
                	    val value3 = 2;
                    	val value4 = 4;
            	    }
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void inconsistentIndents() {
        var cus = kp().parse(
          """
            package org.openrewrite.before;

            import java.util.ArrayList;
            import java.util.List;

            public class HelloWorld {
                public fun main() {
                    System.out.print("Hello");
                        System.out.println(" world");
                }
            }
            """
        );
        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void mixedTabAndWhiteSpacesIndentsWithTabSize4WithSomeErrors() {
        var cus = kp().parse(
          """
            /**
             *
             */
            public class Test {
                private final publisher: String = "A"
                public fun method() {
            	     var value = 0
            	   var value1 = 1
            	    var value2 = 2
            	    {
                	     var value3 = 2
                   	var value4 = 4
            	    }
            	}
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));
        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void defaultKotlinImportLayout() {
        var cus = kp().parse(
          """
            import org.a.A
            import org.a.B

            import java.util.Map
            import java.util.Set

            import javax.lang.model.type.ArrayType

            import kotlin.math.PI
            import kotlin.math.sqrt
            import kotlin.random.Random

            import kotlin.collections.Map as KMap
            import kotlin.collections.Set as KSet

            class Test {
                var a : Map<Int, Int>? = null
                var b : Set<Int>? = null
                var c = PI;
                var d = sqrt(1.0)
                var e = Random(1)
                var f : KMap<Int, String> = mapOf(1 to "one", 2 to "two", 3 to "three")
                var g : KSet<Int> = setOf(1, 2, 3)
                var h : ArrayType? = null
                var i : A? = null
                var j : B? = null
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getLayout().size()).isEqualTo(5);

        assertThat(importLayout.getLayout().get(0)).isInstanceOf(ImportLayoutStyle.Block.AllOthers.class);

        assertThat(importLayout.getLayout().get(1))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("java\\..+"));

        assertThat(importLayout.getLayout().get(2))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("javax\\..+"));

        assertThat(importLayout.getLayout().get(3))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("kotlin\\..+"));

        assertThat(importLayout.getLayout().get(4)).isInstanceOf(ImportLayoutStyle.Block.AllAliases.class);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void customizedKotlinImportLayout() {
        var cus = kp().parse(
          """
            import kotlin.collections.Map as KMap
            import kotlin.collections.Set as KSet

            import kotlin.math.PI
            import kotlin.math.sqrt
            import kotlin.random.Random

            import javax.lang.model.type.ArrayType
            
            import java.util.Map
            import java.util.Set

            import org.a.A
            import org.a.B

            class Test {
                var a : Map<Int, Int>? = null
                var b : Set<Int>? = null
                var c = PI;
                var d = sqrt(1.0)
                var e = Random(1)
                var f : KMap<Int, String> = mapOf(1 to "one", 2 to "two", 3 to "three")
                var g : KSet<Int> = setOf(1, 2, 3)
                var h : ArrayType? = null
                var i : A? = null
                var j : B? = null
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getLayout().size()).isEqualTo(5);

        assertThat(importLayout.getLayout().get(0)).isInstanceOf(ImportLayoutStyle.Block.AllAliases.class);

        assertThat(importLayout.getLayout().get(1))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("kotlin\\..+"));

        assertThat(importLayout.getLayout().get(2))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("javax\\..+"));

        assertThat(importLayout.getLayout().get(3))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("java\\..+"));

        assertThat(importLayout.getLayout().get(4)).isInstanceOf(ImportLayoutStyle.Block.AllOthers.class);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void partialImportLayout() {
        var cus = kp().parse(
          """
            import java.util.Map
            import java.util.Set
            import java.util.stream.Collectors

            import kotlin.math.PI
            import kotlin.math.cos
            import kotlin.math.sin
            import kotlin.math.sqrt
            import kotlin.random.Random
            import kotlin.collections.List
            import kotlin.text.toCharArray
            import kotlin.time.Duration
            import kotlin.sequences.Sequence

            class Test {
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getLayout().get(0))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("java\\..+"));

        assertThat(importLayout.getLayout().get(1))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("kotlin\\..+"));

        assertThat(importLayout.getLayout().get(2)).isInstanceOf(ImportLayoutStyle.Block.AllOthers.class);

        assertThat(importLayout.getLayout().get(3))
          .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class)
          .matches(b -> ((ImportLayoutStyle.Block.ImportPackage) b).getPackageWildcard().toString().equals("javax\\..+"));

        assertThat(importLayout.getLayout().get(4)).isInstanceOf(ImportLayoutStyle.Block.AllAliases.class);
    }

    @Disabled
    // @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void detectStarImport() {
        var cus = kp().parse(
          """
            import java.util.*;

            class Test {
                val l: List<Integer>? = null
                val s: Set<Integer>? = null
                val m: Map<Integer, Integer>? = null
                val c: Collection<Integer>? = null
                val lhm: LinkedHashMap<Integer, Integer>? = null
                val integer: HashSet<Integer>? = null
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getTopLevelSymbolsToUseStarImport()).isEqualTo(6);
        // assertThat(importLayout.getClassCountToUseStarImport()).isEqualTo(6);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void detectImportCounts() {
        var cus = kp().parse(
          """
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.HashSet;
                import java.util.List;
                import java.util.Set;

                import javax.persistence.Entity;
                import javax.persistence.FetchType;
                import javax.persistence.JoinColumn;
                import javax.persistence.JoinTable;
                import javax.persistence.ManyToMany;
                import javax.persistence.Table;
                import javax.xml.bind.annotation.XmlElement;

                public class Test {
                    List<Integer> l;
                    Set<Integer> s;
                    Map<Integer, Integer> m;
                    Collection<Integer> c;
                    LinkedHashMap<Integer, Integer> lhm;
                    HashSet<Integer> integer;
                }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var importLayout = NamedStyles.merge(ImportLayoutStyle.class, singletonList(styles));

        assertThat(importLayout.getTopLevelSymbolsToUseStarImport()).isEqualTo(5);
        assertThat(importLayout.getJavaStaticsAndEnumsToUseStarImport()).isEqualTo(3);
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void detectMethodArgs() {
        var cus = kp().parse(
          """
                class Test {
                    fun i() {
                        a("a" ,"b" ,"c" ,"d");
                    }
                }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isTrue();
        assertThat(spacesStyle.getOther().getAfterComma()).isFalse();
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void detectMethodArgAfterComma() {
        var cus = kp().parse(
          """
                class Test {
                    fun i() {
                        a("a", "b");
                    }
                }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void detectMethodArgsNoArgs() {
        var cus = kp().parse(
          """
            class Test {
                void i() {
                    a();
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void detectMethodArgsNoSpaceForComma() {
        var cus = kp().parse(
          """
            class Test {
                fun i() {
                    a("a","b","c");
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isFalse();
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void detectMethodArgsSpaceForComma() {
        var cus = kp().parse(
          """
                class Test {
                    fun i() {
                        a("a" , "b" , "c");
                    }
                }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isTrue();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void detectAfterCommaInNewArray() {
        var cus = kp().parse(
          """
            class T {
                companion object {
                    val i = intArrayOf(1, 2, 3, 4)
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3172")
    void detectAfterCommaShouldIgnoreFirstElement() {
        var cus = kp().parse(
          """
            class T {
                companion object {
                    val i0 = intArrayOf(1, 2)
                    val i1 = intArrayOf(2, 3)
                    val i2 = intArrayOf(3, 4)
                    val i3 = intArrayOf(4, 5)
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }

    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3172")
    void detectAfterCommaBasedOnLambdas() {
        var cus = kp().parse(
          """
            import java.util.function.BiConsumer
                            
            class T {
                companion object {
                    init {
                        val i0 = intArrayOf(1, 2)
                        val i1 = intArrayOf(2, 3)
                            
                        val c0: BiConsumer<*, *> = BiConsumer { a, b -> }
                        val c1: BiConsumer<*, *> = BiConsumer { a, b -> }
                        val c2: BiConsumer<*, *> = BiConsumer { a, b -> }
                    }
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var spacesStyle = NamedStyles.merge(SpacesStyle.class, singletonList(styles));

        assertThat(spacesStyle.getOther().getBeforeComma()).isFalse();
        assertThat(spacesStyle.getOther().getAfterComma()).isTrue();
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void detectElseWithNoNewLine() {
        var cus = kp().parse(
          """
            class Test {
                fun method(n: Int) {
                    if (n == 0) {
                    } else if (n == 1) {
                    } else {
                    }
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var wrappingAndBracesStyle = NamedStyles.merge(WrappingAndBracesStyle.class, singletonList(styles));

        assertThat(wrappingAndBracesStyle.getIfStatement().getElseOnNewLine()).isFalse();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void detectElseOnNewLine() {
        var cus = kp().parse(
          """
                class Test {
                    fun method(n: Int) {
                        if (n == 0) {
                        }
                        else if (n == 1) {
                        }
                        else {
                        }
                    }
                }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var wrappingAndBracesStyle = NamedStyles.merge(WrappingAndBracesStyle.class, singletonList(styles));

        assertThat(wrappingAndBracesStyle.getIfStatement().getElseOnNewLine()).isTrue();
    }

    @Disabled
    // @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void mostCommonIndentTakesPrecedence() {
        var cus = kp().parse(
          """
            package com.test;

            public class Foo {
               private val underIndented: Int = 1
                     var order: Int = 2
                  fun setOrder(order: Int) {
                        this.order = order
                        print("One two-space indent shouldn't override predominant 4-space indent")
                        val o = object {
                              fun fooBar() {
                                    print("fooBar");
                              }
                        };
                  }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(6);
        assertThat(tabsAndIndents.getIndentSize())
          .as("While there are outlier 3 and 9 space indents, the most prevalent indentation is 6")
          .isEqualTo(6);
        assertThat(tabsAndIndents.getContinuationIndent())
          .as("With no actual continuation indents to go off of, assume IntelliJ default of 2x the normal indent")
          .isEqualTo(12);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "RedundantStreamOptionalCall"})
    @Disabled("FIXME, to be supported by PSI parser")
    @Test
    void continuationIndents() {
        var cus = kp().parse(
          """
            import java.util.stream.Stream;

            class Continuations {
                fun cont() {
                    Stream.of("foo",
                                            "continuation")
                                .map{it ->
                                            Stream.of(it)
                                                        .map{it2 ->
                                                                    Stream.of(it2)
                                                                                .map{it3 ->
                                                                                            it3}}
                                                        .flatMap{it4 ->
                                                                    it4}}
                    val higherContIndent = 1 +
                                                    2
                    val lowerContIndent = 1 +
                            2
                    val sum = 1 +
                                (2 +
                                3) +
                                Stream.of(
                                            2 + 4,
                                            4
                                ).reduce(0){ acc, value -> acc + value}
                }
            }
            """
        );

        var detector = Autodetect.detector();
        cus.forEach(detector::sample);
        var styles = detector.build();
        var tabsAndIndents = NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(styles));

        assertThat(tabsAndIndents.getUseTabCharacter()).isFalse();
        assertThat(tabsAndIndents.getTabSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getIndentSize()).isEqualTo(4);
        assertThat(tabsAndIndents.getContinuationIndent()).isEqualTo(12);
    }
}
