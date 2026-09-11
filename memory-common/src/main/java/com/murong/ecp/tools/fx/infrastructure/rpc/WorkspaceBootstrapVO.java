package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkspaceBootstrapVO {
    private List<UserProjGroupPO> groups = new ArrayList<>();
    private String currentGroupName;
    private List<UserProjSettingPO> projects = new ArrayList<>();
}
