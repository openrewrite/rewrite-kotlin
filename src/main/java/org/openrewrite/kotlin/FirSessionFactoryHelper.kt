package org.openrewrite.kotlin

import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirJvmSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.createSymbolProviders
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

object FirSessionFactoryHelper {

    fun createSessionWithDependencies(
        moduleName: Name,
        platform: TargetPlatform,
        externalSessionProvider: FirProjectSessionProvider?,
        projectEnvironment: VfsBasedProjectEnvironment,
        languageVersionSettings: LanguageVersionSettings,
        javaSourcesScope: AbstractProjectFileSearchScope,
        lookupTracker: LookupTracker?,
        enumWhenTracker: EnumWhenTracker?,
        importTracker: ImportTracker?,
        incrementalCompilationContext: IncrementalCompilationContext?,
        extensionRegistrars: List<FirExtensionRegistrar>,
        needRegisterJavaElementFinder: Boolean,
        dependenciesConfigurator: DependencyListForCliModule.Builder.() -> Unit = {},
        sessionConfigurator: FirSessionConfigurator.() -> Unit = {},
    ): FirSession {
        val binaryModuleData = BinaryModuleData.initialize(moduleName, platform)
        val dependencyList = DependencyListForCliModule.build(binaryModuleData, init = dependenciesConfigurator)
        val sessionProvider = externalSessionProvider ?: FirProjectSessionProvider()

        val mainModuleData = FirModuleDataImpl(
            moduleName,
            dependencyList.regularDependencies,
            dependencyList.dependsOnDependencies,
            dependencyList.friendsDependencies,
            platform,
        )
        return FirJvmSessionFactory.createModuleBasedSession(
            mainModuleData,
            sessionProvider,
            javaSourcesScope,
            projectEnvironment,
            { incrementalCompilationContext?.createSymbolProviders(it, mainModuleData, projectEnvironment) },
            extensionRegistrars,
            languageVersionSettings,
            JvmTarget.DEFAULT,
            lookupTracker,
            enumWhenTracker,
            importTracker,
            predefinedJavaComponents = null,
            needRegisterJavaElementFinder,
            init = sessionConfigurator
        )
    }
}
