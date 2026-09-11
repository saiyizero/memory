package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DbConnectionPO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据源连接 HTTP 门面，SQL 在 memory-service 执行。
 */
@Repository
public class DbConnectionDao extends HttpDaoSupport<DbConnectionPO> {

    public void save(DbConnectionPO po) {
        super.insert(po);
    }

    public List<DbConnectionPO> queryForList (DbConnectionPO po) {
        return super.queryForList(po);
    }

    public List<DbConnectionPO> queryForListWithNullEnv() {
        return invokeList("queryForListWithNullEnv", DbConnectionPO.class);
    }

    public void updateMainFlg(DbConnectionPO po) {
        invokeVoid("updateMainFlg", po);
    }

    public List<DbConnectionPO> queryByEnvAndProject(String envName, String groupName, String projectName, String appName) {
        DbConnectionPO query = new DbConnectionPO();
        query.setEnvName(envName);
        query.setGroupName(groupName);
        query.setProjectName(projectName);
        query.setAppName(appName);
        return queryForList(query);
    }
}
