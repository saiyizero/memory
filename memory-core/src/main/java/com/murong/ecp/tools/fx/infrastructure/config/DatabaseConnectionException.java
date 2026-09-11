package com.murong.ecp.tools.fx.infrastructure.config;

/**
 * 数据库连接异常
 * 当数据库连接失败时抛出此异常，用于触发数据库配置对话框
 */
public class DatabaseConnectionException extends RuntimeException {
    
    public DatabaseConnectionException(String message) {
        super(message);
    }
    
    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
