package com.murong.ecp.tools.fx.infrastructure.rpc;

import lombok.Data;

/**
 * 服务端请求上下文，由 HTTP 头填充，供 DAO/Service 读取当前用户与项目信息。
 */
public final class ServiceRequestContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private ServiceRequestContext() {
    }

    public static void set(Context context) {
        HOLDER.set(context);
    }

    public static Context get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Data
    public static class Context {
        private String userId;
        private String username;
        private String realName;
        private String roles;
        private String groupName;
        private String projectName;
        private String appName;
        private com.murong.ecp.tools.fx.domain.entity.DbConfig dbConfig;
    }
}
