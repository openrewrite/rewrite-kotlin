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

import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;

import java.util.*;

public class PsiTreePrinter {
    private static final String TAB = "    ";
    private static final String ELEMENT_PREFIX = "\\----";
    private static final char BRANCH_CONTINUE_CHAR = '|';
    private static final char BRANCH_END_CHAR = '\\';
    private static final int CONTENT_MAX_LENGTH = 200;

    private final List<StringBuilder> outputLines;

    protected PsiTreePrinter() {
        outputLines = new ArrayList<>();
    }

    public static String printPsiSkeleton(PsiElement psiElement) {
        PsiTreePrinter treePrinter = new PsiTreePrinter();
        StringBuilder sb = new StringBuilder();
        sb.append("------------").append("\n");
        sb.append("PSI Element Skeleton").append("\n");
        Set<TextRange> covered =  new HashSet<>();
        collectCovered(psiElement, covered);
        treePrinter.printNode(psiElement, 1);
        sb.append(String.join("\n", treePrinter.outputLines));
        return sb.toString();
    }

    public static String printPsiAll(PsiElement psiElement) {
        PsiTreePrinter treePrinter = new PsiTreePrinter();
        StringBuilder sb = new StringBuilder();
        sb.append("------------").append("\n");
        sb.append("PSI Element All").append("\n");
        Set<TextRange> covered =  new HashSet<>();
        collectCovered(psiElement, covered);
        treePrinter.printNode(psiElement, 1, covered, false);
        sb.append(String.join("\n", treePrinter.outputLines));
        return sb.toString();
    }

    public static String printPsiTree(PsiTree psiTree) {
        StringBuilder sb = new StringBuilder();

        // 0. print Tokens
        sb.append("------------").append("\n");
        sb.append("PSI Tokens").append("\n");
        for (int i = 0; i < psiTree.getTokens().size(); i++) {
            PsiToken t = psiTree.getTokens().get(i);
            sb.append(i).append(": ").append(t).append("\n");
        }

        // 1. Source code
        sb.append(printIndexedSourceCode(psiTree.getSource())).append("\n");

        // 2. print AST
        PsiTreePrinter treePrinter = new PsiTreePrinter();
        sb.append("------------").append("\n");
        sb.append("Parsed Full PSI AST").append("\n");
        treePrinter.printNode(psiTree.getRoot(), 1);
        sb.append(String.join("\n", treePrinter.outputLines));
        return sb.toString();
    }

    private void printNode(PsiElement psiElement, int depth) {
        StringBuilder line = new StringBuilder();
        line.append(leftPadding(depth));
        line.append(" ")
            .append(psiElement.getTextRange())
            .append(" | ")
            .append(psiElement.getNode().getElementType())
            .append(" | ")
            .append(psiElement.getClass().getSimpleName())
            .append(" | Text: \"")
            .append(truncate(psiElement.getText()).replace("\n", "\\n"))
            .append("\"");
        connectToLatestSibling(depth);
        outputLines.add(line);

        for (PsiElement childNode : psiElement.getChildren()) {
            printNode(childNode, depth + 1);
        }
    }

    private void printNode(PsiElement psiElement, int depth, Set<TextRange> covered, boolean isExtendedNode) {
        StringBuilder line = new StringBuilder();
        line.append(leftPadding(depth));
        line.append(" ")
            .append(psiElement.getTextRange())
            .append(isExtendedNode ? " [E]" : "")
            .append(" | ")
            .append(psiElement.getNode().getElementType())
            .append(" | ")
            .append(psiElement.getClass().getSimpleName())
            .append(" | Text: \"")
            .append(truncate(psiElement.getText()).replace("\n", "\\n"))
            .append("\"");
        connectToLatestSibling(depth);
        outputLines.add(line);

        for (PsiElement childNode : psiElement.getChildren()) {
            List<PsiElement> preSiblings = new ArrayList<>();
            PsiElement prevSibling = childNode.getPrevSibling();
            while (prevSibling != null && covered.add(prevSibling.getTextRange())) {
                preSiblings.add(prevSibling);
                prevSibling = prevSibling.getPrevSibling();
            }
            Collections.reverse(preSiblings);

            for (PsiElement p : preSiblings) {
                printNode(p, depth + 1, covered, true);
            }

            printNode(childNode, depth + 1, covered, false);

            List<PsiElement> nextSiblings = new ArrayList<>();
            PsiElement nextSibling = childNode.getNextSibling();
            while (nextSibling != null && covered.add(nextSibling.getTextRange())) {
                nextSiblings.add(nextSibling);
                nextSibling = nextSibling.getNextSibling();
            }

            for (PsiElement n : nextSiblings) {
                printNode(n, depth + 1, covered, true);
            }
        }
    }

    private static void collectCovered(PsiElement psiElement, Set<TextRange> covered) {
        covered.add(psiElement.getTextRange());
        for (PsiElement childNode : psiElement.getChildren()) {
            collectCovered(childNode, covered);
        }
    }

    private void printNode(PsiTree.Node node, int depth) {
        StringBuilder line = new StringBuilder();
        line.append(leftPadding(depth));
        line.append(" ")
            .append(node.getRange())
            .append(" | ")
            .append(node.getType())
            .append(" | Text: \"")
            .append(truncate(node.getPsiElement().getText()).replace("\n", "\\n"))
            .append("\"");
        connectToLatestSibling(depth);
        outputLines.add(line);
        for (PsiTree.Node childNode : node.getChildNodes()) {
            printNode(childNode, depth + 1);
        }
    }

    public static String printIndexedSourceCode(String sourceCode) {
        int count = 0;
        String[] lines = sourceCode.split("\n");
        StringBuilder sb = new StringBuilder();
        sb.append("------------").append("\n");
        sb.append("Source code with index:").append("\n\n");
        Queue<Integer> digits = new ArrayDeque<>();

        for (String line : lines) {
            StringBuilder spacesSb = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                if (count % 10 == 0) {
                    String numStr = Integer.toString(count);
                    for (int j = 0; j < numStr.length(); j++) {
                        char c = numStr.charAt(j);
                        int digit = Character.getNumericValue(c);
                        digits.add(digit);
                    }
                }

                if (!digits.isEmpty()) {
                    spacesSb.append(digits.poll()) ;
                } else {
                    spacesSb.append(" ");
                }

                count++;
            }

            sb.append(line)
                .append("\n")
                .append(spacesSb)
                .append("\n");
            count++;
        }
        return sb.toString();
    }

    /**
     * print left padding for a line
     * @param depth, depth starts from 0 (the root)
     */
    private static String leftPadding(int depth) {
        StringBuilder sb = new StringBuilder();
        int tabCount = depth - 1;
        if (tabCount > 0) {
            sb.append(String.join("", Collections.nCopies(tabCount, TAB)));
        }
        // only root has not prefix
        if (depth > 0) {
            sb.append(ELEMENT_PREFIX);
        }
        return sb.toString();
    }

    /**
     * Print a vertical line that connects the current element to the latest sibling.
     * @param depth current element depth
     */
    private void connectToLatestSibling(int depth) {
        if (depth <= 1) {
            return;
        }

        int pos = (depth - 1) * TAB.length();
        for (int i = outputLines.size() - 1; i > 0; i--) {
            StringBuilder line = outputLines.get(i);
            if (pos >= line.length()) {
                break;
            }

            if (line.charAt(pos) != ' ') {
                if (line.charAt(pos) == BRANCH_END_CHAR) {
                    line.setCharAt(pos, BRANCH_CONTINUE_CHAR);
                }
                break;
            }
            line.setCharAt(pos, BRANCH_CONTINUE_CHAR);
        }
    }

    private String truncate(String content) {
        if (content.length() > CONTENT_MAX_LENGTH) {
            return content.substring(0, CONTENT_MAX_LENGTH - 3) + "...";
        }
        return content;
    }
}
