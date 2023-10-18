package org.openrewrite.kotlin

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.types.Variance
import org.openrewrite.java.JavaTypeSignatureBuilder
import java.util.*

class KotlinTypeIrSignatureBuilder : JavaTypeSignatureBuilder {
    private var typeVariableNameStack: MutableSet<String>? = null

    override fun signature(type: Any?): String {
        if (type == null || type is IrErrorType) {
            return "{undefined}"
        }

        if (type is IrClassifierSymbol) {
            return signature(type.owner)
        }

        val baseType = if (type is IrSimpleType) type.classifier.owner else type

        when (baseType) {
            is IrFile -> {
                return fileSignature(baseType)
            }

            is IrClass -> {
                // The IrSimpleType may contain bounded type arguments
                val useSimpleType = (type is IrSimpleType && (type.arguments.isNotEmpty() || type.annotations.isNotEmpty()))
                return if (baseType.typeParameters.isEmpty()) classSignature(baseType) else parameterizedSignature(if (useSimpleType) type else baseType)
            }

            is IrFunction -> {
                return methodDeclarationSignature(baseType)
            }

            is IrProperty -> {
                return variableSignature(baseType)
            }

            is IrVariable -> {
                TODO("IrVariable not yet implemented.")
            }

            is IrField -> {
                TODO("IrField not yet implemented.")
            }

            is IrTypeAlias -> {
                TODO("IrTypeAlias not yet implemented.")
            }

            is IrScript -> {
                TODO("IrScript not yet implemented.")
            }

            is IrConstructorCall -> {
                TODO("IrConstructorCall not yet implemented.")
            }

            is IrTypeProjection, is IrStarProjection -> {
                return typeProjection(baseType)
            }

            is IrTypeParameter -> {
                return genericSignature(baseType)
            }
        }

        throw IllegalStateException("Unexpected type " + baseType.javaClass.getName())
    }

    private fun fileSignature(type: Any): String {
        if (type !is IrFile) {
            TODO()
        }

        return (if (type.fqName.asString().isNotEmpty()) type.fqName.asString() + "." else "") + type.name.replace(".kt", "Kt")
    }

    /**
     * Kotlin does not support dimensioned arrays.
     */
    override fun arraySignature(type: Any): String {
        throw UnsupportedOperationException("This should never happen.")
    }

    override fun classSignature(type: Any): String {
        if (type !is IrClass) {
            TODO("Not yet implemented")
        }
        val sb = StringBuilder()
        // TODO: review how Method parents should be represented.
        if (type.parent is IrClass) {
            sb.append(classSignature(type.parent)).append("$")
        } else if (type.packageFqName != null) {
            sb.append(type.packageFqName).append(".")
        }
        sb.append(type.name)
        return sb.toString()
    }

    override fun genericSignature(type: Any): String {
        val typeParameter: IrTypeParameter = type as IrTypeParameter
        val name = typeParameter.name.asString()

        if (typeVariableNameStack == null) {
            typeVariableNameStack = HashSet()
        }

        if (!typeVariableNameStack!!.add(name)) {
            return "Generic{$name}"
        }

        val s = StringBuilder("Generic{").append(name)
        val boundSigs = StringJoiner(" & ")
        for (bound in typeParameter.superTypes) {
            if (isNotAny(bound)) {
                boundSigs.add(signature(bound))
            }
        }
        val boundSigStr = boundSigs.toString()
        if (boundSigStr.isNotEmpty()) {
            s.append(" extends ").append(boundSigStr)
        }
        typeVariableNameStack!!.remove(name)
        return s.append("}").toString()
    }

    private fun typeProjection(type: Any): String {
        val sig = StringBuilder("Generic{")
        when (type) {
            is IrTypeProjection -> {
                sig.append("?")
                sig.append(if (type.variance == Variance.OUT_VARIANCE) " extends " else " super ")
                sig.append(signature(type.type))
            }
            is IrStarProjection -> {
                sig.append("*")
            }
            else -> {
                TODO()
            }
        }
        return sig.append("}").toString()
    }

    override fun parameterizedSignature(type: Any): String {
        if (type !is IrSimpleType && type !is IrClass) {
            TODO("Not yet implemented")
        }

        val s = StringBuilder(classSignature(if (type is IrSimpleType) type.classifier.owner else type))
        val joiner = StringJoiner(", ", "<", ">")
        val params = if (type is IrSimpleType) type.arguments else (type as IrClass).typeParameters
        for (tp in params) {
            joiner.add(signature(tp))
        }
        s.append(joiner)
        return s.toString()
    }

    override fun primitiveSignature(type: Any): String {
        TODO("Not yet implemented")
    }

    fun variableSignature(
        property: IrProperty
    ): String {
        val owner = if (property.parent is IrClass) classSignature(property.parent) else signature(property.parent)
        val typeSig = if (property.getter != null) {
            signature(property.getter!!.returnType)
        } else if (property.backingField != null) {
            signature(property.backingField!!.type)
        } else {
            TODO()
        }
        return "$owner{name=${property.name.asString()},type=$typeSig}"
    }

    fun methodDeclarationSignature(function: IrFunction): String {
        val parent = when (function.parent) {
            is IrClass -> classSignature(function.parent)
            else -> signature(function.parent)
        }
        val signature = StringBuilder(parent)
        if (function is IrConstructor) {
            signature.append("{name=<constructor>,return=$parent")
        } else {
            signature.append("{name=").append(function.name.asString())
            signature.append(",return=").append(signature(function.returnType))
        }
        signature.append(",parameters=").append(methodArgumentSignature(function)).append("}")
        return signature.toString()
    }

    private fun methodArgumentSignature(function: IrFunction): String {
        val genericArgumentTypes = StringJoiner(",", "[", "]")
        for (param: IrValueParameter in function.valueParameters) {
            genericArgumentTypes.add(signature(param.type))
        }
        return genericArgumentTypes.toString()
    }

    private fun isNotAny(type: IrType): Boolean {
        return !(type.classifierOrNull != null && type.classifierOrNull!!.owner is IrClass && "kotlin.Any" == (type.classifierOrNull!!.owner as IrClass).kotlinFqName.asString())
    }
}