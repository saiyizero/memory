package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectGroupPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目组 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class ProjectGroupRpcService extends HttpDaoSupport<ProjectGroupPO> {

    public ProjectGroupPO queryCurGroup() {
        return invoke("queryCurGroup", ProjectGroupPO.class);
    }

    public List<ProjectGroupPO> queryAllGroups() {
        return invokeList("queryAllGroups", ProjectGroupPO.class);
    }

    public void save(ProjectGroupPO projectGroup) {
        super.insert(projectGroup);
    }

    public void deleteProjectGroup(String groupName) {
        invokeVoid("deleteProjectGroup", groupName);
    }

    public ProjectGroupPO queryByGroupName(String groupName) {
        ProjectGroupPO queryPo = new ProjectGroupPO();
        queryPo.setGroupName(groupName);
        return super.queryOne(queryPo);
    }
}
