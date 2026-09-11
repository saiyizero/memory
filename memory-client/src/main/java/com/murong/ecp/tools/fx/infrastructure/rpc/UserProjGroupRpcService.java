package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.enums.FlgEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.LocalSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.LocalSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserInfoPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户项目组 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class UserProjGroupRpcService extends HttpDaoSupport<UserProjGroupPO> {

    @Autowired
    private LocalSettingDao localSettingDao;

    @Autowired
    private UserInfoRpcService userInfoRpcService;

    private UserInfoPO getCurrentUser() {
        LocalSettingPO localSettingPO = new LocalSettingPO();
        localSettingPO.setStatus(FlgEnum.YES.getValue());
        LocalSettingPO currentSetting = localSettingDao.queryOne(localSettingPO);
        if (currentSetting != null && currentSetting.getLinkUsrName() != null) {
            return userInfoRpcService.queryByUsername(currentSetting.getLinkUsrName());
        }
        return null;
    }

    public UserProjGroupPO queryCurGroup() {
        return invoke("queryCurGroup", UserProjGroupPO.class);
    }

    public UserProjGroupPO queryCurGroup(LocalSettingPO localSettingPO) {
        return invoke("queryCurGroup", UserProjGroupPO.class, localSettingPO);
    }

    public void switchGroup(String groupName) {
        invokeVoid("switchGroup", groupName);
    }

    public void save(UserProjGroupPO po) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            po.setUserId(currentUser.getUserId());
            po.setUsername(currentUser.getUsername());
        }
        super.insert(po);
    }

    public List<UserProjGroupPO> queryForList(UserProjGroupPO po) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            po.setUserId(currentUser.getUserId());
            po.setUsername(currentUser.getUsername());
        }
        return super.queryForList(po, "group_name asc");
    }

    public List<UserProjGroupPO> queryByUserId(String userId) {
        UserProjGroupPO queryPo = new UserProjGroupPO();
        queryPo.setUserId(userId);
        return super.queryForList(queryPo, "group_name asc");
    }

    public List<UserProjGroupPO> queryByUsername(String username) {
        UserProjGroupPO queryPo = new UserProjGroupPO();
        queryPo.setUsername(username);
        return super.queryForList(queryPo, "group_name asc");
    }

    public UserProjGroupPO queryByGroupName(String groupName) {
        UserProjGroupPO queryPo = new UserProjGroupPO();
        queryPo.setGroupName(groupName);
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            queryPo.setUserId(currentUser.getUserId());
            queryPo.setUsername(currentUser.getUsername());
        }
        return super.queryOne(queryPo);
    }

    public boolean isGroupNameExists(String groupName, String userId, String username) {
        Boolean exists = invoke("isGroupNameExists", Boolean.class, groupName, userId, username);
        return Boolean.TRUE.equals(exists);
    }

    public void deleteByGroupName(String groupName, String userId, String username) {
        invokeVoid("deleteByGroupName", groupName, userId, username);
    }

    public void update(UserProjGroupPO updatePo, UserProjGroupPO wherePo) {
        UserInfoPO currentUser = getCurrentUser();
        if (currentUser != null) {
            wherePo.setUserId(currentUser.getUserId());
            wherePo.setUsername(currentUser.getUsername());
        }
        super.updateByOne(updatePo, wherePo);
    }
}
