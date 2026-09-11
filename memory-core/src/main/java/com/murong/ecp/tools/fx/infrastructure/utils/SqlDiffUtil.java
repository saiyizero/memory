package com.murong.ecp.tools.fx.infrastructure.utils;

import java.util.*;

/**
 * SQL比对工具类
 * 使用智能算法进行SQL语句比对
 */
public class SqlDiffUtil {

    /**
     * SQL比对结果
     */
    public static class SqlDiffResult {
        private List<DiffLine> localLines = new ArrayList<>();
        private List<DiffLine> remoteLines = new ArrayList<>();
        private List<DiffDetail> differences = new ArrayList<>();

        public List<DiffLine> getLocalLines() { return localLines; }
        public List<DiffLine> getRemoteLines() { return remoteLines; }
        public List<DiffDetail> getDifferences() { return differences; }
    }

    /**
     * 差异行
     */
    public static class DiffLine {
        private String content;
        private DiffType type;
        private int lineNumber;
        private String normalizedContent;

        public DiffLine(String content, DiffType type, int lineNumber) {
            this.content = content;
            this.type = type;
            this.lineNumber = lineNumber;
            this.normalizedContent = normalizeSqlLine(content);
        }

        public String getContent() { return content; }
        public DiffType getType() { return type; }
        public int getLineNumber() { return lineNumber; }
        public String getNormalizedContent() { return normalizedContent; }
    }

    /**
     * 差异详情
     */
    public static class DiffDetail {
        private DiffType type;
        private String description;
        private String localValue;
        private String remoteValue;

        public DiffDetail(DiffType type, String description, String localValue, String remoteValue) {
            this.type = type;
            this.description = description;
            this.localValue = localValue;
            this.remoteValue = remoteValue;
        }

        public DiffType getType() { return type; }
        public String getDescription() { return description; }
        public String getLocalValue() { return localValue; }
        public String getRemoteValue() { return remoteValue; }
    }

    /**
     * 差异类型
     */
    public enum DiffType {
        ADDED,      // 新增
        DELETED,    // 删除
        MODIFIED,   // 修改
        UNCHANGED   // 未变化
    }

    /**
     * 比对两个SQL DDL
     */
    public static SqlDiffResult compareSqlDdl(String localDdl, String remoteDdl) {
        SqlDiffResult result = new SqlDiffResult();
        
        // 预处理SQL，忽略开头的空行，从第一个非空行开始
        List<String> localLines = preprocessSqlLines(localDdl);
        List<String> remoteLines = preprocessSqlLines(remoteDdl);
        
        // 只有当两边都有有效内容时才开始对比
        if (localLines.isEmpty() || remoteLines.isEmpty()) {
            System.err.println("警告: 预处理后没有有效内容");
            System.err.println("本地DDL长度: " + localDdl.length());
            System.err.println("远程DDL长度: " + remoteDdl.length());
            return result;
        }
        
        // 验证两边第一行都不为空（这里应该总是true，因为preprocessSqlLines已经过滤了空行）
        if (localLines.get(0).trim().isEmpty() || remoteLines.get(0).trim().isEmpty()) {
            System.err.println("警告: 第一行仍然为空，这不应该发生");
            return result;
        }
        
        System.out.println("开始对比表结构:");
        System.out.println("本地第一行: " + localLines.get(0));
        System.out.println("远程第一行: " + remoteLines.get(0));
        
        // 使用智能比对算法
        performSmartDiff(localLines, remoteLines, result);
        
        // 智能分析差异
        analyzeDifferences(result);
        
        return result;
    }

    /**
     * 预处理SQL行
     */
    private static List<String> preprocessSqlLines(String ddl) {
        List<String> lines = new ArrayList<>();
        
        if (ddl == null || ddl.trim().isEmpty()) {
            System.err.println("警告: 输入的DDL为空");
            return lines;
        }
        
        String[] rawLines = ddl.split("\n");
        
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        
        // 确保至少有一行内容
        if (lines.isEmpty()) {
            System.err.println("警告: 预处理后没有有效行");
            System.err.println("原始DDL: '" + ddl + "'");
        }
        
        return lines;
    }

    /**
     * 执行智能比对
     */
    private static void performSmartDiff(List<String> localLines, List<String> remoteLines, SqlDiffResult result) {
        // 创建本地和远程行的映射，用于智能匹配
        Map<String, Integer> localLineMap = new HashMap<>();
        Map<String, Integer> remoteLineMap = new HashMap<>();
        
        // 构建本地行映射
        for (int i = 0; i < localLines.size(); i++) {
            String normalized = normalizeSqlLine(localLines.get(i));
            if (!normalized.isEmpty()) {
                localLineMap.put(normalized, i);
            }
        }
        
        // 构建远程行映射
        for (int i = 0; i < remoteLines.size(); i++) {
            String normalized = normalizeSqlLine(remoteLines.get(i));
            if (!normalized.isEmpty()) {
                remoteLineMap.put(normalized, i);
            }
        }
        
        // 标记已匹配的行
        Set<Integer> matchedLocal = new HashSet<>();
        Set<Integer> matchedRemote = new HashSet<>();
        
        // 第一轮：完全匹配
        for (int i = 0; i < localLines.size(); i++) {
            String localNormalized = normalizeSqlLine(localLines.get(i));
            if (localNormalized.isEmpty()) continue;
            
            Integer remoteIndex = remoteLineMap.get(localNormalized);
            
            if (remoteIndex != null && !matchedRemote.contains(remoteIndex)) {
                // 找到匹配
                result.localLines.add(new DiffLine(localLines.get(i), DiffType.UNCHANGED, i));
                result.remoteLines.add(new DiffLine(remoteLines.get(remoteIndex), DiffType.UNCHANGED, remoteIndex));
                matchedLocal.add(i);
                matchedRemote.add(remoteIndex);
            }
        }
        
        // 第二轮：处理未匹配的行
        for (int i = 0; i < localLines.size(); i++) {
            if (!matchedLocal.contains(i)) {
                result.localLines.add(new DiffLine(localLines.get(i), DiffType.DELETED, i));
            }
        }
        
        for (int i = 0; i < remoteLines.size(); i++) {
            if (!matchedRemote.contains(i)) {
                result.remoteLines.add(new DiffLine(remoteLines.get(i), DiffType.ADDED, i));
            }
        }
        
        // 按行号排序
        result.localLines.sort((a, b) -> Integer.compare(a.getLineNumber(), b.getLineNumber()));
        result.remoteLines.sort((a, b) -> Integer.compare(a.getLineNumber(), b.getLineNumber()));
        
        // 不添加任何占位符，保持真实的差异行数
        System.out.println("对比结果:");
        System.out.println("本地行数: " + result.localLines.size());
        System.out.println("远程行数: " + result.remoteLines.size());
    }

    /**
     * 智能分析差异
     */
    private static void analyzeDifferences(SqlDiffResult result) {

        // 分析字段差异
        analyzeFieldDifferences(result);
        
        // 分析索引差异
        analyzeIndexDifferences(result);
        
        // 分析注释差异
        analyzeCommentDifferences(result);
    }

    /**
     * 分析字段差异
     */
    private static void analyzeFieldDifferences(SqlDiffResult result) {
        Set<String> localFields = extractFields(result.localLines);
        Set<String> remoteFields = extractFields(result.remoteLines);
        
        // 找出新增的字段
        for (String field : remoteFields) {
            if (!localFields.contains(field)) {
                result.differences.add(new DiffDetail(
                    DiffType.ADDED, 
                    "新增字段: " + field,
                    "", 
                    field
                ));
            }
        }
        
        // 找出删除的字段
        for (String field : localFields) {
            if (!remoteFields.contains(field)) {
                result.differences.add(new DiffDetail(
                    DiffType.DELETED, 
                    "删除字段: " + field,
                    field, 
                    ""
                ));
            }
        }
    }

    /**
     * 分析索引差异
     */
    private static void analyzeIndexDifferences(SqlDiffResult result) {
        Set<String> localIndexes = extractIndexes(result.localLines);
        Set<String> remoteIndexes = extractIndexes(result.remoteLines);
        
        // 找出新增的索引
        for (String index : remoteIndexes) {
            if (!localIndexes.contains(index)) {
                result.differences.add(new DiffDetail(
                    DiffType.ADDED, 
                    "新增索引: " + index,
                    "", 
                    index
                ));
            }
        }
        
        // 找出删除的索引
        for (String index : localIndexes) {
            if (!remoteIndexes.contains(index)) {
                result.differences.add(new DiffDetail(
                    DiffType.DELETED, 
                    "删除索引: " + index,
                    index, 
                    ""
                ));
            }
        }
    }

    /**
     * 分析注释差异
     */
    private static void analyzeCommentDifferences(SqlDiffResult result) {
        // 注释差异通常不是关键差异，可以忽略或标记为轻微差异
        // 这里可以根据需要实现具体的注释比对逻辑
    }

    /**
     * 提取字段定义
     */
    private static Set<String> extractFields(List<DiffLine> lines) {
        Set<String> fields = new HashSet<>();
        
        for (DiffLine line : lines) {
            String content = line.getContent();
            if (content == null || content.isEmpty()) continue;
            
            if (content.contains("CREATE TABLE") || content.startsWith("PRIMARY KEY") || 
                content.startsWith("INDEX") || content.startsWith("COMMENT")) {
                continue;
            }
            
            // 提取字段名
            if (content.contains("NOT NULL") || content.contains("DEFAULT") || 
                content.contains("VARCHAR") || content.contains("INT") || 
                content.contains("DECIMAL") || content.contains("DATETIME") ||
                content.contains("TINYINT")) {
                
                String fieldName = extractFieldName(content);
                if (fieldName != null) {
                    fields.add(fieldName);
                }
            }
        }
        
        return fields;
    }

    /**
     * 提取索引定义
     */
    private static Set<String> extractIndexes(List<DiffLine> lines) {
        Set<String> indexes = new HashSet<>();
        
        for (DiffLine line : lines) {
            String content = line.getContent();
            if (content == null || content.isEmpty()) continue;
            
            if (content.startsWith("INDEX")) {
                String indexName = extractIndexName(content);
                if (indexName != null) {
                    indexes.add(indexName);
                }
            }
        }
        
        return indexes;
    }

    /**
     * 提取字段名
     */
    private static String extractFieldName(String line) {
        // 简单的字段名提取逻辑
        String[] parts = line.split("\\s+");
        if (parts.length > 0) {
            return parts[0].trim();
        }
        return null;
    }

    /**
     * 提取索引名
     */
    private static String extractIndexName(String line) {
        // 简单的索引名提取逻辑
        if (line.contains("INDEX")) {
            String[] parts = line.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                if ("INDEX".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        }
        return null;
    }

    /**
     * 标准化SQL行
     */
    private static String normalizeSqlLine(String line) {
        if (line == null) return "";
        
        // 移除多余的空格
        String normalized = line.replaceAll("\\s+", " ").trim();
        
        // 标准化引号
        normalized = normalized.replaceAll("['\"]", "'");
        
        // 移除末尾的逗号或分号
        normalized = normalized.replaceAll("[,;]$", "");
        
        // 标准化大小写（可选）
        // normalized = normalized.toLowerCase();
        
        return normalized;
    }

    /**
     * 获取差异统计
     */
    public static Map<String, Integer> getDiffStatistics(SqlDiffResult result) {
        Map<String, Integer> stats = new HashMap<>();
        
        int added = 0, deleted = 0, modified = 0, unchanged = 0;
        
        for (DiffLine line : result.localLines) {
            switch (line.getType()) {
                case ADDED: added++; break;
                case DELETED: deleted++; break;
                case MODIFIED: modified++; break;
                case UNCHANGED: unchanged++; break;
            }
        }
        
        stats.put("added", added);
        stats.put("deleted", deleted);
        stats.put("modified", modified);
        stats.put("unchanged", unchanged);
        stats.put("total", result.localLines.size());
        
        return stats;
    }
} 