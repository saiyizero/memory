package com.murong.ecp.tools.fx.infrastructure.view;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL语法高亮组件
 * 提供类似IDEA和DBeaver的SQL语法高亮功能
 */
public class SqlSyntaxHighlighter extends StackPane {
    
    private final TextArea textArea;
    private final TextFlow textFlow;
    private final ScrollPane scrollPane;
    private final StringProperty textProperty;
    
    // SQL关键字正则表达式
    private static final Pattern SQL_KEYWORDS_PATTERN = Pattern.compile(
        "\\b(SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TABLE|INDEX|VIEW|PROCEDURE|FUNCTION|TRIGGER|" +
        "JOIN|LEFT|RIGHT|INNER|OUTER|ON|AND|OR|NOT|IN|EXISTS|BETWEEN|LIKE|IS|NULL|ORDER|BY|GROUP|HAVING|" +
        "UNION|ALL|DISTINCT|AS|ASC|DESC|LIMIT|OFFSET|TOP|CASE|WHEN|THEN|ELSE|END|IF|WHILE|FOR|" +
        "INT|INTEGER|BIGINT|SMALLINT|TINYINT|DECIMAL|NUMERIC|FLOAT|DOUBLE|REAL|" +
        "CHAR|VARCHAR|TEXT|LONGTEXT|BLOB|LONGBLOB|DATE|DATETIME|TIMESTAMP|TIME|YEAR|" +
        "PRIMARY|KEY|FOREIGN|REFERENCES|UNIQUE|CHECK|DEFAULT|AUTO_INCREMENT|" +
        "COMMIT|ROLLBACK|TRANSACTION|BEGIN|SAVEPOINT|GRANT|REVOKE|DENY|" +
        "COUNT|SUM|AVG|MAX|MIN|COALESCE|NULLIF|CAST|CONVERT|" +
        "SUBSTRING|CONCAT|LENGTH|UPPER|LOWER|TRIM|LTRIM|RTRIM|" +
        "YEAR|MONTH|DAY|HOUR|MINUTE|SECOND|NOW|CURRENT_DATE|CURRENT_TIME|CURRENT_TIMESTAMP)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // 字符串正则表达式
    private static final Pattern STRING_PATTERN = Pattern.compile(
        "'([^']|'')*'|\"([^\"]|\"\")*\"",
        Pattern.CASE_INSENSITIVE
    );
    
    // 数字正则表达式
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
        "\\b\\d+(\\.\\d+)?\\b"
    );
    
    // 注释正则表达式
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
        "--.*$|/\\*.*?\\*/",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    
    // 操作符正则表达式
    private static final Pattern OPERATOR_PATTERN = Pattern.compile(
        "\\b(\\+|-|\\*|/|%|=|!=|<>|<|>|<=|>=|\\||&|\\^|~|<<|>>)\\b"
    );
    
    public SqlSyntaxHighlighter() {
        this.textArea = new TextArea();
        this.textFlow = new TextFlow();
        this.scrollPane = new ScrollPane();
        this.textProperty = new SimpleStringProperty("");
        
        initializeComponents();
        setupEventHandlers();
    }
    
    private void initializeComponents() {
        // 设置CSS样式类
        getStyleClass().add("sql-syntax-highlighter");
        textArea.getStyleClass().add("text-area");
        textFlow.getStyleClass().add("text-flow");
        scrollPane.getStyleClass().add("scroll-pane");
        
        // 初始时显示TextArea
        getChildren().add(textArea);
        
        // 绑定文本属性
        textProperty.bindBidirectional(textArea.textProperty());
    }
    
    private void setupEventHandlers() {
        // 监听文本变化，实时更新语法高亮
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && !newText.equals(oldText)) {
                Platform.runLater(this::updateSyntaxHighlighting);
            }
        });
        
        // 监听焦点变化，切换显示模式
        textArea.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                // 获得焦点时显示TextArea用于编辑
                if (getChildren().contains(scrollPane)) {
                    getChildren().remove(scrollPane);
                    getChildren().add(textArea);
                }
            } else {
                // 失去焦点时显示语法高亮的TextFlow
                if (getChildren().contains(textArea)) {
                    getChildren().remove(textArea);
                    getChildren().add(scrollPane);
                    scrollPane.setContent(textFlow);
                    updateSyntaxHighlighting();
                }
            }
        });
        
        // 为ScrollPane添加鼠标点击事件，点击时切换回编辑模式
        scrollPane.setOnMouseClicked(event -> {
            if (getChildren().contains(scrollPane)) {
                getChildren().remove(scrollPane);
                getChildren().add(textArea);
                textArea.requestFocus();
            }
        });
        
        // 为TextFlow添加鼠标点击事件
        textFlow.setOnMouseClicked(event -> {
            if (getChildren().contains(scrollPane)) {
                getChildren().remove(scrollPane);
                getChildren().add(textArea);
                textArea.requestFocus();
            }
        });
    }
    
    /**
     * 更新语法高亮
     */
    private void updateSyntaxHighlighting() {
        String text = textArea.getText();
        if (text == null) {
            text = "";
        }
        
        // 清空TextFlow
        textFlow.getChildren().clear();
        
        // 解析并高亮文本
        List<TextSegment> segments = parseText(text);
        
        for (TextSegment segment : segments) {
            Text textNode = new Text(segment.getText());
            textNode.setFont(Font.font("Monaco", 14));
            
            // 根据类型设置CSS样式类
            switch (segment.getType()) {
                case KEYWORD:
                    textNode.getStyleClass().add("sql-keyword");
                    break;
                case STRING:
                    textNode.getStyleClass().add("sql-string");
                    break;
                case NUMBER:
                    textNode.getStyleClass().add("sql-number");
                    break;
                case COMMENT:
                    textNode.getStyleClass().add("sql-comment");
                    break;
                case OPERATOR:
                    textNode.getStyleClass().add("sql-operator");
                    break;
                case IDENTIFIER:
                    textNode.getStyleClass().add("sql-identifier");
                    break;
                default:
                    textNode.getStyleClass().add("sql-plain");
                    break;
            }
            
            textFlow.getChildren().add(textNode);
        }
    }
    
    /**
     * 解析文本，识别不同类型的语法元素
     */
    private List<TextSegment> parseText(String text) {
        List<TextSegment> segments = new ArrayList<>();
        
        // 先处理注释（优先级最高）
        segments.addAll(parseComments(text));
        
        // 处理字符串（优先级次之）
        segments.addAll(parseStrings(text));
        
        // 处理关键字
        segments.addAll(parseKeywords(text));
        
        // 处理数字
        segments.addAll(parseNumbers(text));
        
        // 处理操作符
        segments.addAll(parseOperators(text));
        
        // 按位置排序并处理重叠
        segments.sort((a, b) -> Integer.compare(a.getStart(), b.getStart()));
        segments = resolveOverlaps(segments);
        
        // 填充空白区域
        return fillGaps(text, segments);
    }
    
    /**
     * 解决重叠的文本段，保留优先级高的
     */
    private List<TextSegment> resolveOverlaps(List<TextSegment> segments) {
        if (segments.isEmpty()) {
            return segments;
        }
        
        List<TextSegment> result = new ArrayList<>();
        TextSegment current = segments.get(0);
        
        for (int i = 1; i < segments.size(); i++) {
            TextSegment next = segments.get(i);
            
            if (current.getEnd() <= next.getStart()) {
                // 没有重叠
                result.add(current);
                current = next;
            } else {
                // 有重叠，保留优先级高的
                if (getPriority(current.getType()) >= getPriority(next.getType())) {
                    // 当前段优先级更高，跳过下一段
                    continue;
                } else {
                    // 下一段优先级更高，替换当前段
                    current = next;
                }
            }
        }
        
        result.add(current);
        return result;
    }
    
    /**
     * 获取语法元素类型的优先级
     */
    private int getPriority(SegmentType type) {
        switch (type) {
            case COMMENT: return 5;  // 最高优先级
            case STRING: return 4;
            case KEYWORD: return 3;
            case NUMBER: return 2;
            case OPERATOR: return 1;
            case IDENTIFIER: return 0;
            case PLAIN: return -1;   // 最低优先级
            default: return 0;
        }
    }
    
    private List<TextSegment> parseComments(String text) {
        List<TextSegment> segments = new ArrayList<>();
        Matcher matcher = COMMENT_PATTERN.matcher(text);
        
        while (matcher.find()) {
            segments.add(new TextSegment(
                matcher.group(),
                matcher.start(),
                matcher.end(),
                SegmentType.COMMENT
            ));
        }
        
        return segments;
    }
    
    private List<TextSegment> parseKeywords(String text) {
        List<TextSegment> segments = new ArrayList<>();
        Matcher matcher = SQL_KEYWORDS_PATTERN.matcher(text);
        
        while (matcher.find()) {
            segments.add(new TextSegment(
                matcher.group(),
                matcher.start(),
                matcher.end(),
                SegmentType.KEYWORD
            ));
        }
        
        return segments;
    }
    
    private List<TextSegment> parseStrings(String text) {
        List<TextSegment> segments = new ArrayList<>();
        Matcher matcher = STRING_PATTERN.matcher(text);
        
        while (matcher.find()) {
            segments.add(new TextSegment(
                matcher.group(),
                matcher.start(),
                matcher.end(),
                SegmentType.STRING
            ));
        }
        
        return segments;
    }
    
    private List<TextSegment> parseNumbers(String text) {
        List<TextSegment> segments = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        
        while (matcher.find()) {
            segments.add(new TextSegment(
                matcher.group(),
                matcher.start(),
                matcher.end(),
                SegmentType.NUMBER
            ));
        }
        
        return segments;
    }
    
    private List<TextSegment> parseOperators(String text) {
        List<TextSegment> segments = new ArrayList<>();
        Matcher matcher = OPERATOR_PATTERN.matcher(text);
        
        while (matcher.find()) {
            segments.add(new TextSegment(
                matcher.group(),
                matcher.start(),
                matcher.end(),
                SegmentType.OPERATOR
            ));
        }
        
        return segments;
    }
    
    private List<TextSegment> fillGaps(String text, List<TextSegment> segments) {
        List<TextSegment> result = new ArrayList<>();
        int lastEnd = 0;
        
        for (TextSegment segment : segments) {
            // 添加空白文本
            if (segment.getStart() > lastEnd) {
                String plainText = text.substring(lastEnd, segment.getStart());
                result.add(new TextSegment(plainText, lastEnd, segment.getStart(), SegmentType.PLAIN));
            }
            
            result.add(segment);
            lastEnd = segment.getEnd();
        }
        
        // 添加最后的空白文本
        if (lastEnd < text.length()) {
            String plainText = text.substring(lastEnd);
            result.add(new TextSegment(plainText, lastEnd, text.length(), SegmentType.PLAIN));
        }
        
        return result;
    }
    
    // Getter方法
    public TextArea getTextArea() {
        return textArea;
    }
    
    public StringProperty textProperty() {
        return textProperty;
    }
    
    public String getText() {
        return textArea.getText();
    }
    
    public void setText(String text) {
        textArea.setText(text);
    }
    
    public String getSelectedText() {
        return textArea.getSelectedText();
    }
    
    public void selectRange(int start, int end) {
        textArea.selectRange(start, end);
    }
    
    public void positionCaret(int pos) {
        textArea.positionCaret(pos);
    }
    
    public void requestFocus() {
        textArea.requestFocus();
    }
    
    public void clear() {
        textArea.clear();
    }
    
    public void setPromptText(String promptText) {
        textArea.setPromptText(promptText);
    }
    
    public void setPrefRowCount(int rowCount) {
        textArea.setPrefRowCount(rowCount);
    }
    
    // 内部类：文本段
    private static class TextSegment {
        private final String text;
        private final int start;
        private final int end;
        private final SegmentType type;
        
        public TextSegment(String text, int start, int end, SegmentType type) {
            this.text = text;
            this.start = start;
            this.end = end;
            this.type = type;
        }
        
        public String getText() { return text; }
        public int getStart() { return start; }
        public int getEnd() { return end; }
        public SegmentType getType() { return type; }
    }
    
    // 枚举：文本段类型
    private enum SegmentType {
        KEYWORD, STRING, NUMBER, COMMENT, OPERATOR, IDENTIFIER, PLAIN
    }
} 