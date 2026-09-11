package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="local_setting")
public class LocalSettingPO {
    private String linkUsrName;
    private String linkPassWord;
    private String dbUsrName;
    private String dbPassWord;
    private String dbDriverName;
    private String dbUrl;
    private String translateUrl;
    private String translateAccess;
    private String translateToken;
    private String status;
    private String updateBy;
    private String updateTime;
}
