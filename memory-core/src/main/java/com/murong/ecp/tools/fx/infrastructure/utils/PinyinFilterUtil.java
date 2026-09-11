package com.murong.ecp.tools.fx.infrastructure.utils;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * 拼音过滤工具类
 * 用于处理JavaFX TextField中的拼音候选词问题
 */
public class PinyinFilterUtil {

    /**
     * 为TextField添加实时拼音过滤功能
     * 
     * @param textField 要添加过滤功能的TextField
     * @param converter 字符串转换器，用于数据类型转换
     * @param <T> 数据类型
     */
    public static <T> void addPinyinFilter(TextField textField, StringConverter<T> converter) {
        // 监听文本变化，实时过滤拼音候选词
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                // 延迟处理，避免在输入过程中频繁过滤
                Platform.runLater(() -> {
                    String filteredText = filterInputText(newVal);
                    if (!filteredText.equals(newVal)) {
                        textField.setText(filteredText);
                        // 将光标移到末尾
                        textField.positionCaret(filteredText.length());
                    }
                });
            }
        });
    }

    /**
     * 为TextField添加实时拼音过滤功能（简化版本，不需要转换器）
     * 
     * @param textField 要添加过滤功能的TextField
     */
    public static void addPinyinFilter(TextField textField) {
        addPinyinFilter(textField, new javafx.util.converter.DefaultStringConverter());
    }

    /**
     * 实时过滤输入文本，只保留中文、英文、数字和常用符号
     * 
     * @param text 原始文本
     * @return 过滤后的文本
     */
    public static String filterInputText(String text) {
        if (text == null) return "";
        
        StringBuilder result = new StringBuilder();
        boolean hasChinese = false;
        boolean hasEnglish = false;
        
        // 先分析文本内容
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                hasChinese = true;
            } else if (Character.isLetter(c) && c <= 127) {
                hasEnglish = true;
            }
        }
        
        // 如果同时包含中文和英文，优先保留中文
        if (hasChinese && hasEnglish) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                
                if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                    // 中文字符，直接添加
                    result.append(c);
                } else if (Character.isLetter(c) && c <= 127) {
                    // 英文字母，跳过（可能是拼音）
                    continue;
                } else if (Character.isDigit(c)) {
                    // 数字，跳过（可能是拼音的一部分）
                    continue;
                } else if (c == ' ' || c == '\t') {
                    // 空格和制表符，跳过
                    continue;
                } else {
                    // 其他符号，保留
                    result.append(c);
                }
            }
        } else if (hasChinese) {
            // 只有中文，过滤掉拼音
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                
                if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                    // 中文字符，直接添加
                    result.append(c);
                } else if (Character.isLetter(c) || Character.isDigit(c)) {
                    // 英文字母或数字，跳过
                    continue;
                } else if (c == ' ' || c == '\t') {
                    // 空格和制表符，跳过
                    continue;
                } else {
                    // 其他符号，保留
                    result.append(c);
                }
            }
        } else {
            // 只有英文或其他字符，保持原样
            return text;
        }
        
        return result.toString();
    }

    /**
     * 创建带有拼音过滤功能的TextField
     * 
     * @param initialText 初始文本
     * @param converter 字符串转换器
     * @param <T> 数据类型
     * @return 配置好的TextField
     */
    public static <T> TextField createFilteredTextField(String initialText, StringConverter<T> converter) {
        TextField textField = new TextField(initialText);
        addPinyinFilter(textField, converter);
        return textField;
    }

    /**
     * 创建带有拼音过滤功能的TextField（简化版本）
     * 
     * @param initialText 初始文本
     * @return 配置好的TextField
     */
    public static TextField createFilteredTextField(String initialText) {
        return createFilteredTextField(initialText, new javafx.util.converter.DefaultStringConverter());
    }

    /**
     * 创建带有拼音过滤功能的TextField（空初始文本）
     * 
     * @return 配置好的TextField
     */
    public static TextField createFilteredTextField() {
        return createFilteredTextField("");
    }
} 