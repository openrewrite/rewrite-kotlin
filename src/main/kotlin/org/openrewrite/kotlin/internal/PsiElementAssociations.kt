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
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.openrewrite.java.tree.JavaType
import org.openrewrite.kotlin.KotlinIrTypeMapping

class PsiElementAssociations(val typeMapping: KotlinIrTypeMapping, private val psiFile: PsiFile, val file: IrFile) {

    private val psiMap: MutableMap<TextRange, MutableSet<PsiElement>> = HashMap()
    private val elementMap: MutableMap<PsiElement, MutableSet<IrInfo>> = HashMap()

    fun initialize() {
        var depth = 0
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                psiMap.computeIfAbsent(element.textRange) { HashSet() } += element
                element.acceptChildren(this)
            }
        }.visitFile(psiFile)

        object : IrElementVisitor<Unit, MutableMap<PsiElement, MutableSet<IrInfo>>> {
            override fun visitElement(element: IrElement, data: MutableMap<PsiElement, MutableSet<IrInfo>>) {
                if (element is IrMetadataSourceOwner) {
                    if (element.metadata is FirMetadataSource) {
                        when (element.metadata) {
                            is FirMetadataSource.File -> {
                                val source = (element.metadata!! as FirMetadataSource.File).files[0].source
                                if (source is KtRealPsiSourceElement) {
                                    elementMap.computeIfAbsent(source.psi) { HashSet() } += IrInfo(element, depth)
                                }
                            }

                            is FirMetadataSource.Class -> {
                                val source = (element.metadata!! as FirMetadataSource.Class).fir.source
                                if (source is KtRealPsiSourceElement) {
                                    elementMap.computeIfAbsent(source.psi) { HashSet() } += IrInfo(element, depth)
                                }
                            }

                            is FirMetadataSource.Function -> {
                                val source = (element.metadata!! as FirMetadataSource.Function).fir.source
                                if (source is KtRealPsiSourceElement) {
                                    elementMap.computeIfAbsent(source.psi) { HashSet() } += IrInfo(element, depth)
                                }
                            }

                            is FirMetadataSource.Property -> {
                                val source = (element.metadata!! as FirMetadataSource.Property).fir.source
                                if (source is KtRealPsiSourceElement) {
                                    elementMap.computeIfAbsent(source.psi) { HashSet() } += IrInfo(element, depth)
                                }
                            }

                            is FirMetadataSource.Script -> {
                                val source = (element.metadata!! as FirMetadataSource.Script).fir.source
                                if (source is KtRealPsiSourceElement) {
                                    elementMap.computeIfAbsent(source.psi) { HashSet() } += IrInfo(element, depth)
                                }
                            }
                        }
                    }
                } else if (element.startOffset >= 0 && element.endOffset >= 0) {
                    val textRange = TextRange.create(element.startOffset, element.endOffset)
                    if (psiMap.containsKey(textRange)) {
                        val psi = psiMap[textRange]
                        when (element) {
                            is IrConst<*> -> {
                                for (p in psi!!.toList()) {
                                    if (p is KtConstantExpression || p is KtStringTemplateExpression) {
                                        elementMap.computeIfAbsent(p) { HashSet() } += IrInfo(element, depth)
                                        break
                                    }
                                }
                            }
                            is IrBlockBody, is IrExpressionBody, is IrWhen, is IrBranch -> {
                                // Skip
                            }
                            else -> {
                                // TODO: map if necessary
                            }
                        }
                    }
                }

                depth++
                element.acceptChildren(this, data)
                depth--
            }
        }.visitFile(file, elementMap)
        validate()
    }

    private fun validate() {
        // TODO
    }

    fun type(psiElement: PsiElement): JavaType? {
        val ir = primary(psiElement)
        return if (ir != null) typeMapping.type(ir) else null
    }

    fun primary(psiElement: PsiElement) =
        ir(psiElement) {
            when (psiElement) {
                is KtClass -> {
                    it is IrClass
                }

                is KtConstantExpression, is KtStringTemplateExpression -> {
                    it is IrConst<*>
                }

                is KtProperty -> {
                    it is IrProperty
                }

                else -> {
                    it is IrElement
                }
            }
        }

    fun ir(psi: PsiElement?, filter: (IrElement) -> Boolean): IrElement? {
        var p = psi
        while (p != null && !elementMap.containsKey(p)) {
            p = p.parent
        }

        if (p == null) {
            return null
        }

        val allIrInfos = elementMap[p]!!
        val directIrInfos = allIrInfos.filter { filter.invoke(it.ir) }
        return if (directIrInfos.isNotEmpty())
            directIrInfos[0].ir
        else if (directIrInfos.isNotEmpty())
            directIrInfos[0].ir
        else
            null
    }

    fun methodDeclarationType(psiElement: PsiElement): IrFunction? {
        val ir = primary(psiElement)
        return if (ir is IrFunction) ir else null
    }

    fun methodInvocationType(psiElement: PsiElement): IrElement? {
        val ir = primary(psiElement)
        return if (ir is IrFunction) ir else null
    }

    enum class ExpressionType {
        CONSTRUCTOR,
        METHOD_INVOCATION,
        RETURN_EXPRESSION
    }

    fun getFunctionType(psi: KtExpression): ExpressionType? {
        val ir = ir(psi) { it is IrCall } as? IrCall

        if (ir == null) {
            return null
        }

        return null
    }

    private fun PsiElement.customToString(): String {
        return "PSI ${this.textRange} $this"
    }

    override fun toString(): String {
        val sb = StringBuilder()
//        elementMap.forEach{ (psi, firs) ->
//            sb.append(t).append("\n")
//            firs.forEach{ fir ->
//                sb.append("  - $fir\n")
//            }
//            sb.append("\n")
//        }
        return sb.toString()
    }

    private class IrInfo(
        val ir: IrElement,
        val depth: Int,
    ) {
        override fun toString(): String {
            val s = PsiTreePrinter.printIrElement(ir)
            return "FIR($depth, $s)"
        }
    }

    companion object {
        fun printElement(firElement: FirElement): String {
            if (firElement is FirSingleExpressionBlock) {
                return PsiTreePrinter.firElementToString(firElement.statement)
            } else if (firElement is FirElseIfTrueCondition) {
                return "true";
            }

            return "";
        }
    }
}