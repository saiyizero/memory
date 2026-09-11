package com.murong.ecp.tools.fx.domain.service.different;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.domain.factory.TableEntityFactory;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.enums.DiffTypeEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.rpc.TableDiffRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDataPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDiffPO;
import com.murong.ecp.tools.fx.infrastructure.view.SelectDialogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;

/**
 * 表结构差异对比服务
 */
@Service
public class TableDiffService {

    @Autowired
    private TableDiffRpcService tableDiffRpcService;
    
    @Autowired
    private TableEntityFactory tableEntityFactory;
    
    @Autowired
    private GlobalProperties globalProps;

    @Autowired
    private RemoteDatabaseService remoteDatabaseService;
    
    @Autowired
    private DdlGenerateService ddlGenerateService;

    /**
     * 对比表结构差异（带对比选项和进度回调）
     */
    public CrResult<List<TableDiffPO>> compareTableStructure(List<TableDataPO> selectedTables, 
                                                           SelectDialogUtil.TableCompareOptions compareOptions,
                                                           java.util.function.BiConsumer<Double, String> progressCallback) {
        try {
            List<TableDiffPO> diffList = new ArrayList<>();
            String schemaNm = globalProps.getDDLSchema();

            // 对比前删除已存在的记录
            if (progressCallback != null) {
                progressCallback.accept(0.05, "正在删除旧差异记录...");
            }
            deleteExistingDiffRecords(selectedTables, compareOptions.getCompareEnv(), schemaNm);

            // 提取所有表名
            List<String> tableNames = new ArrayList<>();
            for (TableDataPO localTable : selectedTables) {
                String tableName = localTable.getTableNameSnake();
                if (tableName != null && !tableName.trim().isEmpty()) {
                    tableNames.add(tableName);
                }
            }

            // 批量获取远程表结构
            if (progressCallback != null) {
                progressCallback.accept(0.1, "正在连接远程数据库...");
            }
            Map<String, TableEntity> remoteTableMap = remoteDatabaseService.batchGetRemoteTableStructure(
                tableNames, compareOptions.getCompareEnv(), progressCallback != null ? 
                (progress, tableName) -> {
                    // 将远程获取进度映射到10%-40%的范围
                    double mappedProgress = 0.1 + (progress * 0.3);
                    progressCallback.accept(mappedProgress, "正在对比远程表结构: " + tableName);
                } : null
            );

            // 逐个对比表结构
            int totalTables = selectedTables.size();
            int processedTables = 0;
            
            for (TableDataPO localTable : selectedTables) {
                processedTables++;
                double progress = 0.4 + ((double) processedTables / totalTables) * 0.6; // 40%-100%
                
                String tableName = localTable.getTableNameSnake();
                if (tableName == null || tableName.trim().isEmpty()) {
                    continue;
                }

                // 报告当前处理的表
                if (progressCallback != null) {
                    progressCallback.accept(progress, "正在对比表结构: " + tableName + " (" + processedTables + "/" + totalTables + ")");
                }

                // 获取本地表结构
                TableEntity localTableEntity = tableEntityFactory.getTableEntity(tableName);
                localTableEntity=tableEntityFactory.assembleCommonClass(localTableEntity);
                if (localTableEntity == null) {
                    continue;
                }


                // 根据对比选项处理本地表结构
                localTableEntity = processTableEntityByOptions(localTableEntity, compareOptions);

                // 获取远程表结构
                TableEntity remoteTableEntity = remoteTableMap.get(tableName);
                if (remoteTableEntity == null) {
                    // 表在远程环境中不存在，记录为删除
                    TableDiffPO diff = createDiffRecord(localTable, compareOptions.getCompareEnv(), DiffTypeEnum.STRUCTURE_DIFF, null,
                        "表在远程环境中不存在", tableName, null, localTableEntity.getRxFields().size());
                    diffList.add(diff);
                    continue;
                }

                // 根据对比选项处理远程表结构
                remoteTableEntity = processTableEntityByOptions(remoteTableEntity, compareOptions);

                // 对比表结构差异（使用DDL对比）
                TableDiffPO tableDiff = compareTableStructureByDdl(localTableEntity, remoteTableEntity, localTable, compareOptions.getCompareEnv());
                // 无论是否有差异，都添加记录
                diffList.add(tableDiff);
            }

            // 保存差异记录
            if (progressCallback != null) {
                progressCallback.accept(0.95, "正在保存差异记录...");
            }
            if (!diffList.isEmpty()) {
                tableDiffRpcService.batchSave(diffList);
            }

            // 清理远程数据库连接
            if (progressCallback != null) {
                progressCallback.accept(1.0, "正在清理连接...");
            }
            cleanupRemoteConnection(compareOptions.getCompareEnv());

            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
            crResult.setData(diffList);
            return crResult;

        } catch (Exception e) {
            // 发生异常时也要清理连接
            cleanupRemoteConnection(compareOptions.getCompareEnv());
            e.printStackTrace();
            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            crResult.setMsgInf("对比表结构失败: " + e.getMessage());
            return crResult;
        }
    }

    /**
     * 对比表结构差异（带对比选项）
     */
    public CrResult<List<TableDiffPO>> compareTableStructure(List<TableDataPO> selectedTables, 
                                                           SelectDialogUtil.TableCompareOptions compareOptions) {
        return compareTableStructure(selectedTables, compareOptions, null);
    }

    /**
     * 对比表结构差异（兼容旧版本）
     */
    public CrResult<List<TableDiffPO>> compareTableStructure(List<TableDataPO> selectedTables, String compareEnv) {
        // 创建默认的对比选项
        SelectDialogUtil.TableCompareOptions defaultOptions = new SelectDialogUtil.TableCompareOptions();
        defaultOptions.setCompareEnv(compareEnv);
        defaultOptions.setCompareNullable(true);
        defaultOptions.setCompareDefaultValue(true);
        defaultOptions.setCompareNonPrimaryIndex(true);
        defaultOptions.setCompareComment(true);
        
        return compareTableStructure(selectedTables, defaultOptions, null);
    }

    /**
     * 根据对比选项处理表结构
     */
    private TableEntity processTableEntityByOptions(TableEntity tableEntity, SelectDialogUtil.TableCompareOptions options) {
        if (tableEntity == null || tableEntity.getRxFields() == null) {
            return tableEntity;
        }

        // 处理字段的nullable和defaultValue
        for (RxField field : tableEntity.getRxFields()) {
            if (!options.isCompareNullable()) {
                field.setNotNull(false); // 设置为允许为空
            }
            if (!options.isCompareDefaultValue()) {
                field.setDefaultValue(null); // 清除默认值
            }
            if (!options.isCompareComment()) {
                field.setCommentCn(null); // 清除中文注释
                field.setCommentEn(null); // 清除英文注释
            }
        }

        // 处理表注释
        if (!options.isCompareComment()) {
            tableEntity.setTableCommentCn(null); // 清除表中文注释
            tableEntity.setTableCommentEn(null); // 清除表英文注释
        }

        // 处理非主键索引
        if (!options.isCompareNonPrimaryIndex()) {
            // 清除非主键索引
            if (tableEntity.getIndexes() != null) {
                tableEntity.setIndexes(new ArrayList<>());
            }
            if (tableEntity.getUniqueIndexes() != null) {
                tableEntity.setUniqueIndexes(new ArrayList<>());
            }
        }

        return tableEntity;
    }
    
    /**
     * 获取远程表结构
     * 
     * @param tableName 表名
     * @param compareEnv 对比环境
     * @return 远程表结构，如果表不存在返回null，如果连接失败抛出异常
     */
    private TableEntity getRemoteTableStructure(String tableName, String compareEnv) {
        return remoteDatabaseService.getRemoteTableStructure(tableName, compareEnv);
    }

    /**
     * 使用DDL对比表结构差异
     */
    private TableDiffPO compareTableStructureByDdl(TableEntity localTable, TableEntity remoteTable,
                                                 TableDataPO localTablePO, String compareEnv) {
        try {
            // 标准化字段顺序，确保两边的字段顺序一致
            localTable = ddlGenerateService.standardizeFieldOrder(localTable);
            remoteTable = ddlGenerateService.standardizeFieldOrder(remoteTable);
            
            // 生成DDL语句
            String localDdl = ddlGenerateService.generatePostgresqlDdl(localTable);
            String remoteDdl = ddlGenerateService.generatePostgresqlDdl(remoteTable);
            
            // 对比DDL语句
            if (localDdl.equals(remoteDdl)) {
                // 没有差异，创建无差异记录
                return createDiffRecord(localTablePO, compareEnv, DiffTypeEnum.NO_DIFF, null,
                    "表结构完全一致，无差异", "无差异", "无差异", 0);
            }
            
            // 有差异，创建差异记录
            StringBuilder diffDetail = new StringBuilder();
            diffDetail.append("表结构存在差异，请查看详情对比。\n");
            diffDetail.append("本地表字段数: ").append(localTable.getRxFields().size()).append("\n");
            diffDetail.append("远程表字段数: ").append(remoteTable.getRxFields().size()).append("\n");
            
            // 检查字段差异
            List<String> localFieldNames = new ArrayList<>();
            List<String> remoteFieldNames = new ArrayList<>();
            
            for (RxField field : localTable.getRxFields()) {
                localFieldNames.add(field.getNameSnake());
            }
            for (RxField field : remoteTable.getRxFields()) {
                remoteFieldNames.add(field.getNameSnake());
            }
            
            // 找出新增和删除的字段
            List<String> addedFields = new ArrayList<>();
            List<String> deletedFields = new ArrayList<>();
            
            for (String fieldName : remoteFieldNames) {
                if (!localFieldNames.contains(fieldName)) {
                    addedFields.add(fieldName);
                }
            }
            
            for (String fieldName : localFieldNames) {
                if (!remoteFieldNames.contains(fieldName)) {
                    deletedFields.add(fieldName);
                }
            }
            
            // 计算字段差异数量
            int diffFieldCount = addedFields.size() + deletedFields.size();
            
            if (!addedFields.isEmpty()) {
                diffDetail.append("新增字段: ").append(String.join(", ", addedFields)).append("\n");
            }
            if (!deletedFields.isEmpty()) {
                diffDetail.append("删除字段: ").append(String.join(", ", deletedFields)).append("\n");
            }
            
            return createDiffRecord(localTablePO, compareEnv, DiffTypeEnum.STRUCTURE_DIFF, null,
                diffDetail.toString(), localDdl, remoteDdl, diffFieldCount);
                
        } catch (Exception e) {
            throw new RuntimeException(e);
            // 如果DDL生成失败，记录错误
        }
    }
    

    
    /**
     * 创建差异记录（带字段差异数量）
     */
    private TableDiffPO createDiffRecord(TableDataPO localTable, String compareEnv, DiffTypeEnum diffType, 
                                       String fieldName, String detail, String localValue, String remoteValue, Integer diffFieldCount) {
        TableDiffPO diff = new TableDiffPO();
        diff.setGroupName(localTable.getGroupName());
        diff.setProjectName(localTable.getProjectName());
        diff.setAppName(localTable.getAppName());
        diff.setTableNameCamel(localTable.getTableNameCamel());
        diff.setTableNameSnake(localTable.getTableNameSnake());
        diff.setSchemaNm(globalProps.getDDLSchema());
        diff.setCompareEnv(compareEnv);
        diff.setDiffType(diffType);
        diff.setLocalValue(localValue);
        diff.setRemoteValue(remoteValue);
        diff.setDiffFieldCount(diffFieldCount);
        return diff;
    }
    
    /**
     * 创建差异记录（兼容旧版本，字段差异数量默认为0）
     */
    private TableDiffPO createDiffRecord(TableDataPO localTable, String compareEnv, DiffTypeEnum diffType, 
                                       String fieldName, String detail, String localValue, String remoteValue) {
        return createDiffRecord(localTable, compareEnv, diffType, fieldName, detail, localValue, remoteValue, 0);
    }
    
    /**
     * 查询差异记录
     */
    public CrResult<List<TableDiffPO>> queryDiffRecords(TableDiffPO query) {
        try {
            List<TableDiffPO> records = tableDiffRpcService.queryForListWithCustomMapper(query);
            
            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
            crResult.setData(records);
            return crResult;
        } catch (Exception e) {
            e.printStackTrace();
            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            crResult.setMsgInf("查询差异记录失败: " + e.getMessage());
            return crResult;
        }
    }

    /**
     * 删除已存在的差异记录
     */
    private void deleteExistingDiffRecords(List<TableDataPO> selectedTables, String compareEnv, String schemaNm) {
        try {
            for (TableDataPO table : selectedTables) {
                // 构建删除条件
                TableDiffPO deleteQuery = new TableDiffPO();
                deleteQuery.setGroupName(table.getGroupName());
                deleteQuery.setProjectName(table.getProjectName());
                deleteQuery.setTableNameCamel(table.getTableNameCamel());
                deleteQuery.setSchemaNm(schemaNm);
                deleteQuery.setCompareEnv(compareEnv);
                
                // 执行删除
                tableDiffRpcService.delete(deleteQuery);
            }
            System.out.println("已删除 " + selectedTables.size() + " 张表的旧差异记录");
        } catch (Exception e) {
            System.err.println("删除旧差异记录失败: " + e.getMessage());
            // 不抛出异常，继续执行对比
        }
    }
    
    /**
     * 清理远程数据库连接
     */
    private void cleanupRemoteConnection(String compareEnv) {
        try {
            remoteDatabaseService.closeConnection(compareEnv);
        } catch (Exception e) {
            System.err.println("清理远程数据库连接失败: " + e.getMessage());
        }
    }
} 