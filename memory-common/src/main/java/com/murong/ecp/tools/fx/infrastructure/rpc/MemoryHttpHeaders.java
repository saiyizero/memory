package com.murong.ecp.tools.fx.infrastructure.rpc;

/**
 * Client 与 Service 之间传递会话上下文的 HTTP 头。
 */
public final class MemoryHttpHeaders {
    public static final String USER_ID = "X-Memory-UserId";
    public static final String USERNAME = "X-Memory-Username";
    public static final String REAL_NAME = "X-Memory-RealName";
    public static final String ROLES = "X-Memory-Roles";
    public static final String GROUP_NAME = "X-Memory-GroupName";
    public static final String PROJECT_NAME = "X-Memory-ProjectName";
    public static final String APP_NAME = "X-Memory-AppName";

    private MemoryHttpHeaders() {
    }
}
