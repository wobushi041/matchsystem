package com.wobushi041.matchsystem.utils;

import java.util.List;
import java.util.Objects;

public class AlgorithmUtils {
    /**
     * 计算两个用户的标签列表之间的最小编辑距离。
     * @param tagList1 第一个用户的标签列表。
     * @param tagList2 第二个用户的标签列表。
     * @return 两个标签列表之间的最小编辑距离。
     */
    public static int minDistance(List<String> tagList1, List<String> tagList2) {


        if (tagList1 == null || tagList2 == null) {
            // 如果任一列表为null，返回最大可能距离（或根据业务需求调整）
            return (tagList1 == null ? 0 : tagList1.size()) +
                    (tagList2 == null ? 0 : tagList2.size());
        }
        int n = tagList1.size();
        int m = tagList2.size();

        // 处理任一标签列表为空的情况
        if (n * m == 0) {
            return n + m;
        }

        int[][] d = new int[n + 1][m + 1];

        // 初始化边界条件，单个列表到空列表的编辑距离
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }

        // 使用动态规划算法填充表格
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1; // 插入操作
                int down = d[i][j - 1] + 1; // 删除操作
                int left_down = d[i - 1][j - 1]; // 替换操作
                if (!Objects.equals(tagList1.get(i - 1), tagList2.get(j - 1))) {
                    left_down += 1; // 不同字符需替换
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }

    /**
     * 计算两个单词或字符串之间的最小编辑距离。
     * @param word1 第一个字符串。
     * @param word2 第二个字符串。
     * @return 两个字符串之间的最小编辑距离。
     */
    public static int minDistance(String word1, String word2) {
        int n = word1.length();
        int m = word2.length();

        // 处理任一字符串为空的情况
        if (n * m == 0) {
            return n + m;
        }

        int[][] d = new int[n + 1][m + 1];

        // 初始化边界条件
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }

        // 动态规划解决问题
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1; // 插入
                int down = d[i][j - 1] + 1; // 删除
                int left_down = d[i - 1][j - 1]; // 替换
                if (word1.charAt(i - 1) != word2.charAt(j - 1)) {
                    left_down += 1; // 需要替换
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }

}

