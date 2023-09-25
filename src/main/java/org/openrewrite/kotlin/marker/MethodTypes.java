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
 */
package org.openrewrite.kotlin.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;

import java.util.List;
import java.util.UUID;

@Value
@With
public class MethodTypes implements Marker {
    UUID id;
    List<JavaType.Method> methodTypes;

    public MethodTypes(UUID id, List<JavaType.Method> methodTypes) {
        this.id = id;
        this.methodTypes = methodTypes;
    }
}
