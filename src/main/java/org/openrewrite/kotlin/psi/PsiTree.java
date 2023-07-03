package org.openrewrite.kotlin.psi;

import lombok.Data;
import org.jetbrains.kotlin.KtSourceElement;
import org.jetbrains.kotlin.com.intellij.lang.LighterASTNode;
import org.jetbrains.kotlin.com.intellij.lang.impl.TokenSequence;
import org.jetbrains.kotlin.com.intellij.openapi.util.Ref;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KotlinLexer;

import java.util.*;

@Data
public class PsiTree {
    private static final String parenthesisType = "PARENTHESIS";
    private List<Token> tokens;
    private Map<Integer, Token> tokenOffSetMap;


    private String source;
    private Node root;
    // private Map<SourceRange, Node> nodesMap;

    public PsiTree(String sourceCode) {
        source = sourceCode;
        tokenize(source);
        root = null;
    }

    private static class NodeComparator implements Comparator<Node> {
        @Override
        public int compare(Node node1, Node node2) {
            int start1 = node1.getRange().getStart();
            int end1 = node1.getRange().getEnd();
            int start2 = node2.getRange().getStart();
            int end2 = node2.getRange().getEnd();

            int startComparison = Integer.compare(start1, start2);
            if (startComparison != 0) {
                return startComparison;
            }
            return Integer.compare(end1, end2);
        }
    }

    @Data
    public static class Node {
        public Node(SourceRange range, String type, Node parent) {
            this.range = range;
            this.type = type;
            childNodes = new ArrayList<>();
            // childNodes = new PriorityQueue<>(new NodeComparator());
            this.parent = parent;
        }

        @Override
        public String toString() {
            return range.toString() + " type:" + type;
        }

        SourceRange range;

        /**
         * Node type, From PSI AST tree
         */
        String type;

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
            null);

        Ref<LighterASTNode[]> childNodes = new Ref<>();
        ktSourceElement.getTreeStructure().getChildren(astNode, childNodes);
        for (LighterASTNode childASTNode : childNodes.get()) {
            if (childASTNode != null) {
                int realSourceOffset = childASTNode.getStartOffset() - nodeOffset + sourceOffset;
                int realSourceEnd = childASTNode.getEndOffset() - nodeOffset + sourceOffset;
                Node childNode = new Node(new SourceRange(realSourceOffset, realSourceEnd),
                    childASTNode.getTokenType().toString(),
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
                // Something is optimised and skipped by compiler, and we need to preserve
                SourceRange leftGap = new SourceRange(parentNode.getRange().getStart(), node.getRange().getStart());
                SourceRange rightGap = new SourceRange(node.getRange().getEnd(), parentNode.getRange().getEnd());
                int leftGapTokenStartIndex = findTokenIndex(leftGap.getStart());
                int leftGapTokenEndIndex = findTokenIndex(leftGap.getEnd() - 1);
                int rightGapTokenStartIndex = findTokenIndex(rightGap.getStart());
                int rightGapTokenEndIndex = findTokenIndex(rightGap.getEnd() - 1);;
                int leftIndex = leftGapTokenStartIndex;
                int rightIndex = rightGapTokenEndIndex;


                while (leftIndex <= leftGapTokenEndIndex) {
                    Token left = tokens.get(leftIndex);
                    if (left.getType().equals("LPAR")) {
                        while (!tokens.get(rightIndex).getType().equals("RPAR") && rightIndex > rightGapTokenStartIndex) {
                            // todo, might need to handle skipped whitespaces
                            rightIndex--;
                        }

                        Token right = tokens.get(rightIndex);

                        Node newParNode = new Node(new SourceRange(left.getRange().getStart(), right.getRange().getEnd()), parenthesisType, parentNode);
                        newParNode.getChildNodes().add(new Node(left.getRange(), left.getType(), newParNode));
                        newParNode.getChildNodes().add(new Node(right.getRange(), right.getType(), newParNode));

                        parentNode.getChildNodes().add(newParNode);
                        Collections.sort(parentNode.getChildNodes(), new NodeComparator());

                        parentNode = newParNode;
                        rightIndex--;
                    } else if (left.getType().equals("WHITE_SPACE")) {
                        // continue, might need some handling here
                    } else {
                        throw new RuntimeException("Unsupported missing PSI tree element! Token = " + left);
                    }

                    leftIndex++;
                }

                parentNode.getChildNodes().add(node);
                Collections.sort(parentNode.getChildNodes(), new NodeComparator());

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

    private int findTokenIndex(int position) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getRange().covers(position)) {
                return i;
            }
        }
        return -1;
    }

    // Debug purpose only, not invoked in product.
    private static void printASTNodes(KtSourceElement source) {
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

    // Debug purpose only, not invoked in product.
    private static void printNode(LighterASTNode node, int sourceOffset, int nodeOffset, String prefix) {
        int realSourceOffset = node.getStartOffset() - nodeOffset + sourceOffset;
        int realSourceEnd = node.getEndOffset() - nodeOffset + sourceOffset;
        System.out.println(prefix + "AST Node: [" + node.getStartOffset() + ",  " + node.getEndOffset() + ") " +
                           "SourcePos: [ " + realSourceOffset + ", " + realSourceEnd + ") " +
                           " | type: " + node.getTokenType() + " | text: " + node.toString().replace("\n", "\\n"));
    }

    private void tokenize(String sourceCode)
    {
        tokens = new ArrayList<>();
        tokenOffSetMap = new HashMap<>();

        KotlinLexer kotlinLexer = new KotlinLexer();
        TokenSequence tokenSequence = TokenSequence.performLexing(sourceCode, kotlinLexer);
        int tokenCount = tokenSequence.getTokenCount();


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
            tokens.add(token);
            tokenOffSetMap.put(token.getRange().getStart(), token);
            preToken = token;
        }

        if (preToken != null) {
            preToken.setText(sourceCode.substring(preToken.getRange().getStart()));
            preToken.getRange().setEnd(sourceCode.length());
        }
    }

    // todo
    public String printSource() {
        // todo, print all leaf node and supposed to be equal to the source
        return "";
    }


}
