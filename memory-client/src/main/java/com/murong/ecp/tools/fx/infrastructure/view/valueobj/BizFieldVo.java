package com.murong.ecp.tools.fx.infrastructure.view.valueobj;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BizFieldVo {
    private final StringProperty fieldId = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty length = new SimpleStringProperty();
    private final StringProperty dict = new SimpleStringProperty();
    private final StringProperty remark = new SimpleStringProperty();
    private final StringProperty englishRemark = new SimpleStringProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final BooleanProperty required = new SimpleBooleanProperty(false);
    private final StringProperty enumValues = new SimpleStringProperty();
    private final BooleanProperty isEmptyRow = new SimpleBooleanProperty(false);

    public String getFieldId() {
        return fieldId.get();
    }

    public StringProperty fieldIdProperty() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId.set(fieldId);
    }

    public String getType() {
        return type.get();
    }

    public StringProperty typeProperty() {
        return type;
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public String getLength() {
        return length.get();
    }

    public StringProperty lengthProperty() {
        return length;
    }

    public void setLength(String length) {
        this.length.set(length);
    }

    public String getDict() {
        return dict.get();
    }

    public StringProperty dictProperty() {
        return dict;
    }

    public void setDict(String dict) {
        this.dict.set(dict);
    }

    public String getRemark() {
        return remark.get();
    }

    public StringProperty remarkProperty() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark.set(remark);
    }

    public String getEnglishRemark() {
        return englishRemark.get();
    }

    public StringProperty englishRemarkProperty() {
        return englishRemark;
    }

    public void setEnglishRemark(String englishRemark) {
        this.englishRemark.set(englishRemark);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public boolean isRequired() {
        return required.get();
    }

    public BooleanProperty requiredProperty() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required.set(required);
    }

    public String getEnumValues() {
        return enumValues.get();
    }

    public StringProperty enumValuesProperty() {
        return enumValues;
    }

    public void setEnumValues(String enumValues) {
        this.enumValues.set(enumValues);
    }

    public boolean isEmptyRow() {
        return isEmptyRow.get();
    }

    public BooleanProperty isEmptyRowProperty() {
        return isEmptyRow;
    }

    public void setIsEmptyRow(boolean isEmptyRow) {
        this.isEmptyRow.set(isEmptyRow);
    }
} 