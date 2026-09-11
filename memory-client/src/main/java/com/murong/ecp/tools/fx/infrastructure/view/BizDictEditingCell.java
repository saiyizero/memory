package com.murong.ecp.tools.fx.infrastructure.view;

import com.murong.ecp.tools.fx.infrastructure.rpc.BizDictRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.control.TableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 支持Command+G快捷键的业务字段编辑单元格
 */
public class BizDictEditingCell<S> extends TableCell<S, String> {
    
    private TextField textField;
    private final StringConverter<String> converter;
    private final BizDictRpcService bizDictRpcService;
    private final OnBizDictSelectedListener listener;
    
    public interface OnBizDictSelectedListener {
        void onBizDictSelected(BizDictPO bizDict);
    }
    
    public BizDictEditingCell(StringConverter<String> converter, BizDictRpcService bizDictRpcService, OnBizDictSelectedListener listener) {
        this.converter = converter;
        this.bizDictRpcService = bizDictRpcService;
        this.listener = listener;
    }
    
    @Override
    public void startEdit() {
        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }
        
        super.startEdit();
        
        if (isEditing()) {
            if (textField == null) {
                textField = new TextField(getItem());
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
                
                // 设置快捷键监听
                setupShortcuts();
                
                // 设置文本变化监听
                textField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!isEditing()) {
                        commitEdit(converter.fromString(newValue));
                    }
                });
                
                // 失去焦点时提交编辑
                textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue) {
                        commitEdit(converter.fromString(textField.getText()));
                    }
                });
            }
            
            textField.setText(getItem());
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
            
            // 双击进入编辑模式后，延迟一下再弹出业务字段选择对话框
            Platform.runLater(() -> {
                showBizDictSelectionDialog();
            });
        }
    }
    
    private void setupShortcuts() {
        // 保留回车键触发搜索的功能，作为备用方式
        textField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // 回车键触发搜索
                event.consume();
                showBizDictSelectionDialog();
            }
        });
    }
    
    private void showBizDictSelectionDialog() {
        // 获取当前输入的文本作为搜索条件
        String searchText = textField.getText();
        
        // 调用BizDictDao搜索业务字段，不通过SQL筛选，直接查询所有数据
        List<BizDictPO> bizDictList = bizDictRpcService.searchByName(null);
        
        if (bizDictList.isEmpty()) {
            // 如果没有找到匹配的业务字段，显示提示
            Platform.runLater(() -> {
                ViewUtils.alertForFail("未找到匹配的业务字段！");
            });
            return;
        }
        
        // 显示业务字段选择对话框，传递搜索条件
        BizDictSelectionDialog dialog = new BizDictSelectionDialog();
        Optional<BizDictPO> selectedBizDict = dialog.showAndWait(bizDictList, searchText);
        
        selectedBizDict.ifPresent(bizDict -> {
            // 用户选择了业务字段，更新单元格内容
            if (listener != null) {
                listener.onBizDictSelected(bizDict);
            }
            
            // 更新文本字段
            String displayText = StringUtils.isNotBlank(bizDict.getNameSnake()) ? 
                bizDict.getNameSnake() : bizDict.getNameCamel();
            textField.setText(displayText);
            
            // 提交编辑
            commitEdit(displayText);
        });
    }
    
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(converter.toString(getItem()));
        setGraphic(null);
    }
    
    @Override
    public void updateItem(String item, boolean empty) {
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
    }
    
    private String getString() {
        return getItem() == null ? "" : converter.toString(getItem());
    }
}
