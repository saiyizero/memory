# Apis Standard Interface

**Contacts**:Beijing MuRong Information Technology Co., Ltd.

**Version**:3.6


| Revision date | Change number | Change description| Revision method |Revision statement | Reviser |
| -------- | -------- | ----- | -------- | -------- | ------ |
| | | | | | |

Revision method： Add/Modify/Delete


## API list

* The following are the interfaces.

|  API  | Introduction |
|------ |----- |
<#if interfaceList??>
<#list interfaceList as interface>
|[${interface.transName}](#${interface.transName})| ${interface.transCommentZh}|
</#list>
</#if>

***


## Interface Details

<#if interfaceList??>
<#list interfaceList as interface>
* <span id="${interface.transName}">**${interface.transName}**</span>

**Endpoint URL**: ${interface.properties.interfaceUrl}${interface.properties.methodUrl}
**Response Format**: Json
**Request Method**: Post
**Interface Description**: ${interface.transCommentZh}


* Request rxFields：
| No. | 字段名 | 类型 | 长度 | 必填 | 枚举值 | 中文说明 | 英文说明 |
| --- | ------ | ---- | ---- | ---- | ------ | -------- | -------- |
<#if interface.request??>
  <#list interface.request as req>
|${req_index + 1}|${req.nameCamel}|${req.type}|${req.length!""}|${req.notNull?string('true','false')}|${req.enumNme!""}|${req.commentCn!""}|${req.commentEn!""}|
  </#list>
</#if>


* Response rxFields：
| No. | 字段名 | 类型 | 长度 | 必填 | 枚举值 | 中文说明 | 英文说明 |
| --- | ------ | ---- | ---- | ---- | ------ | -------- | -------- |
<#if interface.response??>
  <#list interface.response as res>
|${res_index + 1}|${res.nameCamel}|${res.type}|${res.length!""}|${res.notNull?string('true','false')}|${res.enumNme!""}|${res.commentCn!""}|${res.commentEn!""}|
  </#list>
</#if>

**Request Example**:

```json
${interface.requestExample!""}
```

**Response Example**:

```json
${interface.responseExample!""}
```

---

</#list>
</#if>

<#-- =================== 枚举说明 =================== -->

## 枚举类型明细

<#if enumGroupMap?? && enumGroupMap?size gt 0>
<#list enumGroupMap?keys as groupKey>
  <#assign group = enumGroupMap[groupKey]>
### ${group[0].enumNme!""}（${group[0].dbName!""}）

| 键 | 值 | 中文 | 英文 |
| --- | --- | --- | --- |
<#list group as enum>
| ${enum.enumCd!""}| ${enum.enumVal!""} | ${enum.descCn!""} | ${enum.descEn!""} |
</#list>
</#list>
</#if>