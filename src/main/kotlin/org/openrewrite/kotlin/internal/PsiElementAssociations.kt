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
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.openrewrite.java.tree.JavaType
import org.openrewrite.kotlin.KotlinTypeMapping

class PsiElementAssociations(val typeMapping: KotlinTypeMapping) {

    private val elementMap: MutableMap<PsiElement, MutableList<FirInfo>> = HashMap()
    private var file: FirFile? = null

    fun initialize(file: FirFile) {
        this.file = file
        var depth = 0
        object : FirDefaultVisitor<Unit, MutableMap<PsiElement, MutableList<FirInfo>>>() {
            override fun visitElement(element: FirElement, data: MutableMap<PsiElement, MutableList<FirInfo>>) {
                if (element.source != null && element.source.psi != null) {
                    val psiElement = element.source!!.psi!!
                    val firInfo = FirInfo(element, depth)
                    data.computeIfAbsent(psiElement) { ArrayList() } += firInfo
                }
                depth++
                element.acceptChildren(this, data)
                if (element is FirResolvedTypeRef) {
                    // not sure why this isn't taken care of by `FirResolvedTypeRefImpl#acceptChildren()`
                    element.delegatedTypeRef?.accept(this, data)
                }
                depth--
            }
        }.visitFile(file, elementMap)
    }

    fun type(psiElement: PsiElement, ownerFallBack: FirBasedSymbol<*>?): JavaType? {
        val fir = primary(psiElement)
        return if (fir != null) typeMapping.type(fir, ownerFallBack) else null
    }

    fun symbol(psi: KtDeclaration?): FirBasedSymbol<*>? {
        val fir = fir(psi) { it is FirDeclaration }
        return if (fir != null) (fir as FirDeclaration).symbol else null
    }

    fun symbol(psi: KtExpression?): FirBasedSymbol<*>? {
        val fir = fir(psi) { it is FirResolvedNamedReference }
        return if (fir != null) (fir as FirResolvedNamedReference).resolvedSymbol else null
    }

    fun primary(psiElement: PsiElement) =
        fir(psiElement) { it.source is KtRealPsiSourceElement }

    fun fir(psi: PsiElement?, filter: (FirElement) -> Boolean) : FirElement? {
        var p = psi
        while (p != null && !elementMap.containsKey(p)) {
            p = p.parent
        }

        if (p == null) {
            return null
        }

        val allFirInfos = elementMap[p]!!
        val directFirInfos = allFirInfos.filter { filter.invoke(it.fir) }
        return if (directFirInfos.isNotEmpty())
            directFirInfos[0].fir
        else if (allFirInfos.isNotEmpty())
            allFirInfos[0].fir
        else
            null
    }

    private class FirInfo(
        val fir: FirElement,
        val depth: Int,
    ) {
        override fun toString(): String {
            val s = PsiTreePrinter.printFirElement(fir)
            return "FIR($depth, $s)"
        }
    }
}