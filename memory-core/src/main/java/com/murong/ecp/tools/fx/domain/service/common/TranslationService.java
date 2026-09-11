package com.murong.ecp.tools.fx.domain.service.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.InterFaceEntity;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.msgcode.TranslationResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.EnumDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.InterfaceDataDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.BizDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import com.murong.ecp.tools.fx.infrastructure.utils.BusinessUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

@Service
public class TranslationService {
    @Autowired
    private GlobalProperties globalPropes;

    @Autowired
    private InterfaceDataDao interfaceDataDao;
    
    @Autowired
    private EnumDictDao enumDictDao;

    @Autowired
    private BizDictDao bizDictDao;

    private static final int MAX_RETRIES = 3; // 最大重试次数


    /**
     * 中文转英文
     */
    public String translateZhToEn(String zh) {
        if (zh == null || zh.trim().isEmpty()) {
            return "";
        }
        String translate = translate(zh, "zh", "en");
        if (StringUtils.isNotBlank(translate)) {
            System.out.println("翻译成功：" + translate);
        }
        return translate;
    }

    /**
     * 英文转中文
     */
    public String translateEnToZh(String en) {
        if (en == null || en.trim().isEmpty()) {
            return "";
        }
        String translate = translate(en, "en", "zh");
        if (StringUtils.isNotBlank(translate)) {
            System.out.println("翻译成功：" + translate);
        }
        return translate;
    }

    /**
     * 批量翻译接口数据
     */
    public TranslationResult translateInterfaceData(Consumer<TranslationProgress> progressCallback) {
        TranslationResult result = new TranslationResult();
        
        try {
            // 查询所有接口数据
            InterfaceDataPO infcDataPO = new InterfaceDataPO();
            infcDataPO.setGroupName(globalPropes.getGroupName());
            infcDataPO.setProjectName(globalPropes.getProjectName());
            List<InterfaceDataPO> interfaceDataList = interfaceDataDao.queryForList(infcDataPO);
            
            if (interfaceDataList.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("未找到任何接口数据");
                return result;
            }
            
            result.setTotalCount(interfaceDataList.size());
            
            // 批量翻译
            for (int i = 0; i < interfaceDataList.size(); i++) {
                InterfaceDataPO interfaceData = interfaceDataList.get(i);
                
                try {
                    // 更新进度
                    if (progressCallback != null) {
                        TranslationProgress progress = new TranslationProgress();
                        progress.setCurrentIndex(i + 1);
                        progress.setTotalCount(interfaceDataList.size());
                        progress.setCurrentItem(interfaceData.getTransName());
                        progress.setTableName("interface_data");
                        progressCallback.accept(progress);
                    }
                    
                    // 直接进行翻译
                    InterFaceEntity entity = convertToInterFaceEntity(interfaceData);
                    CrResult<InterFaceEntity> translationResult = translateInterFaceEntity(entity);
                    
                    if (translationResult.isSucess()) {
                        // 更新翻译结果
                        updateInterfaceDataWithTranslation(interfaceData, translationResult.getData());
                        result.incrementSuccessCount();
                    } else {
                        result.incrementFailCount();
                        result.addFailedItem(interfaceData.getTransName() + " (翻译失败)");
                    }
                    
                } catch (Exception e) {
                    result.incrementFailCount();
                    result.addFailedItem(interfaceData.getTransName() + " (异常: " + e.getMessage() + ")");
                    System.err.println("翻译接口 " + interfaceData.getTransName() + " 失败: " + e.getMessage());
                }
                
                // 添加小延迟，避免API调用过于频繁
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            result.setSuccess(true);
            result.setMessage("接口数据翻译完成");
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("接口数据翻译失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 批量翻译枚举数据
     */
    public TranslationResult translateEnumData(Consumer<TranslationProgress> progressCallback) {
        TranslationResult result = new TranslationResult();
        
        try {
            // 查询所有枚举数据
            EnumDictPO enumDictPO = new EnumDictPO();
            enumDictPO.setGroupName(globalPropes.getGroupName());
            enumDictPO.setProjectName(globalPropes.getProjectName());
            List<EnumDictPO> enumDataList = enumDictDao.queryForList(enumDictPO);
            
            if (enumDataList.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("未找到任何枚举数据");
                return result;
            }
            
            result.setTotalCount(enumDataList.size());
            
            // 批量翻译
            for (int i = 0; i < enumDataList.size(); i++) {
                EnumDictPO enumData = enumDataList.get(i);
                
                try {
                    // 更新进度
                    if (progressCallback != null) {
                        TranslationProgress progress = new TranslationProgress();
                        progress.setCurrentIndex(i + 1);
                        progress.setTotalCount(enumDataList.size());
                        progress.setCurrentItem(enumData.getEnumVal());
                        progress.setTableName("enum_dict");
                        progressCallback.accept(progress);
                    }
                    
                    // 直接进行翻译
                    CrResult<EnumDictPO> translationResult = translateEnumDictEntity(enumData);
                    
                    if (translationResult.isSucess()) {
                        // 更新翻译结果
                        updateEnumDataWithTranslation(enumData, translationResult.getData());
                        result.incrementSuccessCount();
                    } else {
                        result.incrementFailCount();
                        result.addFailedItem(enumData.getEnumVal() + " (翻译失败)");
                    }
                    
                } catch (Exception e) {
                    result.incrementFailCount();
                    result.addFailedItem(enumData.getEnumVal() + " (异常: " + e.getMessage() + ")");
                    System.err.println("翻译枚举 " + enumData.getEnumVal() + " 失败: " + e.getMessage());
                }
                
                // 添加小延迟，避免API调用过于频繁
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            result.setSuccess(true);
            result.setMessage("枚举数据翻译完成");
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("枚举数据翻译失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 批量翻译BizDict数据
     */
    public TranslationResult translateBizDictData(Consumer<TranslationProgress> progressCallback) {
        TranslationResult result = new TranslationResult();
        try {
            // 查询所有BizDict数据
            BizDictPO bizDictPO = new BizDictPO();
            bizDictPO.setGroupName(globalPropes.getGroupName());
            List<BizDictPO> bizDictList = bizDictDao.queryForList(bizDictPO);

            if (bizDictList.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("未找到任何BizDict数据");
                return result;
            }

            result.setTotalCount(bizDictList.size());

            // 批量翻译
            for (int i = 0; i < bizDictList.size(); i++) {
                BizDictPO bizDict = bizDictList.get(i);
                try {
                    // 更新进度
                    if (progressCallback != null) {
                        TranslationProgress progress = new TranslationProgress();
                        progress.setCurrentIndex(i + 1);
                        progress.setTotalCount(bizDictList.size());
                        progress.setCurrentItem(bizDict.getNameCamel());
                        progress.setTableName("biz_dict");
                        progressCallback.accept(progress);
                    }
                    // 直接进行翻译
                    CrResult<BizDictPO> translationResult = translateBizDictEntity(bizDict);
                    if (translationResult.isSucess()) {
                        // 更新翻译结果
                        updateBizDictDataWithTranslation(bizDict, translationResult.getData());
                        result.incrementSuccessCount();
                    } else {
                        result.incrementFailCount();
                        result.addFailedItem(bizDict.getNameCamel() + " (翻译失败)");
                    }
                } catch (Exception e) {
                    result.incrementFailCount();
                    result.addFailedItem(bizDict.getNameCamel() + " (异常: " + e.getMessage() + ")");
                    System.err.println("翻译BizDict " + bizDict.getNameCamel() + " 失败: " + e.getMessage());
                }
                // 添加小延迟，避免API调用过于频繁
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            result.setSuccess(true);
            result.setMessage("BizDict数据翻译完成");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("BizDict数据翻译失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 批量翻译所有支持的表数据
     */
    public TranslationResult translateAllData(Consumer<TranslationProgress> progressCallback) {
        TranslationResult totalResult = new TranslationResult();
        
        // 翻译接口数据
        TranslationResult interfaceResult = translateInterfaceData(progressCallback);
        totalResult.addResult(interfaceResult);
        
        // 翻译枚举数据
        TranslationResult enumResult = translateEnumData(progressCallback);
        totalResult.addResult(enumResult);

        // 翻译BizDict数据
        TranslationResult bizDictResult = translateBizDictData(progressCallback);
        totalResult.addResult(bizDictResult);
        
        totalResult.setSuccess(true);
        totalResult.setMessage("所有数据翻译完成");
        
        return totalResult;
    }

    /**
     * 翻译InterFaceEntity
     */
    public CrResult<InterFaceEntity> translateInterFaceEntity(InterFaceEntity entity) {
        SuccessFailureEnum flagEnum = SuccessFailureEnum.FAILURE;
        String transCommentZh = entity.getTransCommentZh();
        String transCommentEn = entity.getTransCommentEn();
        
        // 优先保证 transCommentZh 为中文，transCommentEn 为英文
        if (BusinessUtils.isAllChinese(transCommentZh)) {
            // transCommentZh 已为中文
            if (BusinessUtils.isAllEnglish(transCommentEn)) {
                // transCommentEn 已为英文，无需处理
            } else {
                // transCommentEn 为空或为中文，需翻译
                String desc = translateZhToEn(transCommentZh);
                if (StringUtils.isNotBlank(desc)) {
                    entity.setTransCommentEn(desc);
                    flagEnum = SuccessFailureEnum.SUCCESS;
                }
                flagEnum = SuccessFailureEnum.SUCCESS;
            }
        } else if (BusinessUtils.isAllEnglish(transCommentZh)) {
            // transCommentZh 实际为英文，尝试互换
            if (BusinessUtils.isAllChinese(transCommentEn)) {
                // transCommentEn 已为中文，互换
                entity.setTransCommentZh(transCommentEn);
                entity.setTransCommentEn(transCommentZh);
            } else {
                // transCommentEn 为空或为英文，需翻译
                entity.setTransCommentEn(transCommentZh);
                String desc = translateEnToZh(transCommentZh);
                if (StringUtils.isNotBlank(desc)) {
                    entity.setTransCommentZh(desc);
                    flagEnum = SuccessFailureEnum.SUCCESS;
                }
            }
        } else {
            // transCommentZh 为空或为混合/无效
            if (BusinessUtils.isAllChinese(transCommentEn)) {
                // transCommentEn 实际为中文，互换
                entity.setTransCommentZh(transCommentEn);
                entity.setTransCommentEn(transCommentZh);
            } else if (BusinessUtils.isAllEnglish(transCommentEn)) {
                // transCommentEn 实际为英文，翻译为中文
                entity.setTransCommentEn(transCommentEn);
                String desc = translateEnToZh(transCommentEn);
                if (StringUtils.isNotBlank(desc)) {
                    entity.setTransCommentZh(desc);
                    flagEnum = SuccessFailureEnum.SUCCESS;
                }
            } else {
                // 都为空或都不是有效的中英文，默认都用 transCommentZh，互译
                if (transCommentZh != null && !transCommentZh.isEmpty()) {
                    entity.setTransCommentZh(transCommentZh);
                    // transCommentEn 为空或为中文，需翻译
                    String desc = translateZhToEn(transCommentZh);
                    if (StringUtils.isNotBlank(desc)) {
                        entity.setTransCommentEn(desc);
                        flagEnum = SuccessFailureEnum.SUCCESS;
                    }
                } else if (transCommentEn != null && !transCommentEn.isEmpty()) {
                    entity.setTransCommentEn(transCommentEn);
                    String desc = translateEnToZh(transCommentEn);
                    if (StringUtils.isNotBlank(desc)) {
                        entity.setTransCommentZh(desc);
                        flagEnum = SuccessFailureEnum.SUCCESS;
                    }
                }
            }
        }

        CrResult<InterFaceEntity> crResult = new CrResult<>(flagEnum);
        crResult.setData(entity);
        return crResult;
    }

    /**
     * 翻译EnumDictPO
     */
    public CrResult<EnumDictPO> translateEnumDictEntity(EnumDictPO entity) {
        SuccessFailureEnum flagEnum = SuccessFailureEnum.FAILURE;
        String transCommentZh = entity.getDescCn();
        String transCommentEn = entity.getDescEn();
        
        // 优先保证 transCommentZh 为中文，transCommentEn 为英文
        if (BusinessUtils.isAllChinese(transCommentZh)) {
            // transCommentZh 已为中文
            if (BusinessUtils.isAllEnglish(transCommentEn)) {
                // transCommentEn 已为英文，无需处理
            } else {
                // transCommentEn 为空或为中文，需翻译
                String desc = translateZhToEn(transCommentZh);
                if (StringUtils.isNotBlank(desc)) {
                    entity.setDescEn(desc);
                    flagEnum = SuccessFailureEnum.SUCCESS;
                }
            }
        } else if (BusinessUtils.isAllEnglish(transCommentZh)) {
            // transCommentZh 实际为英文，尝试互换
            if (BusinessUtils.isAllChinese(transCommentEn)) {
                // transCommentEn 已为中文，互换
                entity.setDescCn(transCommentEn);
                entity.setDescEn(transCommentZh);
            } else {
                // transCommentEn 为空或为英文，需翻译
                entity.setDescEn(transCommentZh);

                String desc = translateEnToZh(transCommentZh);
                if (StringUtils.isNotBlank(desc)) {
                    entity.setDescCn(desc);
                    flagEnum = SuccessFailureEnum.SUCCESS;
                }
            }
        } else {
            // transCommentZh 为空或为混合/无效
            if (BusinessUtils.isAllChinese(transCommentEn)) {
                // transCommentEn 实际为中文，互换
                entity.setDescCn(transCommentEn);
                entity.setDescEn(transCommentZh);
            } else if (BusinessUtils.isAllEnglish(transCommentEn)) {
                // transCommentEn 实际为英文，翻译为中文
                entity.setDescEn(transCommentEn);
                String desc =translateEnToZh(transCommentEn);
                if (StringUtils.isNotBlank(desc)) {
                    entity.setDescCn(desc);
                    flagEnum = SuccessFailureEnum.SUCCESS;
                }
            } else {
                // 都为空或都不是有效的中英文，默认都用 transCommentZh，互译
                if (transCommentZh != null && !transCommentZh.isEmpty()) {
                    entity.setDescCn(transCommentZh);
                    // transCommentEn 为空或为中文，需翻译
                    String desc = translateZhToEn(transCommentZh);
                    if (StringUtils.isNotBlank(desc)) {
                        entity.setDescEn(desc);
                        flagEnum = SuccessFailureEnum.SUCCESS;
                    }
                } else if (transCommentEn != null && !transCommentEn.isEmpty()) {
                    entity.setDescEn(transCommentEn);
                    String desc =translateEnToZh(transCommentEn);
                    if (StringUtils.isNotBlank(desc)) {
                        entity.setDescCn(desc);
                        flagEnum = SuccessFailureEnum.SUCCESS;
                    }
                }
            }
        }

        CrResult<EnumDictPO> crResult = new CrResult<>(flagEnum);
        crResult.setData(entity);
        return crResult;
    }

    /**
     * 翻译BizDictPO
     */
    public CrResult<BizDictPO> translateBizDictEntity(BizDictPO entity) {
        SuccessFailureEnum flagEnum = SuccessFailureEnum.FAILURE;
        String commentCn = entity.getCommentCn();
        String commentEn = entity.getCommentEn();
        // 优先保证 commentCn 为中文，commentEn 为英文
        if (BusinessUtils.isAllChinese(commentCn)) {
            if (BusinessUtils.isAllEnglish(commentEn)) {
                // commentEn 已为英文，无需处理
            } else {
                // commentEn 为空或为中文，需翻译
                String desc = translateZhToEn(commentCn);
                if (StringUtils.isNotBlank(desc)) {
                    entity.setCommentEn(desc);
                    flagEnum = SuccessFailureEnum.SUCCESS;
                }
            }
        } else if (BusinessUtils.isAllEnglish(commentCn)) {
            if (BusinessUtils.isAllChinese(commentEn)) {
                // commentEn 已为中文，互换
                entity.setCommentCn(commentEn);
                entity.setCommentEn(commentCn);
            } else {
                // commentEn 为空或为英文，需翻译
                entity.setCommentEn(commentCn);
                String desc = translateEnToZh(commentCn);
                if (StringUtils.isNotBlank(desc)) {
                    entity.setCommentCn(desc);
                    flagEnum = SuccessFailureEnum.SUCCESS;
                }
            }
        } else {
            if (BusinessUtils.isAllChinese(commentEn)) {
                entity.setCommentCn(commentEn);
                entity.setCommentEn(commentCn);
            } else if (BusinessUtils.isAllEnglish(commentEn)) {
                entity.setCommentEn(commentEn);
                String desc = translateEnToZh(commentEn);
                if (StringUtils.isNotBlank(desc)) {
                    entity.setCommentCn(desc);
                    flagEnum = SuccessFailureEnum.SUCCESS;
                }
            } else {
                if (commentCn != null && !commentCn.isEmpty()) {
                    entity.setCommentCn(commentCn);
                    String desc = translateZhToEn(commentCn);
                    if (StringUtils.isNotBlank(desc)) {
                        entity.setCommentEn(desc);
                        flagEnum = SuccessFailureEnum.SUCCESS;
                    }
                } else if (commentEn != null && !commentEn.isEmpty()) {
                    entity.setCommentEn(commentEn);
                    String desc = translateEnToZh(commentEn);
                    if (StringUtils.isNotBlank(desc)) {
                        entity.setCommentCn(desc);
                        flagEnum = SuccessFailureEnum.SUCCESS;
                    }
                }
            }
        }
        CrResult<BizDictPO> crResult = new CrResult<>(flagEnum);
        crResult.setData(entity);
        return crResult;
    }

    /**
     * 通用翻译方法
     */
    private String translate(String text, String from, String to) {
        try {
            return translateWithBaidu(text, from, to);
        } catch (Exception e) {
            System.err.println("百度翻译API调用失败: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * 使用百度翻译API翻译
     */
    private String translateWithBaidu(String text, String from, String to) throws Exception {
        // 1. 获取access_token
        String clientId = "IXt6egbyWaompIU1qeQvHhpn";
        String clientSecret = "hGlq1j72LGdcdempTzdbdV7gHSucMxEQ";
        String tokenUrl = String.format(
            "https://aip.baidubce.com/oauth/2.0/token?client_id=%s&client_secret=%s&grant_type=client_credentials",
            clientId, clientSecret
        );
        String tokenResp = sendHttpRequest(tokenUrl, false); // GET请求
        // 解析access_token
        String accessToken = parseAccessToken(tokenResp);

        // 2. 调用翻译接口
        String transUrl = "https://aip.baidubce.com/rpc/2.0/mt/texttrans/v1?access_token=" + accessToken;
        String postData = String.format(
            "{\"q\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}",
            text.replace("\"", "\\\""), from, to
        );
        String json = sendHttpRequest(transUrl, true, postData); // POST请求

        // 解析JSON，提取翻译结果
        JSONObject obj = JSON.parseObject(json);
        if (obj.containsKey("result")) {
            JSONObject result = obj.getJSONObject("result");
            if (result.containsKey("trans_result")) {
                JSONArray arr = result.getJSONArray("trans_result");
                if (arr.size() > 0) {
                    JSONObject first = arr.getJSONObject(0);
                    return first.getString("dst");
                }
            }
        }
        return "";
    }

    /**
     * 解析access_token
     */
    private String parseAccessToken(String json) {
        try {
            JSONObject obj = JSON.parseObject(json);
            return obj.getString("access_token");
        } catch (Exception e) {
            throw new RuntimeException("access_token解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查是否是错误响应
     */
    private boolean isErrorResponse(String response, String apiType) {
        if ("mymemory".equals(apiType)) {
            return response.contains("\"responseStatus\":429") || 
                   response.contains("Too Many Requests") ||
                   response.contains("MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS");
        } else if ("libre".equals(apiType)) {
            return response.contains("\"error\"") || response.contains("429");
        }
        return false;
    }
    
    /**
     * 判断是否应该重试
     */
    private boolean shouldRetry(String response, String apiType, int retry) {
        if ("mymemory".equals(apiType)) {
            if (response.contains("MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS")) {
                String waitTime = extractWaitTime(response);
                System.err.println("MyMemory免费翻译额度已用完，需要等待: " + waitTime);
                System.err.println("请访问 https://mymemory.translated.net/doc/usagelimits.php 了解更多信息");
                return false; // 不重试
            } else {
                System.out.println("遇到429错误，等待重试... (重试 " + (retry + 1) + "/" + MAX_RETRIES + ")");
                return retry < MAX_RETRIES - 1;
            }
        } else if ("libre".equals(apiType)) {
            System.out.println("LibreTranslate API错误，等待重试... (重试 " + (retry + 1) + "/" + MAX_RETRIES + ")");
            return retry < MAX_RETRIES - 1;
        }
        return false;
    }
    
    /**
     * 解析翻译响应
     */
    private String parseTranslationResponse(String response, String apiType) {
        if ("mymemory".equals(apiType)) {
            return parseMyMemoryResponse(response);
        } else if ("libre".equals(apiType)) {
            return parseLibreResponse(response);
        }
        return null;
    }
    
    /**
     * 解析LibreTranslate响应
     */
    private String parseLibreResponse(String response) {
        try {
            if (response.contains("\"translatedText\"")) {
                int start = response.indexOf("\"translatedText\":\"") + 17;
                int end = response.indexOf("\"", start);
                if (start > 16 && end > start) {
                    String result = response.substring(start, end);
                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("解析LibreTranslate响应失败: " + e.getMessage());
        }
        return null;
    }

    
    /**
     * 解析MyMemory响应
     */
    private String parseMyMemoryResponse(String response) {
        try {
            if (response.contains("\"translatedText\"")) {
                int start = response.indexOf("\"translatedText\":\"") + 17;
                int end = response.indexOf("\"", start);
                if (start > 16 && end > start) {
                    String result = response.substring(start, end);
                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("解析MyMemory响应失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 从MyMemory响应中提取中文翻译
     */
    private String getChineseTranslationFromMyMemory(String text, String from, String to) throws Exception {
        try {
            String langPair = from + "|" + to;
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            String url = "https://api.mymemory.translated.net/get?q=" + encodedText + "&langpair=" + langPair;
            
            String response = sendHttpRequest(url, false);
            
            // 查找包含中文的翻译
            if (response.contains("\"translation\"")) {
                String[] parts = response.split("\"translation\":\"");
                for (int i = 1; i < parts.length; i++) {
                    String part = parts[i];
                    int end = part.indexOf("\"");
                    if (end > 0) {
                        String translation = part.substring(0, end);
                        // 检查是否包含中文字符或Unicode转义
                        if (translation.matches(".*[\\u4e00-\\u9fa5]+.*") || translation.contains("\\u")) {
                            // 处理Unicode转义
                            if (translation.contains("\\u")) {
                                translation = unescapeUnicode(translation);
                            }
                            return translation;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("从MyMemory提取中文翻译失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 解码Unicode转义序列
     */
    private String unescapeUnicode(String text) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '\\' && i + 1 < text.length() && text.charAt(i + 1) == 'u') {
                if (i + 5 < text.length()) {
                    String hex = text.substring(i + 2, i + 6);
                    try {
                        int unicode = Integer.parseInt(hex, 16);
                        result.append((char) unicode);
                        i += 6;
                    } catch (NumberFormatException e) {
                        result.append(text.charAt(i));
                        i++;
                    }
                } else {
                    result.append(text.charAt(i));
                    i++;
                }
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
    
    /**
     * 发送HTTP请求
     */
    private String sendHttpRequest(String urlString, boolean isPost) throws Exception {
        return sendHttpRequest(urlString, isPost, null);
    }
    
    /**
     * 发送HTTP请求
     */
    private String sendHttpRequest(String urlString, boolean isPost, String postData) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");

        if (isPost) {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            // 必须在 getOutputStream 之前设置 header
            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            connection.setRequestMethod("GET");
        }

        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }
    
    /**
     * 转换InterfaceDataPO为InterFaceEntity
     */
    private InterFaceEntity convertToInterFaceEntity(InterfaceDataPO interfaceData) {
        InterFaceEntity entity = new InterFaceEntity();
        entity.setTransCommentZh(interfaceData.getTransCommentZh());
        entity.setTransCommentEn(interfaceData.getTransCommentEn());
        entity.setTransName(interfaceData.getTransName());
        entity.setInterfaceName(interfaceData.getInterfaceName());
        entity.setClassName(interfaceData.getClassName());
        return entity;
    }
    
    /**
     * 更新InterfaceDataPO的翻译结果
     */
    private void updateInterfaceDataWithTranslation(InterfaceDataPO interfaceData, InterFaceEntity translatedEntity) {
        InterfaceDataPO updateData = new InterfaceDataPO();
        updateData.setTransCommentZh(translatedEntity.getTransCommentZh());
        updateData.setTransCommentEn(translatedEntity.getTransCommentEn());
        updateData.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        updateData.setUpdateBy("system");

        InterfaceDataPO whereData = new InterfaceDataPO();
        whereData.setAppName(interfaceData.getAppName());
        whereData.setInterfaceName(interfaceData.getInterfaceName());
        whereData.setClassName(interfaceData.getClassName());
        whereData.setTransName(interfaceData.getTransName());
        
        try {
            interfaceDataDao.updateByOne(updateData, whereData);
        } catch (Exception e) {
            System.err.println("更新接口数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新EnumDictPO的翻译结果
     */
    private void updateEnumDataWithTranslation(EnumDictPO enumData, EnumDictPO translatedEntity) {
        EnumDictPO updateData = new EnumDictPO();
        updateData.setDescCn(translatedEntity.getDescCn());
        updateData.setDescEn(translatedEntity.getDescEn());
        updateData.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        updateData.setUpdateBy("system");

        EnumDictPO whereData = new EnumDictPO();
        whereData.setEnumNme(enumData.getEnumNme());
        whereData.setEnumRef(enumData.getEnumRef());
        whereData.setEnumCd(enumData.getEnumCd());
        whereData.setAppName(enumData.getAppName());
        
        try {
            enumDictDao.updateByOne(updateData, whereData);
        } catch (Exception e) {
            System.err.println("更新枚举数据失败: " + e.getMessage());
        }
    }

    /**
     * 更新BizDictPO的翻译结果
     */
    private void updateBizDictDataWithTranslation(BizDictPO bizDict, BizDictPO translatedEntity) {
        BizDictPO updateData = new BizDictPO();
        updateData.setCommentCn(translatedEntity.getCommentCn());
        updateData.setCommentEn(translatedEntity.getCommentEn());
        // updateData.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // updateData.setUpdateBy("system"); // BizDictPO没有这两个字段

        BizDictPO whereData = new BizDictPO();
        whereData.setGroupName(bizDict.getGroupName());
        whereData.setProjectName(bizDict.getProjectName());
        whereData.setAppName(bizDict.getAppName());
        whereData.setNameCamel(bizDict.getNameCamel());
        try {
            bizDictDao.updateByOne(updateData, whereData);
        } catch (Exception e) {
            System.err.println("更新BizDict数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 翻译进度信息
     */
    public static class TranslationProgress {
        private int currentIndex;
        private int totalCount;
        private String currentItem;
        private String tableName;
        
        public int getCurrentIndex() { return currentIndex; }
        public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }
        
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        
        public String getCurrentItem() { return currentItem; }
        public void setCurrentItem(String currentItem) { this.currentItem = currentItem; }
        
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public double getProgress() {
            return totalCount > 0 ? (double) currentIndex / totalCount : 0.0;
        }
        
        public String getProgressText() {
            return String.format("%s: %d/%d (%.1f%%)", tableName, currentIndex, totalCount, getProgress() * 100);
        }
    }

    /**
     * 从MyMemory响应中提取等待时间
     */
    private String extractWaitTime(String response) {
        try {
            // 查找 "NEXT AVAILABLE IN" 后面的时间信息
            int start = response.indexOf("NEXT AVAILABLE IN");
            if (start > 0) {
                int end = response.indexOf("VISIT", start);
                if (end > start) {
                    return response.substring(start + 18, end).trim();
                }
            }
        } catch (Exception e) {
            System.err.println("提取等待时间失败: " + e.getMessage());
        }
        return "未知时间";
    }
}
