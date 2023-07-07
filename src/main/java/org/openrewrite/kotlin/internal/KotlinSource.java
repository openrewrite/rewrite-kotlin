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
import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiElementVisitor;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.psi.KtElement;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;

import java.util.*;

@Getter
public class KotlinSource {
    Parser.Input input;
    Map<Integer, ASTNode> nodes; // Maybe replace with PsiTree?
    PsiTree psiTree;

    @Setter
    FirFile firFile;

    public KotlinSource(Parser.Input input,
                        @Nullable PsiFile psiFile) {
        this.input = input;
        this.nodes = map(psiFile);
        this.psiTree = new PsiTree(psiFile, input.getSource(new InMemoryExecutionContext()).readFully());
    }

    // Map the PsiFile ahead of time so that the Disposable may be disposed early and free up memory.
    private Map<Integer, ASTNode> map(@Nullable PsiFile psiFile) {
        Map<Integer, ASTNode> result = new HashMap<>();
        if (psiFile == null) {
            return result;
        }

        Set<PsiElement> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        PsiElementVisitor v = new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (!visited.add(element)) {
                    return;
                }

                result.put(element.getTextRange().getStartOffset(), element.getNode());

                for (PsiElement child : element.getChildren()) {
                    if (child instanceof KtElement) {
                        visitElement(child);
                    }
                }

                if (element.getNextSibling() != null) {
                    visitElement(element.getNextSibling());
                }
            }
        };
        v.visitElement(psiFile);
        return result;
    }
}
