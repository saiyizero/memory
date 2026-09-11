package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.LocalSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LocalSettingPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PaneBasicConfigController {
    
    @Autowired
    private LocalSettingDao localSettingDao;
    
    @Autowired
    private GlobalProperties globalProperties;
    
    // 表单控件
    @FXML private TextField dbUsrNameField;
    @FXML private PasswordField dbPassWordField;
    @FXML private TextField dbDriverNameField;
    @FXML private TextField dbUrlField;
    @FXML private TextField translateUrlField;
    @FXML private TextField translateAccessField;
    @FXML private TextField translateTokenField;
    

    
    // 当前编辑的记录
    private LocalSettingPO currentRecord;
    
    @FXML
    public void initialize() {
        // 初始加载数据到输入框（静默加载，不显示提示框）
        loadDataToFormSilently();
    }
    
    /**
     * 静默加载数据到输入框（不显示提示框）
     */
    private void loadDataToFormSilently() {
        try {
            // 查询状态为Y的记录
            List<LocalSettingPO> resultList = localSettingDao.queryActiveSettings();
            
            // 如果有数据，直接加载第一条到输入框
            if (resultList != null && !resultList.isEmpty()) {
                LocalSettingPO firstRecord = resultList.get(0);
                loadDataToForm(firstRecord);
                currentRecord = firstRecord;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 查询数据
     */
    @FXML
    public void queryData() {
        try {
            // 查询状态为Y的记录
            List<LocalSettingPO> resultList = localSettingDao.queryActiveSettings();
            
            // 如果有数据，直接加载第一条到输入框
            if (resultList != null && !resultList.isEmpty()) {
                LocalSettingPO firstRecord = resultList.get(0);
                loadDataToForm(firstRecord);
                currentRecord = firstRecord;
                ViewUtils.alertForSucess("查询成功，共找到 " + resultList.size() + " 条记录");
            } else {
                ViewUtils.alertForSucess("查询成功，但没有找到状态为Y的记录");
            }
            
        } catch (Exception e) {
            ViewUtils.alertForFail("查询失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    
    /**
     * 新增记录
     */
    @FXML
    public void addNew() {
        resetForm();
        // 清空当前记录引用，表示这是新增
        currentRecord = null;
    }
    
    /**
     * 重置表单
     */
    @FXML
    public void resetForm() {
        dbUsrNameField.clear();
        dbPassWordField.clear();
        dbDriverNameField.clear();
        dbUrlField.clear();
        translateUrlField.clear();
        translateAccessField.clear();
        translateTokenField.clear();
        
        currentRecord = null;
    }
    
    /**
     * 验证表单
     */
    private boolean validateForm() {

        if (dbDriverNameField.getText() == null || dbDriverNameField.getText().trim().isEmpty()) {
            ViewUtils.alertForFail("请输入数据库驱动名称");
            return false;
        }
        
        if (dbUrlField.getText() == null || dbUrlField.getText().trim().isEmpty()) {
            ViewUtils.alertForFail("请输入数据库连接URL");
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取表单数据
     */
    private LocalSettingPO getFormData() {
        LocalSettingPO po = new LocalSettingPO();
        po.setDbUsrName(dbUsrNameField.getText().trim());
        po.setDbPassWord(dbPassWordField.getText().trim());
        po.setDbDriverName(dbDriverNameField.getText().trim());
        po.setDbUrl(dbUrlField.getText().trim());
        po.setTranslateUrl(translateUrlField.getText().trim());
        po.setTranslateAccess(translateAccessField.getText().trim());
        po.setTranslateToken(translateTokenField.getText().trim());
        po.setStatus("Y"); // 固定设置为Y状态
        
        return po;
    }
    
    /**
     * 将数据加载到表单
     */
    private void loadDataToForm(LocalSettingPO po) {
        currentRecord = po;
        
        dbUsrNameField.setText(po.getDbUsrName());
        dbPassWordField.setText(po.getDbPassWord());
        dbDriverNameField.setText(po.getDbDriverName());
        dbUrlField.setText(po.getDbUrl());
        translateUrlField.setText(po.getTranslateUrl());
        translateAccessField.setText(po.getTranslateAccess());
        translateTokenField.setText(po.getTranslateToken());
    }
    
    /**
     * 保存数据
     */
    @FXML
    public void saveData() {
        try {
            // 验证必填字段
            if (!validateForm()) {
                return;
            }
            
            // 获取表单数据
            LocalSettingPO savePO = getFormData();
            
            // 设置更新信息
            if (globalProperties != null && globalProperties.getOperator() != null) {
                savePO.setUpdateBy(globalProperties.getOperator().getUsername());
            }
            savePO.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 保存数据
            if (currentRecord != null) {
                // 更新现有记录，使用状态为Y作为查询条件
                LocalSettingPO wherePO = new LocalSettingPO();
                wherePO.setStatus("Y");
                localSettingDao.updateByOne(savePO, wherePO);
                ViewUtils.alertForSucess("更新成功");
            } else {
                // 新增记录
                localSettingDao.save(savePO);
                ViewUtils.alertForSucess("保存成功");
            }
            
            // 刷新数据
            queryData();
            resetForm();
            
        } catch (Exception e) {
            ViewUtils.alertForFail("保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
