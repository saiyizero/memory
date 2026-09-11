package com.murong.ecp.tools.fx.domain.service.common;

import com.murong.ecp.tools.fx.infrastructure.repository.dao.UserPreferenceDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserPreferencePO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 用户偏好设置服务
 */
@Service
public class UserPreferenceService {
    
    @Autowired
    private UserPreferenceDao userPreferenceDao;
    
    // 默认用户ID
    private static final String DEFAULT_USER_ID = "default";
    
    // 菜单选中状态的偏好键
    private static final String MENU_SELECTED_KEY = "menu_selected";
    
    /**
     * 保存菜单选中状态
     */
    public void saveMenuSelected(String menuKey) {
        try {
            System.out.println("[UserPreferenceService] 保存菜单选中状态: " + menuKey);
            UserPreferencePO existingPo = userPreferenceDao.queryByUserIdAndKey(DEFAULT_USER_ID, MENU_SELECTED_KEY);
            
            if (existingPo != null) {
                // 更新现有记录
                UserPreferencePO updatePo = new UserPreferencePO();
                updatePo.setPreferenceValue(menuKey);
                updatePo.setUpdateTime(MrDateUtils.getCurrentTime());
                updatePo.setUpdateBy("system");
                
                UserPreferencePO wherePo = new UserPreferencePO();
                wherePo.setId(existingPo.getId());
                
                userPreferenceDao.updateByOne(updatePo, wherePo);
            } else {
                // 创建新记录
                UserPreferencePO newPo = new UserPreferencePO();
                newPo.setId(UUID.randomUUID().toString());
                newPo.setUserId(DEFAULT_USER_ID);
                newPo.setPreferenceKey(MENU_SELECTED_KEY);
                newPo.setPreferenceValue(menuKey);
                newPo.setDescription("用户最后选中的菜单项");
                newPo.setUpdateTime(MrDateUtils.getCurrentTime());
                newPo.setUpdateBy("system");
                
                userPreferenceDao.save(newPo);
            }
        } catch (Exception e) {
            System.err.println("保存菜单选中状态失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取菜单选中状态
     */
    public String getMenuSelected() {
        try {
            System.out.println("[UserPreferenceService] 查询菜单选中状态，用户ID: " + DEFAULT_USER_ID + ", 键: " + MENU_SELECTED_KEY);
            UserPreferencePO po = userPreferenceDao.queryByUserIdAndKey(DEFAULT_USER_ID, MENU_SELECTED_KEY);
            System.out.println("[UserPreferenceService] 查询结果: " + (po != null ? po.getPreferenceValue() : "null"));
            return po != null ? po.getPreferenceValue() : null;
        } catch (Exception e) {
            System.err.println("获取菜单选中状态失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 清除菜单选中状态
     */
    public void clearMenuSelected() {
        try {
            UserPreferencePO po = userPreferenceDao.queryByUserIdAndKey(DEFAULT_USER_ID, MENU_SELECTED_KEY);
            if (po != null) {
                userPreferenceDao.delete(po);
            }
        } catch (Exception e) {
            System.err.println("清除菜单选中状态失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 