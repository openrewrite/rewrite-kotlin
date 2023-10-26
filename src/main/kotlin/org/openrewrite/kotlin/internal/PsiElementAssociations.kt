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
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.openrewrite.java.tree.JavaType
import org.openrewrite.kotlin.KotlinIrTypeMapping

class PsiElementAssociations(
    val typeMapping: KotlinIrTypeMapping,
    private val fir2IrComponents: Fir2IrComponents,
    private val commonMemberStorage: Fir2IrCommonMemberStorage,
    private val file: FirFile,
    private val irFile: IrFile // TEMP: to compare mappings.
) {

    private val elementMap: MutableMap<PsiElement, MutableList<FirInfo>> = HashMap()

    fun initialize() {
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

//        validate()
    }
//    private fun validate() {
//        var found1ToNMapping = false
//        elementMap.forEach { (psi, firList) ->
//            var fakeCount = 0
//            var realCount = 0
//            var otherCount = 0
//            for (firElement in firList) {
//                if (firElement.fir.source is KtRealPsiSourceElement) {
//                    realCount++
//                } else if (firElement.fir.source is KtFakeSourceElement) {
//                    fakeCount++
//                } else {
//                    otherCount++
//                }
//            }
//
//            // print out logs, debug purpose only, to be removed after complete parser
//            if (false) {
//                found1ToNMapping = realCount > 1
//
//                println("---------")
//                println("PSI: $psi")
//                println("FIR: $firList")
//
//                println("Found 1 to $realCount Real mapping!")
//                println("    types from $realCount Real elements:")
//                var firstUnknown = false
//                var hasNonUnknown = false
//                for ((index, firElement) in firList.withIndex()) {
//                    if (firElement.fir.source is KtRealPsiSourceElement) {
//                        val type = typeMapping.type(firElement.fir).toString()
//                        if (index == 0 && type.equals("Unknown")) {
//                            firstUnknown = true
//                        }
//
//                        if (!type.equals("Unknown")) {
//                            hasNonUnknown = true
//                        }
//
//                        val padded = "        -$type".padEnd(30, ' ')
//                        println("$padded - $firElement")
//                    }
//                }
//
//                if (firstUnknown && hasNonUnknown) {
//                    throw IllegalArgumentException("First type is Unknown!")
//                }
//
//                println("    types from $fakeCount Fake elements:")
//                for (firElement in firList) {
//                    if (firElement.fir.source is KtFakeSourceElement) {
//                        val type = typeMapping.type(firElement.fir).toString()
//                        val padded = "        -$type".padEnd(30, ' ')
//                        println("$padded - $firElement")
//
//                    }
//                }
//            }
//        }
//
//        if (found1ToNMapping) {
//            // throw IllegalArgumentException("Found 1 to N real mapping!")
//        }
//    }

//    fun type(psiElement: PsiElement, ownerFallBack: FirBasedSymbol<*>?): JavaType? {
//        val fir = primary(psiElement)
//        return if (fir != null) typeMapping.type(fir, ownerFallBack) else null
//    }

    // TODO: Knut/Sam input on nullability. We'll need it for other languages as well.
    fun type(psi: PsiElement): JavaType {
        val ir: Any? = when (val fir = primary(psi)) {
            is FirPackageDirective -> fir2IrComponents.declarationStorage.getIrExternalPackageFragment(fir.packageFqName)
            is FirBinaryLogicExpression -> fir.typeRef.toIrType(getTypeConverter())
            is FirBlock -> {
                when (psi) {
                    is KtPostfixExpression -> (fir.statements.first { it is FirPropertyAccessExpression } as FirPropertyAccessExpression).typeRef.toIrType(getTypeConverter())
                    is KtPrefixExpression -> (fir.statements.first { it is FirVariableAssignment } as FirVariableAssignment).lValue.typeRef.toIrType(getTypeConverter())
                    /* is KtNamedReferenceExpression it is FirFunctionCall ?? */ else -> null // TODO
                }
            }
            is FirCall -> null // TODO
            is FirComparisonExpression -> fir.typeRef.toIrType(getTypeConverter())
            is FirConstExpression<*> -> fir.typeRef.toIrType(getTypeConverter())
            is FirDeclaration -> resolveDeclaration(fir)
            is FirElvisExpression -> fir.typeRef.toIrType(getTypeConverter())
            is FirNamedArgumentExpression -> fir.typeRef.toIrType(getTypeConverter())
            is FirResolvedNamedReference -> resolveSymbol(fir.resolvedSymbol)
            is FirResolvedTypeRef -> fir.toIrType(getTypeConverter())
            is FirResolvedQualifier -> null // TODO
            is FirSafeCallExpression -> fir.typeRef.toIrType(getTypeConverter())
            is FirQualifiedAccessExpression -> fir.typeRef.toIrType(getTypeConverter())
            is FirWhenExpression -> fir.typeRef.toIrType(getTypeConverter())
            is FirVariableAssignment -> fir.lValue.typeRef.toIrType(getTypeConverter())
            is FirErrorNamedReference -> {}
            else -> null
        }
        return typeMapping.type(ir)
    }

    @OptIn(SymbolInternals::class)
    private fun resolveDeclaration(fir: FirDeclaration): IrElement? {
        return when (fir) {
//            is FirAnonymousInitializer -> TODO()
//            is FirAnonymousFunction -> TODO()
//            is FirAnonymousObject -> TODO()
//            is FirBackingField -> TODO()
            is FirConstructor -> getCachedFunction(fir)
//            is FirDanglingModifierList -> TODO()
            is FirEnumEntry -> getCachedEnumEntry(fir)
//            is FirErrorFunction -> TODO()
//            is FirErrorProperty -> TODO()
//            is FirField -> TODO()
//            is FirFile -> TODO()
//            is FirPropertyAccessor -> TODO()
            is FirSimpleFunction -> getCachedFunction(fir)
            is FirProperty -> getCachedProperty(fir)
            is FirValueParameter -> {
                null // TODO: find link from FIR through components to IR.
            }

            is FirRegularClass -> getCachedClass(fir)
//            is FirTypeAlias -> TODO()
//            is FirScript -> TODO()

            is FirTypeParameter -> getCachedTypeParameter(fir)
            else -> null
        }
    }

    @OptIn(SymbolInternals::class)
    private fun resolveSymbol(symbol: FirBasedSymbol<*>): IrElement? {
        return when (symbol) {
            is FirConstructorSymbol -> getCachedConstructor(symbol.fir)
            is FirEnumEntrySymbol -> getCachedEnumEntry(symbol.fir)
            is FirNamedFunctionSymbol -> getCachedFunction(symbol.fir)
            is FirPropertySymbol -> getCachedProperty(symbol.fir)
            else -> null
        }
    }

    // TODO: Fix primary for syntax sugar elements like KtPostFixExpression.
    //       The first real psi element may be incorrect for sugared elements
    fun primary(psi: PsiElement) =
        fir(psi) { it.source is KtRealPsiSourceElement }

    fun methodDeclarationType(psi: PsiElement): JavaType.Method? {
        return when (val fir = primary(psi)) {
            is FirFunction -> typeMapping.methodDeclarationType(getCachedFunction(fir))
            is FirAnonymousFunctionExpression -> TODO()
            else -> null
        }
    }

    fun methodDeclarationType(fir: FirFunction): JavaType.Method? {
        return typeMapping.methodDeclarationType(getCachedFunction(fir))
    }

    @OptIn(SymbolInternals::class)
    fun methodInvocationType(psi: PsiElement): JavaType.Method? {
        return when (val fir = primary(psi)) {
            is FirFunctionCall -> null // find link from FIR call to IR call
            else -> null
        }
    }

    fun primitive(psi: PsiElement): JavaType.Primitive {
        return when (val fir = primary(psi)) {
            is FirConstExpression<*> -> typeMapping.primitive(fir.typeRef.toIrType(fir2IrComponents.typeConverter))
            else -> JavaType.Primitive.None
        }
    }

    fun variableType(psi: PsiElement): JavaType.Variable? {
        return when (val fir = primary(psi)) {
            is FirProperty -> typeMapping.variableType(getCachedProperty(fir))
            is FirValueParameter -> null // TODO
            else -> null
        }
    }

    fun variableType(fir: FirProperty): JavaType.Variable? {
        return typeMapping.variableType(getCachedProperty(fir))
    }

    private fun getCachedClass(constructor: FirClass): IrClass? {
        return fir2IrComponents.classifierStorage.getCachedIrClass(constructor)
    }

    private fun getCachedConstructor(constructor: FirConstructor): IrConstructor? {
        return fir2IrComponents.declarationStorage.getCachedIrConstructor(constructor)
    }

    private fun getCachedEnumEntry(enumEntry: FirEnumEntry): IrEnumEntry? {
        return commonMemberStorage.enumEntryCache[enumEntry]
    }

    private fun getCachedFunction(function: FirFunction): IrFunction? {
        return fir2IrComponents.declarationStorage.getCachedIrFunction(function)
    }

    private fun getCachedProperty(property: FirProperty): IrProperty? {
        return fir2IrComponents.declarationStorage.getCachedIrProperty(property)
    }

    private fun getCachedTypeParameter(parameter: FirTypeParameter): IrTypeParameter? {
        return commonMemberStorage.typeParameterCache[parameter]
    }

    private fun getTypeConverter(): Fir2IrTypeConverter {
        return fir2IrComponents.typeConverter
    }

    fun fir(psi: PsiElement?, filter: (FirElement) -> Boolean): FirElement? {
        var p = psi
        while (p != null && !elementMap.containsKey(p)) {
            p = p.parent
            // don't skip KtDotQualifiedExpression for field access
//            if (p is KtDotQualifiedExpression) {
//                return null
//            }
        }

        if (p == null) {
            return null
        }

        val allFirInfos = elementMap[p]!!
        val directFirInfos = allFirInfos.filter { filter.invoke(it.fir) }
        if (directFirInfos.size > 1) {
            println()
        }
        return if (directFirInfos.isNotEmpty())
            directFirInfos[0].fir
        else if (allFirInfos.isNotEmpty())
            allFirInfos[0].fir
        else
            null
    }

    enum class ExpressionType {
        CONSTRUCTOR,
        METHOD_INVOCATION,
        RETURN_EXPRESSION
    }

    fun getFunctionCallType(psi: KtExpression): ExpressionType? {
        val fir = fir(psi) { it is FirFunctionCall } as? FirFunctionCall

        if (fir == null) {
            return null
        }

        return if (fir.calleeReference.resolved != null) {
            when (fir.calleeReference.resolved!!.resolvedSymbol) {
                is FirConstructorSymbol -> ExpressionType.CONSTRUCTOR
                else -> ExpressionType.METHOD_INVOCATION
            }
        } else if (fir.typeRef is FirErrorTypeRef) {
            return null
        }
        else {
            throw UnsupportedOperationException("Null resolved symbol on FirFunctionCall: $psi")
        }
    }

    fun getExpressionType(psi: KtExpression): ExpressionType? {
        val fir = fir(psi) { it is FirExpression }
        return if (fir is FirReturnExpression) {
            ExpressionType.RETURN_EXPRESSION
        } else {
            // TODO, other expression type if needed
            null
        }
    }

    private fun PsiElement.customToString(): String {
        return "PSI ${this.textRange} $this"
    }

    override fun toString(): String {
        val sb = StringBuilder()
        elementMap.forEach { (psi, firs) ->
            sb.append(psi.customToString()).append("\n")
            firs.forEach { fir ->
                sb.append("  - $fir\n")
            }
            sb.append("\n")
        }
        return sb.toString()
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
                return PsiTreePrinter.firElementToString(firElement.statement)!!
            } else if (firElement is FirElseIfTrueCondition) {
                return "true";
            }

            return "";
        }
    }
}