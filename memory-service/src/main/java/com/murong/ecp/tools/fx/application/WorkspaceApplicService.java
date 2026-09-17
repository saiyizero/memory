package com.murong.ecp.tools.fx.application;

import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.UserProjGroupDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.UserProjSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.WorkspaceBootstrapVO;
import com.murong.ecp.tools.fx.infrastructure.rpc.WorkspaceSwitchGroupRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceApplicService {

    private final UserProjGroupDao userProjGroupDao;
    private final UserProjSettingDao userProjSettingDao;

    public WorkspaceApplicService(UserProjGroupDao userProjGroupDao, UserProjSettingDao userProjSettingDao) {
        this.userProjGroupDao = userProjGroupDao;
        this.userProjSettingDao = userProjSettingDao;
    }

    @PostMapping("/bootstrap")
    public CrResult<WorkspaceBootstrapVO> bootstrap() {
        List<UserProjGroupPO> groups = userProjGroupDao.queryMine();
        if (groups == null) {
            groups = new ArrayList<>();
        }
        String currentGroupName = resolveCurrentGroupName(groups);
        WorkspaceBootstrapVO vo = new WorkspaceBootstrapVO();
        vo.setGroups(groups);
        vo.setCurrentGroupName(currentGroupName);
        vo.setProjects(queryShowProjects(currentGroupName));
        CrResult<WorkspaceBootstrapVO> result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        result.setData(vo);
        return result;
    }

    @PostMapping("/switch-group")
    public CrResult<WorkspaceBootstrapVO> switchGroup(@RequestBody WorkspaceSwitchGroupRequest request) {
        String groupName = request == null ? null : request.getGroupName();
        if (StringUtils.isBlank(groupName)) {
            CrResult<WorkspaceBootstrapVO> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("项目群名称不允许为空");
            return result;
        }
        userProjGroupDao.switchGroup(groupName);
        WorkspaceBootstrapVO vo = new WorkspaceBootstrapVO();
        vo.setCurrentGroupName(groupName);
        vo.setProjects(queryShowProjects(groupName));
        CrResult<WorkspaceBootstrapVO> result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        result.setData(vo);
        return result;
    }

    private String resolveCurrentGroupName(List<UserProjGroupPO> groups) {
        for (UserProjGroupPO group : groups) {
            if (group != null && "Y".equalsIgnoreCase(group.getCurFlag())) {
                return group.getGroupName();
            }
        }
        return groups.isEmpty() || groups.get(0) == null ? null : groups.get(0).getGroupName();
    }

    private List<UserProjSettingPO> queryShowProjects(String groupName) {
        if (StringUtils.isBlank(groupName)) {
            return new ArrayList<>();
        }
        List<UserProjSettingPO> projects = userProjSettingDao.queryMineByGroup(groupName);
        return projects == null ? new ArrayList<>() : projects;
    }
}
