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
package org.openrewrite.kotlin;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JvmParser;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.kotlin.internal.template.KotlinSubstitutions;
import org.openrewrite.kotlin.internal.template.KotlinTemplateParser;

import java.util.Set;
import java.util.function.Consumer;

public class KotlinTemplate extends JavaTemplate {
    private KotlinTemplate(boolean contextSensitive, JvmParser.Builder<?,?> parser, String code, Set<String> imports, Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate) {
        super(code, StringUtils.countOccurrences(code, "#{"), onAfterVariableSubstitution, new KotlinTemplateParser(contextSensitive, parser, onAfterVariableSubstitution, onBeforeParseTemplate, imports));
    }

    @Override
    protected Substitutions substitutions(Object[] parameters) {
        return new KotlinSubstitutions(getCode(), parameters);
    }

    public static Builder builder(String code) {
        return new Builder(code);
    }

    @SuppressWarnings("unused")
    public static class Builder extends JavaTemplate.Builder {

        Builder(String code) {
            super(code);
            parser = KotlinParser.builder();
        }

        @Override
        public KotlinTemplate build() {
            return new KotlinTemplate(contextSensitive, parser, code, imports,
                    onAfterVariableSubstitution, onBeforeParseTemplate);
        }
    }
}
