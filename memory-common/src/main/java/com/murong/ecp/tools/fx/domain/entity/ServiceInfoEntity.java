package com.murong.ecp.tools.fx.domain.entity;

import lombok.Data;

import java.util.List;

@Data
public class ServiceInfoEntity {
    private String groupName;
    private String envName;
    private String ip;
    private String port;
    private String username;
    private String password;
    private String serverDesc;
    private String updateBy;
    private String updateTime;
    private List<MicroServiceInfo> microServiceList;


    @Data
    public static class MicroServiceInfo {
        private String projectName;
        private String appName;
        private String appPort;
        private String appPath;
        private String appProp;
    }
}
