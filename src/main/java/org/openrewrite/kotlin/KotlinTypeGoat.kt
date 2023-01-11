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
package org.openrewrite.kotlin

abstract class KotlinTypeGoat<T, S> {
//abstract class KotlinTypeGoat<T, S> where S: PT<S>, S: C {
//    val parameterizedField: PT<TypeA> = object : PT<TypeA> {}

//    abstract class InheritedKotlinTypeGoat<T, U> : KotlinTypeGoat<T, U>() where U : PT<U>, U : C

    enum class EnumTypeA {
        FOO, BAR(),
        @AnnotationWithRuntimeRetention
        FUZ
    }

    enum class EnumTypeB(label: TypeA) {
        FOO(TypeA());

        private val label: TypeA

        init {
            this.label = label
        }
    }

    abstract class Extension<U : Extension<U>>

    open class TypeA
    class TypeB

    @AnnotationWithRuntimeRetention
    @AnnotationWithSourceRetention
    abstract fun clazz(n: C)
    abstract fun primitive(n: Int)
    abstract fun parameterized(n: PT<C>): PT<C>
    abstract fun parameterizedRecursive(n: PT<PT<C>>): PT<PT<C>>
    abstract fun generic(n: PT<out C>): PT<out C>
    abstract fun genericContravariant(n: PT<in C>): PT<in C>
    abstract fun <U : KotlinTypeGoat<U, *>> genericRecursive(n: KotlinTypeGoat<out Array<U>, *>): KotlinTypeGoat<out Array<U>, *>
    abstract fun <U> genericUnbounded(n: PT<U>): PT<U>
    abstract fun inner(n: org.openrewrite.kotlin.C.Inner)
    abstract fun enumTypeA(n: EnumTypeA)
    abstract fun enumTypeB(n: EnumTypeB)
//    abstract fun <U> inheritedKotlinTypeGoat(n: InheritedKotlinTypeGoat<T, U>): InheritedKotlinTypeGoat<T, U> where U : PT<U>, U : C
//    abstract fun <U> genericIntersection(n: U): U where U : TypeA, U : PT<U>, U : C
    abstract fun genericT(n: T): T // remove after signatures are common.

//    abstract fun <U> recursiveIntersection(n: U) where U : KotlinTypeGoat.Extension<U>, U : Intersection<U>
}

interface C {
    class Inner
}

interface PT<T>

//internal interface Intersection<T> where T : KotlinTypeGoat.Extension<T>, T : Intersection<T> {
//    val intersectionType: T
//}

@Retention(AnnotationRetention.SOURCE)
internal annotation class AnnotationWithSourceRetention

@Retention(AnnotationRetention.RUNTIME)
internal annotation class AnnotationWithRuntimeRetention 