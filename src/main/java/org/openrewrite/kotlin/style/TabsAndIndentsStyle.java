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
package org.openrewrite.kotlin.style;

import lombok.AccessLevel;
import lombok.Value;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.kotlin.KotlinStyle;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Value
@With
public class TabsAndIndentsStyle implements KotlinStyle {
    Boolean useTabCharacter;
    Integer tabSize;
    Integer indentSize;
    Integer continuationIndent;
    Boolean keepIndentsOnEmptyLines;

    FunctionDeclarationParameters functionDeclarationParameters;

    @Value
    @With
    public static class FunctionDeclarationParameters {
        Boolean alignWhenMultiple;
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(IntelliJ.tabsAndIndents(), this);
    }
}
