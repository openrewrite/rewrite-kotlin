package org.openrewrite.kotlin.psi;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.kotlin.KtSourceElement;
import org.jetbrains.kotlin.com.intellij.lang.LighterASTNode;
import org.jetbrains.kotlin.com.intellij.lang.impl.TokenSequence;
import org.jetbrains.kotlin.com.intellij.openapi.util.Ref;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KotlinLexer;

import java.util.ArrayList;
import java.util.List;

@Data
public class PsiTree {
    private List<Token> tokens;
    private String source;
    private Node root;
    // private Map<SourceRange, Node> nodesMap;

    public PsiTree(String sourceCode) {
        source = sourceCode;
        tokens = tokenize(source);
        root = null;
    }

    @AllArgsConstructor
    @Data
    public static class Node {
        @Override
        public String toString() {
            return range.toString() + " type:" + type;
        }

        // ? do we need this?
        // UUID id;
        SourceRange range;

        /**
         * Node type, From PSI AST tree
         */
        String type;

        // tobe removed. just for debug purpose,
        // String text;
        List<Node> childNodes;

        Node parent;

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            Node other = (Node) obj;
            return range.equals(other.range) && type.equals(other.getType());
        }
    }

    public void parseFirElementSource(KtSourceElement ktSourceElement) {
        LighterASTNode astNode = ktSourceElement.getLighterASTNode();
        int sourceOffset = ktSourceElement.getStartOffset();
        int nodeOffset = astNode.getStartOffset();
        printASTNodes(ktSourceElement);

        Node node = new Node(new SourceRange(ktSourceElement.getStartOffset(), ktSourceElement.getEndOffset()),
            astNode.getTokenType().toString(),
            new ArrayList<>(),
            null);

        Ref<LighterASTNode[]> childNodes = new Ref<>();
        ktSourceElement.getTreeStructure().getChildren(astNode, childNodes);
        for (LighterASTNode childASTNode : childNodes.get()) {
            if (childASTNode != null) {
                int realSourceOffset = childASTNode.getStartOffset() - nodeOffset + sourceOffset;
                int realSourceEnd = childASTNode.getEndOffset() - nodeOffset + sourceOffset;
                Node childNode = new Node(new SourceRange(realSourceOffset, realSourceEnd),
                    childASTNode.getTokenType().toString(),
                    new ArrayList<>(),
                    node);
                node.getChildNodes().add(childNode);
            }
        }
        addNode(node);
    }

    private void addNode(Node node) {
        if (root == null) {
            // the first node is supposed to be the root and has FILE type
            if (!node.getType().equals("FILE")) {
                throw new RuntimeException("The first node of PSI tree is expected to be FILE type");
            }
            root = node;
        } else {
            Node parentNode = findParentNode(node, root);
            if (parentNode == null) {
                throw new RuntimeException("Not found PSI parent node, something wrong!");
            }

            if (parentNode.equals(node)) {
                parentNode.getChildNodes().addAll(node.getChildNodes());
            } else {
                parentNode.getChildNodes().add(node);
                node.setParent(parentNode);
            }
        }
    }

    private static Node findParentNode(Node node, Node rootNode) {
        if (rootNode.getRange().equals(node.getRange()) &&
            rootNode.getType().equals(node.getType())) {
            return rootNode;
        }

        if (rootNode.getRange().equals(node.getRange()) ||
            rootNode.getRange().includeRange(node.getRange())) {
            if (!rootNode.getChildNodes().isEmpty()) {
                for (Node childNode : rootNode.getChildNodes()) {
                    Node n = findParentNode(node, childNode);
                    if (n != null) {
                        return n;
                    }
                }
            }
            return rootNode;
        }
        return null;
    }

    // Debug purpose only, not invoked.
    private static <T> void printASTNodes(KtSourceElement source) {
        System.out.println("------");
        LighterASTNode node = source.getLighterASTNode();

        int sourceOffset = source.getStartOffset();
        int nodeOffset = node.getStartOffset();
        System.out.println("Source range : [" + source.getStartOffset() + ", " + source.getEndOffset() + "]");

        printNode(node, sourceOffset, nodeOffset, "");
        // System.out.println("AST Node: [" + node.getStartOffset() + ",  " + node.getEndOffset() + ", " + node.getTokenType() + " ]text: " + node.toString().replace("\n", "\\n"));
        System.out.println("    - Child nodes");

        Ref<LighterASTNode[]> childNodes = new Ref<>();
        source.getTreeStructure().getChildren(node, childNodes);
        for (LighterASTNode childNode : childNodes.get()) {
            if (childNode != null) {
                printNode(childNode, sourceOffset, nodeOffset, "    ");
            }
        }
    }

    // Debug purpose only, not invoked.
    private static void printNode(LighterASTNode node, int sourceOffset, int nodeOffset, String prefix) {
        int realSourceOffset = node.getStartOffset() - nodeOffset + sourceOffset;
        int realSourceEnd = node.getEndOffset() - nodeOffset + sourceOffset;
        System.out.println(prefix + "AST Node: [" + node.getStartOffset() + ",  " + node.getEndOffset() + ") " +
                           "SourcePos: [ " + realSourceOffset + ", " + realSourceEnd + ") " +
                           " | type: " + node.getTokenType() + " | text: " + node.toString().replace("\n", "\\n"));
    }

    private static List<Token> tokenize(String sourceCode)
    {
        KotlinLexer kotlinLexer = new KotlinLexer();
        TokenSequence tokenSequence = TokenSequence.performLexing(sourceCode, kotlinLexer);
        int tokenCount = tokenSequence.getTokenCount();
        List<Token> tokenList = new ArrayList<>();
        Token preToken = null;
        for (int i = 0; i < tokenCount; i++) {
            int tokenStart = tokenSequence.getTokenStart(i);
            IElementType iElementType = tokenSequence.getTokenType(i);
            Token token = new Token();
            token.setRange(new SourceRange(tokenStart, Integer.MAX_VALUE));
            token.setType(iElementType.toString());

            if (preToken != null) {
                preToken.setText(sourceCode.substring(preToken.getRange().getStart(), tokenStart));
                preToken.getRange().setEnd(tokenStart);
            }
            tokenList.add(token);
            preToken = token;
        }

        if (preToken != null) {
            preToken.setText(sourceCode.substring(preToken.getRange().getStart()));
            preToken.getRange().setEnd(sourceCode.length());
        }

        return tokenList;
    }

    // todo
    public String printSource() {
        return "";
    }


}
