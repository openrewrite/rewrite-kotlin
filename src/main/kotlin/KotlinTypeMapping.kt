import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.openrewrite.java.internal.JavaTypeCache
import org.openrewrite.java.tree.JavaType

class KotlinTypeMapping(typeCache: JavaTypeCache, firSession: FirSession) {

    fun type(type: Any?, ownerFallback: FirBasedSymbol<*>?): JavaType? {
        return null
    }

    fun type(type: Any?): JavaType? {
        return null
    }

    fun methodDeclarationType(
        fir: FirFunction,
        asFullyQualified: JavaType.FullyQualified?,
        currentFile: FirBasedSymbol<*>?): JavaType.Method? {
        return null
    }

    fun variableType(
        firVariableSymbol: FirVariableSymbol<out FirVariable>,
        asFullyQualified: JavaType.FullyQualified?,
        currentFile: FirBasedSymbol<*>?
    ): JavaType.Variable? {
        return null
    }

    fun primitive(
        coneClassLikeType: ConeClassLikeType
    ): JavaType.Primitive {
        return JavaType.Primitive.None
    }

    fun methodInvocationType(
        functionCall: FirFunctionCall,
        currentFile: FirBasedSymbol<*>?
    ): JavaType.Method? {
        return null
    }


}