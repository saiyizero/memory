create table ${table.tableNameSnake}
(
<#list table.rxFields as rxField>
    ${rxField.name}    ${rxField.type}<#if rxField.default??> default ${rxField.default}</#if><#if rxField.notNull> not null</#if><#if field_has_next>,</#if>
</#list>
<#if table.primaryKey?size gt 0>,
    constraint PK_${table.tableNameSnake}
        primary key (
            <#list table.primaryKey as pk>${table.rxFields?filter(f -> f.nameCamel == pk)[0].name}<#if pk_has_next>, </#if></#list>
        )
</#if>
);

<#-- 字段注释 -->
<#list table.rxFields as rxField>
<#if rxField.commentCn?? && rxField.commentCn?length gt 0>
comment on column ${table.tableNameSnake}.${rxField.name} is '${rxField.commentCn}|${rxField.commentEn}';
</#if>
</#list>

<#-- 索引定义 -->
<#if table.indexes??>
<#list table.indexes as idx>
<#if idx.fields?? && idx.fields?has_content>
create index ${idx.name}
    on ${table.tableNameSnake} (<#list idx.fields as f>${table.rxFields?filter(rxField -> rxField.nameCamel == f)[0].name}<#if f_has_next>, </#if></#list>)
;
</#if>
</#list>
</#if>

<#-- 
  本模板用于批量生成Oracle数据库建表脚本。
  示例数据结构：
  {
    "table": {
      "tableNameCamel": "AcmCddt",
      "tableNameSnake": "T_ACM_CDDT",
      "tableCommentCn": "存款收支明细表",
      "tableCommentEn": "Deposit and Withdrawal Details Table",
      "rxFields": [
        {"nameCamel": "jrnNo", "name": "JRN_NO", "type": "VARCHAR2(32)", "commentCn": "流水号", "commentEn": "Journal number"},
        {"nameCamel": "sysDt", "name": "SYS_DT", "type": "DATE", "commentCn": "系统日期", "commentEn": "System date"}
      ],
      "primaryKey": ["jrnNo", "jrnSeq", "acDt"],
      "indexes": [
        {"name": "NI1_T_ACM_CDDT", "rxFields": ["sepCd", "acDt"]},
        {"name": "NI2_T_ACM_CDDT", "rxFields": ["sysDt", "sepCd"]}
      ]
    }
  }
  变量说明：
    table.tableNameSnake      表名
    table.rxFields              字段列表（含name、type、commentCn、commentEn等）
    table.primaryKey          主键字段列表（nameCamel）
    table.indexes             索引列表（含索引名、字段列表）
-->