package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DbConnectionDao extends HttpDaoSupport<DbConnectionPO> {

    public void save(DbConnectionPO po) {
        super.insert(po);
    }

    public List<DbConnectionPO> queryForList (DbConnectionPO po) {
        return super.queryForList(po);
    }

    public List<DbConnectionPO> queryForListWithNullEnv() {
        String sql = "SELECT * FROM db_connection WHERE env_name IS NULL OR env_name = ''";
        return super.queryListBySql(sql, DbConnectionPO.class);
    }


    public void updateMainFlg(DbConnectionPO po) {
        DbConnectionPO wherePO = new DbConnectionPO();
        wherePO.setGroupName(po.getGroupName());
        wherePO.setProjectName(po.getProjectName());

        DbConnectionPO updatePo = new DbConnectionPO();
        if(StringUtils.equals(po.getMainFlg(),"1")){
            updatePo.setMainFlg("0");
            super.updateByOne(updatePo,wherePO);
        }

        updatePo.setMainFlg(po.getMainFlg());
        wherePO.setEnvName(po.getEnvName());
        super.updateByOne(updatePo,wherePO);
    }

    /**
     * 根据环境、项目信息查询数据库连接
     */
    public List<DbConnectionPO> queryByEnvAndProject(String envName, String groupName, String projectName, String appName) {
        DbConnectionPO query = new DbConnectionPO();
        query.setEnvName(envName);
        query.setGroupName(groupName);
        query.setProjectName(projectName);
        query.setAppName(appName);
        return queryForList(query);
    }
}
