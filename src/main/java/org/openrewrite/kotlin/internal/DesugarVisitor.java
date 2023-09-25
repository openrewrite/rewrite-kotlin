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
package org.openrewrite.kotlin.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.marker.MethodTypes;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Desugar Kotlin code to support data flow analysis.
 */
public class DesugarVisitor extends KotlinVisitor<ExecutionContext> {

    @Nullable
    private static J.MethodInvocation containsMethodCallTemplate = null;

    @Nullable
    private static J.MethodInvocation notMethodCallTemplate = null;

    @Nullable
    private static J.MethodInvocation rangeToMethodCallTemplate = null;

    @Override
    public J visitBinary(K.Binary binary, ExecutionContext ctx) {
        K.Binary kb = (K.Binary ) super.visitBinary(binary, ctx);

        List<JavaType.Method> methodTypes = binary.getMarkers().findFirst(MethodTypes.class)
                .map(MethodTypes::getMethodTypes)
                .orElse(Collections.emptyList());

        if (kb.getOperator() == K.Binary.Type.NotContains) {
            JavaType.Method containsType = findMethodType(methodTypes, "contains");
            JavaType.Method notType = findMethodType(methodTypes, "not");
            if (containsType == null || notType == null) {
                throw new IllegalArgumentException("Didn't find the contains() or not() method type from FIR");
            }

            J.MethodInvocation containsCall = getContainsMethodCallTemplate();
            J.MethodInvocation notCall = getNotMethodCallTemplate();
            containsCall = containsCall.withArguments(Collections.singletonList(kb.getLeft()))
                    .withSelect(maybeParenthesizeSelect(kb.getRight()))
                    .withMethodType(containsType);

            notCall = notCall.withSelect(containsCall).withPrefix(Space.EMPTY)
                    .withMethodType(notType);
            return notCall;
        } else if (kb.getOperator() == K.Binary.Type.RangeTo) {
            JavaType.Method rangeToType = findMethodType(methodTypes, "rangeTo");
            if (rangeToType == null) {
                throw new IllegalArgumentException("Didn't find the rangeTo() method type from FIR");
            }

            J.MethodInvocation rangeTo = getRangeToMethodCallTemplate();
            return rangeTo.withSelect(maybeParenthesizeSelect(kb.getLeft()))
                    .withArguments(Collections.singletonList(kb.getRight().withPrefix(Space.EMPTY)))
                    .withPrefix(binary.getPrefix())
                    .withMethodType(rangeToType);
        }

        return autoFormat(kb, ctx).withPrefix(Space.EMPTY) ;
    }

    @Nullable
    private JavaType.Method findMethodType(List<JavaType.Method> types, String name) {
        return types.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public J visitCompilationUnit(K.CompilationUnit cu, ExecutionContext executionContext) {
        cu = (K.CompilationUnit) super.visitCompilationUnit(cu, executionContext);
        return cu;
    }

    private static Expression maybeParenthesizeSelect(Expression select) {
        if (select instanceof K.Binary || select instanceof J.Binary) {
            select = new J.Parentheses<>(Tree.randomId(), Space.EMPTY, Markers.EMPTY, new JRightPadded<>(select, Space.EMPTY, Markers.EMPTY));
        }
        return select;
    }

    private static J.MethodInvocation buildMethodInvocationTemplate(String sourceCode, String methodName) {
        K.CompilationUnit kcu = KotlinParser.builder().build()
                .parse(sourceCode)
                .map(K.CompilationUnit.class::cast)
                .findFirst()
                .get();

        return new KotlinVisitor<AtomicReference<J.MethodInvocation>>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, AtomicReference<J.MethodInvocation> target) {
                if (method.getSimpleName().equals(methodName)) {
                    target.set(method);
                }
                return method;
            }
        }.reduce(kcu, new AtomicReference<>()).get();
    }

    private static J.MethodInvocation getContainsMethodCallTemplate() {
        if (containsMethodCallTemplate == null) {
            containsMethodCallTemplate = buildMethodInvocationTemplate("val a ='A'.rangeTo('Z').contains('X')", "contains");
        }
        return containsMethodCallTemplate;
    }

    private static J.MethodInvocation getNotMethodCallTemplate() {
        if (notMethodCallTemplate == null) {
            notMethodCallTemplate = buildMethodInvocationTemplate("val a=true.not()", "not");
        }
        return notMethodCallTemplate;
    }

    private static J.MethodInvocation getRangeToMethodCallTemplate() {
        if (rangeToMethodCallTemplate == null) {
            rangeToMethodCallTemplate = buildMethodInvocationTemplate("val a ='A'.rangeTo('Z')", "rangeTo");
        }
        return rangeToMethodCallTemplate;
    }
}
