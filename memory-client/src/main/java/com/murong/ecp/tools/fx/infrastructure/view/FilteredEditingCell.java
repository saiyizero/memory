package com.murong.ecp.tools.fx.infrastructure.view;

import com.murong.ecp.tools.fx.infrastructure.utils.PinyinFilterUtil;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * 通用的带拼音过滤功能的编辑单元格类
 * 其他页面可以直接使用此类来创建支持拼音过滤的表格单元格
 */
public class FilteredEditingCell<S, T> extends TableCell<S, T> {
    private TextField textField;
    private final StringConverter<T> converter;

    public FilteredEditingCell(StringConverter<T> converter) {
        this.converter = converter;
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            createTextField();
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
        }
    }

    @Override
    public void cancelEdit() {
        if (isEditing() && textField != null) {
            commitEdit(converter.fromString(textField.getText()));
        } else {
            super.cancelEdit();
            setText(converter.toString(getItem()));
            setGraphic(null);
        }
    }

    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getString());
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(getString());
                setGraphic(null);
            }
        }
        
        // 强制刷新显示
        if (!empty && !isEditing()) {
            setText(getString());
        }
    }

    private void createTextField() {
        textField = new TextField(getString());
        textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
        
        // 使用工具类添加拼音过滤功能
        PinyinFilterUtil.addPinyinFilter(textField, converter);
        
        // 失去焦点时提交编辑
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                commitEdit(converter.fromString(textField.getText()));
            }
        });
        
        // 回车键提交编辑
        textField.setOnKeyPressed(t -> {
            if (t.getCode() == javafx.scene.input.KeyCode.ENTER) {
                commitEdit(converter.fromString(textField.getText()));
            } else if (t.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
    }

    private String getString() {
        return getItem() == null ? "" : converter.toString(getItem());
    }
} 