<#assign tableFullName = (table.schema?? && table.schema?has_content)?then(table.schema + "." + table.tableNameSnake, table.tableNameSnake)>
CREATE TABLE ${tableFullName}
(
<#list table.rxFields as rxField>
    <#if rxField.nameSnake?? && rxField.nameSnake?has_content>${rxField.nameSnake?right_pad(18)}<#else>field_${rxField_index + 1}?right_pad(18)</#if> <#if rxField.dbTyp?? && rxField.dbTyp?has_content>${rxField.dbTyp}<#else>VARCHAR</#if><#if rxField.dbTyp?? && rxField.dbTyp?upper_case != "TEXT" && rxField.dbTyp?upper_case != "LONGTEXT" && rxField.dbTyp?upper_case != "MEDIUMTEXT" && rxField.dbTyp?upper_case != "TINYTEXT" && rxField.length?? && rxField.length?string?replace(',', '')?number gt 0>(${rxField.length?string?replace(',', '')})</#if><#if rxField.defaultValue?? && rxField.defaultValue?has_content>   default ${rxField.defaultValue}</#if><#if rxField.notNull?? && rxField.notNull>  not null</#if><#if rxField_has_next || (table.primaryKey?? && table.primaryKey.fields?has_content)>,</#if>
</#list>
<#if table.primaryKey?? && table.primaryKey.fields?has_content>
    CONSTRAINT pk_${table.tableNameSnake} PRIMARY KEY (<#list table.primaryKey.fields as pk>${pk}<#if pk_has_next>, </#if></#list>)
</#if>
);

<#if table.tableCommentCn?? && table.tableCommentCn?has_content>
COMMENT ON TABLE ${tableFullName} IS '${table.tableCommentCn}<#if table.tableCommentEn?? && table.tableCommentEn?has_content>|${table.tableCommentEn}</#if>';
</#if>
<#-- 字段注释 -->
<#list table.rxFields as rxField>
<#if rxField.commentCn?? && rxField.commentCn?length gt 0>
COMMENT ON COLUMN ${tableFullName}.${rxField.nameSnake} IS '${rxField.commentCn}<#if rxField.commentEn?? && rxField.commentEn?length gt 0>|${rxField.commentEn}</#if>';
</#if>
</#list>
<#-- 索引定义 -->
<#if table.indexes??>
<#list table.indexes as idx>
<#if idx.fields?? && idx.fields?has_content>
CREATE <#if idx.type == 'unique' || idx.type == 'UNIQUE'>UNIQUE </#if>INDEX <#if idx.name?? && idx.name?has_content>${idx.name}<#else>idx_${table.tableNameSnake}_${idx_index + 1}</#if> ON ${tableFullName} (<#list idx.fields as f>${f}<#if f_has_next>, </#if></#list>);
</#if>
</#list>
</#if>