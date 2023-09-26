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
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.marker.MethodTypes;
import org.openrewrite.kotlin.marker.OperatorOverload;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.kotlin.tree.K.Binary.Type.*;

public class DesugarVisitor<P> extends KotlinVisitor<P> {

    @Override
    public J visitUnary(J.Unary unary, P p) {
        // TODO: apply appropriate whitespace from unary to J.MI based on Unary$Type
        return new J.MethodInvocation(
                randomId(),
                unary.getPrefix(),
                Markers.EMPTY.addIfAbsent(new OperatorOverload(randomId())),
                JRightPadded.build((Expression) Objects.requireNonNull(visit(unary.getExpression(), p))),
                null,
                methodIdentifier(methodName(unary), null), // TODO: ADD type
                JContainer.build(EMPTY, singletonList(JRightPadded.build(new J.Empty(randomId(), EMPTY, Markers.EMPTY))), Markers.EMPTY),
                null // TODO: ADD type
        );
    }

    @Override
    public J visitBinary(K.Binary binary, P p) {
        // TODO: set arguments and select based on K.Binary$Type and apply appropriate whitespace for idempotent print.
        // Every OP other than contains and !in should be the same Binary.L => MI.Select and Binary.R to MI.Args
        // Contains switches the LEFT and RIGHT, and !in adds a not() as the first MI. And contains becomes the SELECT.
        if (binary.getOperator() == NotContains) {
            System.out.println();
        }

        if (binary.getType() != null && binary.getType() instanceof JavaType.Method) {
            return new J.MethodInvocation(
                    randomId(),
                    binary.getPrefix(),
                    Markers.EMPTY.addIfAbsent(new OperatorOverload(randomId())),
                    JRightPadded.build((Expression) visitNonNull(binary.getLeft(), p)),
                    null,
                    methodIdentifier(methodName(binary), (JavaType.Method) binary.getType()),
                    mapContainer(binary, p),
                    (JavaType.Method) binary.getType()
            );
        }
        // Use the K.Binary to preserve printing if the type is null.
        return super.visitBinary(binary, p);
    }

    @Nullable
    private JavaType.Method findMethodType(List<JavaType.Method> types, String name) {
        return types.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
    }

    private JContainer<Expression> mapContainer(K.Binary binary, P p) {
        List<JRightPadded<Expression>> args;
        switch (binary.getOperator()) {
            case RangeTo:
            case RangeUntil:
                args = new ArrayList<>(1);
                args.add(JRightPadded.build((Expression) visitNonNull(binary.getRight(), p)));
                break;
            default:
                throw new UnsupportedOperationException("Binary operator " + binary + " is not supported");
        }
        return JContainer.build(binary.getPadding().getOperator().getBefore(), args, Markers.EMPTY);
    }

    private String methodName(K.Binary binary) {
        switch (binary.getOperator()) {
            case Plus:
                return "plus";
            case Minus:
                return "minus";
            case Mul:
                return "times";
            case Div:
                return "div";
            case RangeTo:
                return "rangeTo";
            case RangeUntil:
                return "rangeUntil";
            default:
                throw new UnsupportedOperationException("Binary operator " + binary + " is not supported");
        }
    }

    private String methodName(J.Unary unary) {
        switch (unary.getOperator()) {
            case PostIncrement:
                return "inc";
            case PostDecrement:
                return "dec";
            case Positive:
                return "unaryPlus";
            case Negative:
                return "unaryMinus";
            case Not:
                return "not";
            default:
                throw new UnsupportedOperationException("Unary operator " + unary.getOperator() + " is not supported");
        }
    }

    private J.Identifier methodIdentifier(String name, JavaType.Method methodType) {
        return new J.Identifier(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                emptyList(),
                name,
                methodType,
                null
        );
    }
}
