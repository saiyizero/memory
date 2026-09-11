<?xml version="1.0" encoding="UTF-8"?>
<mapper namespace="${table.properties.mapperPath}.${table.tableNameCamel}Mapper">
    <select id="selectByPrimaryKey" resultType="${table.properties.entityPath}.${table.tableNameCamel}PO" parameterType="map">
        select <include refid="base_column_list"/>
        from ${table.tableNameSnake} where <#list table.primaryKey.fields as pk>${table.rxFields?filter(f -> f.nameSnake == pk)[0].nameSnake} = ${ "#{"+ table.rxFields?filter(f -> f.nameSnake == pk)[0].nameCamel+ "}" }<#if pk_has_next> and </#if></#list>
    </select>

    <select id="selectByPage" resultType="${table.properties.entityPath}.${table.tableNameCamel}PO">
        select <include refid="base_column_list"/>
        from ${table.tableNameSnake} <include refid="where_condition"/>
        <#if table.defOrderBy?? && table.defOrderBy?length gt 0>
            order by ${table.defOrderBy}
        </#if>
    </select>

    <select id="selectResultHandler" resultType="${table.properties.entityPath}.${table.tableNameCamel}PO">
        select <include refid="base_column_list"/>
        from ${table.tableNameSnake} <include refid="where_condition"/>
        <#if table.defOrderBy?? && table.defOrderBy?length gt 0>
            order by ${table.defOrderBy}
        </#if>
    </select>

    <delete id="deleteByPrimaryKey" parameterType="map">
        delete from ${table.tableNameSnake} where <#list table.primaryKey.fields as pk>${table.rxFields?filter(f -> f.nameSnake == pk)[0].nameSnake} = ${ "#{"+ table.rxFields?filter(f -> f.nameSnake == pk)[0].nameCamel+ "}" }<#if pk_has_next> and </#if></#list>
    </delete>

    <update id="updateByPrimaryKey" parameterType="${table.properties.entityPath}.${table.tableNameCamel}PO">
        update ${table.tableNameSnake} set
        <#list table.rxFields as rxField><#if !(table.primaryKey.fields?seq_contains(rxField.nameSnake))>${rxField.nameSnake}=${ "#{"+rxField.nameCamel+"}"}<#if rxField_has_next>, </#if></#if></#list>
        where <#list table.primaryKey.fields as pk>${table.rxFields?filter(f -> f.nameSnake == pk)[0].nameSnake} = ${ "#{"+ table.rxFields?filter(f -> f.nameSnake == pk)[0].nameCamel +"}" }<#if pk_has_next> and </#if></#list>
    </update>

    <update id="updateByPrimaryKeySelective" parameterType="${table.properties.entityPath}.${table.tableNameCamel}PO">
        update ${table.tableNameSnake}
        <trim prefix="set" suffixOverrides=",">
            <#list table.rxFields as rxField>
            <#if !(table.primaryKey.fields?seq_contains(rxField.nameCamel))>
            <if test="${rxField.nameCamel} != null<#if rxField.type=="String"> and ${rxField.nameCamel} != ''</#if>">${rxField.nameSnake}=${ "#{"+rxField.nameCamel+"}" },</if>
            </#if>
            </#list>
        </trim>
        where <#list table.primaryKey.fields as pk>${table.rxFields?filter(f -> f.nameSnake == pk)[0].nameSnake} = ${ "#{"+table.rxFields?filter(f -> f.nameSnake == pk)[0].nameCamel+"}" }<#if pk_has_next> and </#if></#list>
    </update>

    <insert id="insert" parameterType="${table.properties.entityPath}.${table.tableNameCamel}PO">
        insert into ${table.tableNameSnake}(<#list table.rxFields as rxField>${rxField.nameSnake}<#if rxField_has_next>, </#if></#list>)
             values (<#list table.rxFields as rxField>${ "#{"+rxField.nameCamel+"}"}<#if rxField_has_next>, </#if></#list>)
    </insert>

    <insert id="insertSelective" parameterType="${table.properties.entityPath}.${table.tableNameCamel}PO">
        insert into ${table.tableNameSnake}
        <trim prefix="(" suffix=")" suffixOverrides=",">
            <#list table.rxFields as rxField>
            <if test="${rxField.nameCamel} != null<#if rxField.type=="String"> and ${rxField.nameCamel} != ''</#if>">${rxField.nameSnake},</if>
            </#list>
        </trim>
        <trim prefix="values (" suffix=")" suffixOverrides=",">
            <#list table.rxFields as rxField>
            <if test="${rxField.nameCamel} != null<#if rxField.type=="String"> and ${rxField.nameCamel} != ''</#if>">${"#{"+rxField.nameCamel+"}"},</if>
            </#list>
        </trim>
    </insert>

    <sql id="base_column_list">
        <#list table.rxFields as rxField>${rxField.nameSnake}<#if rxField_has_next>,</#if></#list>
    </sql>

    <sql id="where_condition">
        <trim prefix="where" prefixOverrides="and">
            <#list table.rxFields as rxField>
            <if test="${rxField.nameCamel} != null<#if rxField.type=="String"> and ${rxField.nameCamel} != ''</#if>"> and ${rxField.nameSnake}=${"#{"+rxField.nameCamel+"}"} </if>
            </#list>
        </trim>
    </sql>
</mapper>

<#--
示例数据结构：
{
  "table": {
    "tableNameCamel": "tAcmCddt",
    "tableNameSnake": "T_ACM_CDDT",
    "tableCommentCn": "存款收支明细",
    "tableCommentEn": "Deposit income and expenditure details",
    "rxFields": [
      { "nameCamel": "jrnNo", "nameSnake": "JRN_NO", "type": "String", "commentCn": "JRN_NO", "commentEn": "Journal number", "dbTyp": "VARCHAR", "length": 32 },
      { "nameCamel": "sysDt", "nameSnake": "SYS_DT", "type": "String", "commentCn": "DATE", "commentEn": "System date", "dbTyp": "VARCHAR", "length": 8 },
      { "nameCamel": "sysTm", "nameSnake": "SYS_TM", "type": "String", "commentCn": "TIME", "commentEn": "System time", "dbTyp": "VARCHAR", "length": 6 },
      { "nameCamel": "jrnSeq", "nameSnake": "JRN_SEQ", "type": "YGAmt", "commentCn": "JRN_SEQ", "commentEn": "Journal sequence", "dbTyp": "DECIMAL", "length": 38 },
      { "nameCamel": "capTyp", "nameSnake": "CAP_TYP", "type": "String", "commentCn": "CAP_TYP", "commentEn": "Capital type", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "ccy", "nameSnake": "CCY", "type": "String", "commentCn": "CCY", "commentEn": "Currency", "dbTyp": "VARCHAR", "length": 3 },
      { "nameCamel": "acTyp", "nameSnake": "AC_TYP", "type": "String", "commentCn": "AC_TYP", "commentEn": "Account type", "dbTyp": "VARCHAR", "length": 3 },
      { "nameCamel": "prdNo", "nameSnake": "PRD_NO", "type": "String", "commentCn": "PRD_NO", "commentEn": "Product number", "dbTyp": "VARCHAR", "length": 10 },
      { "nameCamel": "txTyp", "nameSnake": "TX_TYP", "type": "String", "commentCn": "TX_TYP", "commentEn": "Transaction type", "dbTyp": "VARCHAR", "length": 2 },
      { "nameCamel": "busTyp", "nameSnake": "BUS_TYP", "type": "String", "commentCn": "BUS_TYP", "commentEn": "Business type", "dbTyp": "VARCHAR", "length": 4 },
      { "nameCamel": "orgJrnNo", "nameSnake": "ORG_JRN_NO", "type": "String", "commentCn": "JRN_NO", "commentEn": "Organization journal number", "dbTyp": "VARCHAR", "length": 32 },
      { "nameCamel": "orgTxDt", "nameSnake": "ORG_TX_DT", "type": "String", "commentCn": "DATE", "commentEn": "Organization transaction date", "dbTyp": "VARCHAR", "length": 8 },
      { "nameCamel": "cnlTxCd", "nameSnake": "CNL_TX_CD", "type": "String", "commentCn": "TX_CD", "commentEn": "Channel transaction code", "dbTyp": "VARCHAR", "length": 64 },
      { "nameCamel": "orgCd", "nameSnake": "ORG_CD", "type": "String", "commentCn": "AC_ORG", "commentEn": "Organization code", "dbTyp": "VARCHAR", "length": 10 },
      { "nameCamel": "acOrgCd", "nameSnake": "AC_ORG_CD", "type": "String", "commentCn": "AC_ORG", "commentEn": "Account organization code", "dbTyp": "VARCHAR", "length": 10 },
      { "nameCamel": "aeSeq", "nameSnake": "AE_SEQ", "type": "String", "commentCn": "AE_SEQ", "commentEn": "Account sequence", "dbTyp": "VARCHAR", "length": 6 },
      { "nameCamel": "sepCd", "nameSnake": "SEP_CD", "type": "String", "commentCn": "SEP_CD", "commentEn": "Separate code", "dbTyp": "VARCHAR", "length": 32 },
      { "nameCamel": "cdJrnSts", "nameSnake": "CD_JRN_STS", "type": "String", "commentCn": "JRN_STS", "commentEn": "Journal status", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "acDt", "nameSnake": "AC_DT", "type": "String", "commentCn": "DATE", "commentEn": "Accounting date", "dbTyp": "VARCHAR", "length": 8 },
      { "nameCamel": "rvsTxFlg", "nameSnake": "RVS_TX_FLG", "type": "String", "commentCn": "RVS_TX_FLG", "commentEn": "Reversal transaction flag", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "rvsFlgTyp", "nameSnake": "RVS_FLG_TYP", "type": "String", "commentCn": "RVS_FLG", "commentEn": "Reversal type", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "rvsJrn", "nameSnake": "RVS_JRN", "type": "String", "commentCn": "JRN_NO", "commentEn": "Reversal journal number", "dbTyp": "VARCHAR", "length": 32 },
      { "nameCamel": "rvsJrnSeq", "nameSnake": "RVS_JRN_SEQ", "type": "YGAmt", "commentCn": "JRN_SEQ", "commentEn": "Reversal journal sequence", "dbTyp": "DECIMAL", "length": 38 },
      { "nameCamel": "txCd", "nameSnake": "TX_CD", "type": "String", "commentCn": "TX_CD", "commentEn": "Transaction code", "dbTyp": "VARCHAR", "length": 64 },
      { "nameCamel": "drAmt", "nameSnake": "DR_AMT", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Debit amount", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "crAmt", "nameSnake": "CR_AMT", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Credit amount", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "bal", "nameSnake": "BAL", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Balance", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "odAmt", "nameSnake": "OD_AMT", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Overdraft amount", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "notTxAmt", "nameSnake": "NOT_TX_AMT", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Non-withdrawable amount", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "uavaBal", "nameSnake": "UAVA_BAL", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Unavailable balance", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "lastAvaBal", "nameSnake": "LAST_AVA_BAL", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Available balance on last day", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "holdAmt", "nameSnake": "HOLD_AMT", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Hold amount", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "txDesc", "nameSnake": "TX_DESC", "type": "String", "commentCn": "DESC_LONG", "commentEn": "Transaction description", "dbTyp": "VARCHAR", "length": 256 },
      { "nameCamel": "ordTyp", "nameSnake": "ORD_TYP", "type": "String", "commentCn": "ORD_TYP", "commentEn": "Order type", "dbTyp": "VARCHAR", "length": 2 },
      { "nameCamel": "ordNo", "nameSnake": "ORD_NO", "type": "String", "commentCn": "ORD_NO", "commentEn": "Order number", "dbTyp": "VARCHAR", "length": 32 },
      { "nameCamel": "txAmt", "nameSnake": "TX_AMT", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Transaction amount", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "busCnl", "nameSnake": "BUS_CNL", "type": "String", "commentCn": "BUS_CNL", "commentEn": "Business channel", "dbTyp": "VARCHAR", "length": 4 },
      { "nameCamel": "sysCnl", "nameSnake": "SYS_CNL", "type": "String", "commentCn": "SYS_CNL", "commentEn": "System channel", "dbTyp": "VARCHAR", "length": 4 },
      { "nameCamel": "hldNo", "nameSnake": "HLD_NO", "type": "String", "commentCn": "HLD_NO", "commentEn": "Hold number", "dbTyp": "VARCHAR", "length": 32 },
      { "nameCamel": "hldDcFlg", "nameSnake": "HLD_DC_FLG", "type": "String", "commentCn": "DC_FLG", "commentEn": "Hold of debit and credit flag", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "ordSeq", "nameSnake": "ORD_SEQ", "type": "YGAmt", "commentCn": "ORD_SEQ", "commentEn": "Order sequence", "dbTyp": "DECIMAL", "length": 12 },
      { "nameCamel": "tmSmp", "nameSnake": "TM_SMP", "type": "String", "commentCn": "TM_SMP", "commentEn": "Time sample", "dbTyp": "VARCHAR", "length": 26 },
      { "nameCamel": "amtKind", "nameSnake": "AMT_KIND", "type": "String", "commentCn": "AMT_KND", "commentEn": "Money kind", "dbTyp": "VARCHAR", "length": 2 },
      { "nameCamel": "rmk", "nameSnake": "RMK", "type": "String", "commentCn": "RMK_MIDDLE", "commentEn": "Remark", "dbTyp": "VARCHAR", "length": 256 },
      { "nameCamel": "lastAcBal", "nameSnake": "LAST_AC_BAL", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Balance of last day account", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "notTxAvaAmt", "nameSnake": "NOT_TX_AVA_AMT", "type": "YGAmt", "commentCn": "AMT", "commentEn": "Can not withdrawal balance", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "cnlFlg", "nameSnake": "CNL_FLG", "type": "String", "commentCn": "CNL_FLG", "commentEn": "channel flag", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "cnlJrn", "nameSnake": "CNL_JRN", "type": "String", "commentCn": "JRN_NO", "commentEn": "Channel journal number", "dbTyp": "VARCHAR", "length": 32 },
      { "nameCamel": "dcFlg", "nameSnake": "DC_FLG", "type": "String", "commentCn": "DC_FLG", "commentEn": "Debit and credit flag", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "notTxFlg", "nameSnake": "NOT_TX_FLG", "type": "String", "commentCn": "NOT_TX_FLG", "commentEn": "Can not transaction flag", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "balOdFlg", "nameSnake": "BAL_OD_FLG", "type": "String", "commentCn": "BAL_OD_FLG", "commentEn": "Battle sequence number", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "nodId", "nameSnake": "NOD_ID", "type": "String", "commentCn": "NOD_ID", "commentEn": "Node identifier", "dbTyp": "VARCHAR", "length": 64 },
      { "nameCamel": "reqId", "nameSnake": "REQ_ID", "type": "String", "commentCn": "REQ_ID", "commentEn": "Request id", "dbTyp": "VARCHAR", "length": 64 },
      { "nameCamel": "updBalFlg", "nameSnake": "UPD_BAL_FLG", "type": "String", "commentCn": "UPD_BAL_FLG", "commentEn": "Update balance flag", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "stlBatNo", "nameSnake": "STL_BAT_NO", "type": "String", "commentCn": "BAT_NO", "commentEn": "Settlement battle number", "dbTyp": "VARCHAR", "length": 32 },
      { "nameCamel": "stlBatSeq", "nameSnake": "STL_BAT_SEQ", "type": "YGAmt", "commentCn": "BAT_SEQ", "commentEn": "Settlement battle sequence", "dbTyp": "DECIMAL", "length": 38 },
      { "nameCamel": "stlSts", "nameSnake": "STL_STS", "type": "String", "commentCn": "STL_STS", "commentEn": "Settlement status", "dbTyp": "VARCHAR", "length": 1 },
      { "nameCamel": "stlDt", "nameSnake": "STL_DT", "type": "String", "commentCn": "DATE", "commentEn": "Settlement date", "dbTyp": "VARCHAR", "length": 8 },
      { "nameCamel": "stlTm", "nameSnake": "STL_TM", "type": "String", "commentCn": "TIME", "commentEn": "Settlement time", "dbTyp": "VARCHAR", "length": 6 },
      { "nameCamel": "usrNo", "nameSnake": "USR_NO", "type": "YGAmt", "commentCn": "USR_NO", "commentEn": "User number", "dbTyp": "DECIMAL", "length": 13 },
      { "nameCamel": "feeAmt", "nameSnake": "FEE_AMT", "type": "YGAmt", "commentCn": "AMT", "commentEn": "fee amount", "dbTyp": "DECIMAL", "length": 18 },
      { "nameCamel": "extRefNo", "nameSnake": "EXT_REF_NO", "type": "String", "commentCn": "EXT_REF_NO", "commentEn": "External business Reference No", "dbTyp": "VARCHAR", "length": 128 },
      { "nameCamel": "acNo", "nameSnake": "AC_NO", "type": "String", "commentCn": "AC_NO", "commentEn": "Account number", "dbTyp": "VARCHAR", "length": 32 },
      { "nameCamel": "oppAcInf", "nameSnake": "OPP_AC_INF", "type": "String", "commentCn": "OPP_AC_INF", "commentEn": "Opponent account information", "dbTyp": "VARCHAR", "length": 256 },
      { "nameCamel": "narCd", "nameSnake": "NAR_CD", "type": "String", "commentCn": "NAR_CD", "commentEn": "Sub business cod", "dbTyp": "VARCHAR", "length": 6 },
      { "nameCamel": "othDesc", "nameSnake": "OTH_DESC", "type": "String", "commentCn": "TEXT_MIDDLE", "commentEn": "Other desc", "dbTyp": "VARCHAR", "length": 1024 },
      { "nameCamel": "reqBusNo", "nameSnake": "REQ_BUS_NO", "type": "String", "commentCn": "REQ_BUS_NO", "commentEn": "Request business number", "dbTyp": "VARCHAR", "length": 64 }
    ],
    "primaryKey": {
      "name": "PK_T_ACM_CDDT",
      "type": "PRIMARY",
      "rxFields": [ "JRN_NO", "JRN_SEQ", "AC_DT" ]
    },
    "indexes": [
      { "name": "NI1_T_ACM_CDDT", "type": "INDEX", "rxFields": [ "SEP_CD", "AC_DT" ] },
      { "name": "NI3_T_ACM_CDDT", "type": "INDEX", "rxFields": [ "AC_ORG_CD", "AC_DT" ] },
      { "name": "NI6_T_ACM_CDDT", "type": "INDEX", "rxFields": [ "USR_NO" ] },
      { "name": "NI4_T_ACM_CDDT", "type": "INDEX", "rxFields": [ "ORG_JRN_NO", "AC_DT" ] },
      { "name": "NI8_T_ACM_CDDT", "type": "INDEX", "rxFields": [ "AC_NO" ] },
      { "name": "NI2_T_ACM_CDDT", "type": "INDEX", "rxFields": [ "SYS_DT", "SEP_CD" ] },
      { "name": "NI7_T_ACM_CDDT", "type": "INDEX", "rxFields": [ "REQ_BUS_NO" ] },
      { "name": "NI5_T_ACM_CDDT", "type": "INDEX", "rxFields": [ "EXT_REF_NO" ] }
    ],
    "uniqueIndexes": null,
    "updateBy": null,
    "updateTime": "2025-06-14 22:46:11.276",
    "properties": {
      "entityPath": "com.murong.ecp.dbs.ccs.infrastructure.db.po",
      "mapperPath": "com.murong.ecp.dbs.ccs.infrastructure.db.mapper",
      "xmlPath": "app/ccs/etc/mapper"
    }
  }
}
--> 