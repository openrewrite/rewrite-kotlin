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
package org.openrewrite.kotlin.psi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@EqualsAndHashCode
@Data
public class SourceRange {
    /**
     * Source code start position
     */
    int start;

    /**
     * Source code end position, not included.
     */
    int end;

    public boolean isInRange(SourceRange range) {
        return start > range.start && end < range.end;
    }

    public boolean includeRange(SourceRange range) {
        return (!range.equals(this)) && range.start >= start && range.end <= end;
    }

    public boolean covers(int position) {
        return start <= position && position < end;
    }

    @Override
    public String toString() {
        return "[" + start + "," + end + ")";
    }
}
