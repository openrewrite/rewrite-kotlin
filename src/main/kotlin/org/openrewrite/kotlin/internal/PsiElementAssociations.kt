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
package org.openrewrite.kotlin.internal

import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.openrewrite.java.tree.JavaType
import org.openrewrite.kotlin.KotlinTypeMapping
import java.util.*

class PsiElementAssociations(private val typeMapping: KotlinTypeMapping) {

    private val elementMap: MutableMap<PsiInfo, MutableList<FirInfo>> = HashMap()
    private var file: FirFile? = null

    fun initialize(file: FirFile) {
        this.file = file
        // debug purpose only, to be removed
        System.out.println(PsiTreePrinter.print(file))

        var depth = 0
        object : FirDefaultVisitor<Unit, MutableMap<PsiInfo, MutableList<FirInfo>>>() {
            override fun visitElement(element: FirElement, data: MutableMap<PsiInfo, MutableList<FirInfo>>) {
                if (element.source != null && element.source.psi != null) {
                    val psiElement = element.source!!.psi!!
                    val psiInfo = PsiInfo(
                        psiElement.startOffset,
                        psiElement.endOffset
                    )
                    val firInfo = FirInfo(
                        depth,
                        element
                    )
                    data.computeIfAbsent(psiInfo) { ArrayList() } += firInfo
                }
                depth++
                element.acceptChildren(this, data)
                depth--
            }
        }.visitFile(file, elementMap)
    }

    fun type(range: Pair<Int, Int>): JavaType? {
        // find the eligible enclosing FirElement
        var enclosingFir: FirElement? = null
        var minDiff = Int.MAX_VALUE

        elementMap.forEach { (psiInfo, firInfos) ->
            val startOffset = psiInfo.startOffset
            val endOffset = psiInfo.endOffset
            if (startOffset <= range.first &&
                endOffset >= range.second
            ) {
                val diff = (range.first - startOffset) + (endOffset - range.second)
                if (diff < minDiff) {
                    minDiff = diff

                    var maxDepth = -1
                    firInfos.forEach { firInfo ->
                        if (firInfo.fir.source is KtRealPsiSourceElement && firInfo.depth > maxDepth) {
                            enclosingFir = firInfo.fir
                            maxDepth = firInfo.depth
                        }
                    }
                }
            }
        }

        return typeMapping.type(enclosingFir, file?.symbol)
    }

    private class FirInfo (
        val depth: Int,
        val fir: FirElement
    ) {
        override fun toString(): String {
            val s = PsiTreePrinter.printFirElement(fir)
            return "FIR($depth, $s)"
        }
    }

    private class PsiInfo(
        val startOffset: Int,
        val endOffset: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PsiInfo) return false
            return startOffset == other.startOffset && endOffset == other.endOffset
        }

        override fun hashCode(): Int {
            return Objects.hash(startOffset, endOffset)
        }

        override fun toString(): String {
            return "PSI([$startOffset,$endOffset])"
        }
    }
}