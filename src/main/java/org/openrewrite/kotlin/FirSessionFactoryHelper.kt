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
        val librariesScope: AbstractProjectFileSearchScope = !javaSourcesScope
        val binaryModuleData = BinaryModuleData.initialize(moduleName, platform)
        val dependencyList = DependencyListForCliModule.build(binaryModuleData, init = dependenciesConfigurator)
        val sessionProvider = externalSessionProvider ?: FirProjectSessionProvider()
        val packagePartProvider = projectEnvironment.getPackagePartProvider(librariesScope)
        val librarySession = FirJvmSessionFactory.createLibrarySession(
            moduleName,
            sessionProvider,
            dependencyList.moduleDataProvider,
            projectEnvironment,
            extensionRegistrars,
            librariesScope,
            packagePartProvider,
            languageVersionSettings,
            predefinedJavaComponents = null,
        )

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
            init = {
//                registerComponent(FirBuiltinSyntheticFunctionInterfaceProvider::class, librarySession.symbolProvider)
                sessionConfigurator()
            },
        )
    }
}
