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

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.psi.KtFile;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;

import java.util.*;

@Getter
public class KotlinSource {
    Parser.Input input;
    Map<Integer, Node> nodes;

    @Setter
    FirFile firFile;

    public KotlinSource(Parser.Input input,
                        @Nullable PsiFile psiFile) {
        this.input = input;
        this.nodes = map(psiFile);
    }

    // Map the PsiFile ahead of time so that the Disposable may be disposed early and free up memory.
    private Map<Integer, Node> map(@Nullable PsiFile psiFile) {
        Map<Integer, Node> result = new HashMap<>();
        if (psiFile == null) {
            return result;
        }

        String source = input.getSource(new InMemoryExecutionContext()).readFully();
        Set<PsiElement> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        PsiElementVisitor v = new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (!(element instanceof KtFile) && element.getTextLength() != 0) {
                    // Set the node at the cursor to the last visited element.
                    //noinspection UnstableApiUsage
                    result.put(element.getTextRange().getStartOffset(), new Node(element.getTextRange().getEndOffset(),
                            element.getNode().getTreeParent().getElementType().getDebugName(),
                            element.getNode().getElementType().getDebugName()));
                    assert source.substring(element.getTextRange().getStartOffset(), element.getTextRange().getEndOffset()).equals(element.getText());
                }

                for (PsiElement child : element.getChildren()) {
                    if (visited.add(child)) {
                        visitElement(child);
                    }
                }

                if (element.getNextSibling() != null && visited.add(element.getNextSibling())) {
                    visitElement(element.getNextSibling());
                }
            }
        };
        v.visitElement(psiFile);
        return result;
    }

    @Value
    public static class Node {
        int endOffset;
        String parentName;
        String name;
    }
}