# ${transName}

## Interface

- **API Name**: ${transName}
- **Endpoint URL**: ${properties.interfaceUrl}${properties.methodUrl}
- **Description**: ${transCommentEn!''}

## Request

| No. | Field           | Required | Type   | Length | Default | Description  |
| --- | -------------- | -------- | ------ | ------ | ------- | ------------ |
<#if request?? && (request?size > 0)>
  <#list request as field>
|${field_index + 1}|<font color=red>${field.nameCamel}</font>|${field.notNull?string('true','false')}|${field.type}|${field.length!}|${field.defaultValue!}|${field.commentEn!}|
  </#list>
</#if>

**Example**

```json
${requestExample!''}
```

## Response

| No. | Field           | Required | Type   | Length | Default | Description  |
| --- | -------------- | -------- | ------ | ------ | ------- | ------------ |
<#if response?? && (response?size > 0)>
  <#list response as field>
|${field_index + 1}|<font color=red>${field.nameCamel}</font>|${field.notNull?string('true','false')}|${field.type}|${field.length!}|${field.defaultValue!}|${field.commentEn!}|
  </#list>
</#if>

**Example**

```json
${responseExample!''}
```

