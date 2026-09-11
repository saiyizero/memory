package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.LocalSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LocalSettingPO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 本地设置数据访问对象
 */
@Repository
public class LocalSettingDao extends LocalSupport<LocalSettingPO> {

    /**
     * 保存本地设置
     */
    public void save(LocalSettingPO po) {
        super.insert(po);
    }

    /**
     * 清除登录用户信息
     */
    public void clearLinkInfo() {
        String sql = "update local_setting set link_usr_name='',link_pass_word='' where status = 'Y'";
        super.updateBySql(sql);
    }

    public void updateLinkInfo(String linkUsrName, String linkPassWord) {
        LocalSettingPO localSettingPO = new LocalSettingPO();
        localSettingPO.setLinkUsrName(linkUsrName);
        localSettingPO.setLinkPassWord(linkPassWord);

        LocalSettingPO whereLocalPO = new LocalSettingPO();
        whereLocalPO.setStatus(FlgEnum.YES.getValue());
        super.updateByOne(localSettingPO,whereLocalPO);
    }

    /**
     * 查询单个本地设置
     */
    public LocalSettingPO queryOne(LocalSettingPO po) {
        return super.queryOne(po);
    }

    /**
     * 更新本地设置
     */
    public void updateByOne(LocalSettingPO updatePo) {
        super.updateByOne(updatePo, new LocalSettingPO());
    }
    
    /**
     * 根据条件更新本地设置
     */
    public void updateByOne(LocalSettingPO updatePo, LocalSettingPO wherePo) {
        super.updateByOne(updatePo, wherePo);
    }
    
    /**
     * 查询所有状态为Y的记录
     */
    public List<LocalSettingPO> queryActiveSettings() {
        LocalSettingPO queryPO = new LocalSettingPO();
        queryPO.setStatus("Y");
        return super.queryForList(queryPO);
    }

}
