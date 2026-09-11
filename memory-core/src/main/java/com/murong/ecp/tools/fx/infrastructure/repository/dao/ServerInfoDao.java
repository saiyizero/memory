package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.DaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ServerInfoPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ServerInfoDao extends DaoSupport<ServerInfoPO> {
    public void save(ServerInfoPO po) {
        super.insert(po);
    }

    public List<ServerInfoPO> queryForList(ServerInfoPO po) {
        return super.queryForList(po);
    }

    public void updateScanFlg(String groupName, String appName, String scanFlg) {
        String sql = "update server_info set scan_flg=? where group_name=? and app_name=?";
        super.updateBySql(sql, scanFlg, groupName, appName);
    }
} 