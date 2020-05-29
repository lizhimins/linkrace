package com.alirace.util;

import java.util.*;

public class AhoCorasickAutomation {

    /*本示例中的AC自动机只处理英文类型的字符串，所以数组的长度是128*/
    private static final int ASCII = 128;

    /*AC自动机的根结点，根结点不存储任何字符信息*/
    private static Node root = new Node();

    /*待查找的目标字符串集合*/
    private static List<String> target = new ArrayList<String>();

    /*内部静态类，用于表示AC自动机的每个结点，在每个结点中我们并没有存储该结点对应的字符*/
    private static class Node {
        /*如果该结点是一个终点，即，从根结点到此结点表示了一个目标字符串，则str != null, 且str就表示该字符串*/
        String str;
        /*ASCII == 128, 所以这里相当于128叉树*/
        Node[] table = new Node[ASCII];
        /*当前结点的孩子结点不能匹配文本串中的某个字符时，下一个应该查找的结点*/
        Node fail;

        public Boolean isWord() {
            return str != null;
        }
    }

    static {
        target.add("|http.status_code=");
        target.add("&http.status_code=");
        target.add("|error=1&");
        target.add("&error=1&");
        target.add("|error=1\n");
        target.add("&error=1\n");
        buildTrieTree();
        build_AC_FromTrie();
    }

    /*由目标字符串构建Trie树*/
    private static void buildTrieTree() {
        for (String targetStr : target) {
            Node curr = root;
            for (int i = 0; i < targetStr.length(); i++) {
                byte ch = (byte) targetStr.charAt(i);
                if (curr.table[ch] == null) {
                    curr.table[ch] = new Node();
                }
                curr = curr.table[ch];
            }
            /*将每个目标字符串的最后一个字符对应的结点变成终点*/
            curr.str = targetStr;
        }
    }

    /*由Trie树构建AC自动机，本质是一个自动机，相当于构建KMP算法的next数组*/
    private static void build_AC_FromTrie() {
        /*广度优先遍历所使用的队列*/
        LinkedList<Node> queue = new LinkedList<Node>();
        /*单独处理根结点的所有孩子结点*/
        for (Node x : root.table) {
            if (x != null) {
                /*根结点的所有孩子结点的fail都指向根结点*/
                x.fail = root;
                queue.addLast(x);
                /*所有根结点的孩子结点入列*/
            }
        }
        while (!queue.isEmpty()) {
            /*确定出列结点的所有孩子结点的fail的指向*/
            Node p = queue.removeFirst();
            for (int i = 0; i < p.table.length; i++) {
                if (p.table[i] != null) {
                    /*孩子结点入列*/
                    queue.addLast(p.table[i]);
                    /*从p.fail开始找起*/
                    Node failTo = p.fail;
                    while (true) {
                        /*说明找到了根结点还没有找到*/
                        if (failTo == null) {
                            p.table[i].fail = root;
                            break;
                        }
                        /*说明有公共前缀*/
                        if (failTo.table[i] != null) {
                            p.table[i].fail = failTo.table[i];
                            break;
                        } else {
                            /*继续向上寻找*/
                            failTo = failTo.fail;
                        }
                    }
                }
            }
        }
    }

    /*在文本串中查找所有的目标字符串*/
    public HashMap<String, List<Integer>> find(String text) {
        /*创建一个表示存储结果的对象*/
        HashMap<String, List<Integer>> result = new HashMap();
        for (String s : target) {
            result.put(s, new LinkedList<Integer>());
        }
        Node curr = root;
        int i = 0;
        while (i < text.length()) {
            /*文本串中的字符*/
            byte ch = (byte) (int) text.charAt(i);
            /*文本串中的字符和AC自动机中的字符进行比较*/
            if (curr.table[ch] != null) {
                /*若相等，自动机进入下一状态*/
                curr = curr.table[ch];
                if (curr.isWord()) {
                    result.get(curr.str).add(i - curr.str.length() + 1);
                }
                /*这里很容易被忽视，因为一个目标串的中间某部分字符串可能正好包含另一个目标字符串，
                 * 即使当前结点不表示一个目标字符串的终点，但到当前结点为止可能恰好包含了一个字符串*/
                if (curr.fail != null && curr.fail.isWord()) {
                    result.get(curr.fail.str).add(i - curr.fail.str.length() + 1);
                }
                /*索引自增，指向下一个文本串中的字符*/
                i++;
            } else {
                /*若不等，找到下一个应该比较的状态*/
                curr = curr.fail;
                /*到根结点还未找到，说明文本串中以ch作为结束的字符片段不是任何目标字符串的前缀，
                 * 状态机重置，比较下一个字符*/
                if (curr == null) {
                    curr = root;
                    i++;
                }
            }
        }
        return result;
    }

    /**
     * 正数 正确
     * 0 粘包
     * 负数 错误
     * 绝对值为 \n 的 offset
     */
    /*在文本串中查找所有的目标字符串*/
    public static int find(byte[] bytes, int offset, int maxOffset) {
        boolean flag = true;

        Node curr = root;
        int i = offset;
        while (bytes[i] != (byte) '\n') {
            if (i == maxOffset) {
                return 0;
            }
            /*文本串中的字符*/
            byte ch = bytes[i];
            /*文本串中的字符和AC自动机中的字符进行比较*/
            if (curr.table[ch] != null) {
                /*若相等，自动机进入下一状态*/
                curr = curr.table[ch];
                if (curr.isWord()) {
                    // result.get(curr.str).add(i - curr.str.length() + 1);
                    int index = i - curr.str.length() + 1;
                    // System.out.println("Find: " + curr.str + " " + index);
                    if (curr.str.charAt(1) == 'h') {
                        if (bytes[i + 1] == (byte) '2'
                                && bytes[i + 2] == (byte) '0'
                                && bytes[i + 3] == (byte) '0'
                                && (bytes[i + 4] == (byte) '&' || bytes[i + 4] == (byte) '\n')) {
                            // 完全匹配
                        } else {
                            flag = false;
                        }
                    }
                    if (curr.str.charAt(1) == 'e') {
                        flag = false;
                    }
                }
                /*这里很容易被忽视，因为一个目标串的中间某部分字符串可能正好包含另一个目标字符串，
                 * 即使当前结点不表示一个目标字符串的终点，但到当前结点为止可能恰好包含了一个字符串*/
                if (curr.fail != null && curr.fail.isWord()) {
                    // result.get(curr.fail.str).add(i - curr.fail.str.length() + 1);
                    // System.out.println("Find: " + curr.str + " " + (i - curr.fail.str.length() + 1));

                    int index = i - i - curr.fail.str.length() + 1;
                    // System.out.println("Find: " + curr.str + " " + index);
                    if (curr.str.charAt(1) == 'h') {
                        if (bytes[i + 1] == (byte) '2'
                                && bytes[i + 2] == (byte) '0'
                                && bytes[i + 3] == (byte) '0'
                                && (bytes[i + 4] == (byte) '&' || bytes[i + 4] == (byte) '\n')) {
                            // 完全匹配
                        } else {
                            flag = false;
                        }
                    }
                    if (curr.str.charAt(1) == 'e') {
                        flag = false;
                    }
                }
                /*索引自增，指向下一个文本串中的字符*/
                i++;
            } else {
                /*若不等，找到下一个应该比较的状态*/
                curr = curr.fail;
                /*到根结点还未找到，说明文本串中以ch作为结束的字符片段不是任何目标字符串的前缀，
                 * 状态机重置，比较下一个字符*/
                if (curr == null) {
                    curr = root;
                    i++;
                }
            }
        }
        i++;
        return flag ? i : -i;
    }
}
