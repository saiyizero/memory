package com.murong.ecp.tools.fx.domain.service.database;

import com.murong.ecp.tools.fx.infrastructure.utils.SqliteBackupUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * SQLite备份服务
 */
@Service
public class SqliteBackupService {

    @Autowired
    private SqliteBackupUtil sqliteBackupUtil;

    /**
     * 从SQL脚本导入数据
     * @param targetDbPath 目标数据库路径
     * @param sqlPath SQL脚本路径
     * @return 是否成功
     */
    public boolean importFromSqlScript(String targetDbPath, String sqlPath) {
        return sqliteBackupUtil.importFromSqlScript(targetDbPath, sqlPath);
    }
} 