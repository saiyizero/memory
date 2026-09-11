package com.murong.ecp.tools.fx.domain.service.interfaces;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.common.TranslationService;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.BizMsgInfoDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizMsgInfoPO;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MsgCodeService {
    @Autowired
    private BizMsgInfoDao bizMsgInfoDao;
    @Autowired
    private GlobalProperties globalPropes;
    @Autowired
    private TranslationService translationService;
    @Autowired
    private Configuration cfg;


    public void generateCode(String msgClass){
        BizMsgInfoPO bizMsgInfoPO = new BizMsgInfoPO();
        bizMsgInfoPO.setMsgClass(msgClass);
        bizMsgInfoPO.setProjectName(globalPropes.getProjectName());
        bizMsgInfoPO.setGroupName(globalPropes.getGroupName());
        List<BizMsgInfoPO> bizMsgInfoLst = bizMsgInfoDao.queryForList(bizMsgInfoPO);

        if (bizMsgInfoLst.isEmpty()) {
            throw new RuntimeException("未找到消息引用为 " + msgClass + " 的数据");
        }

        String moduleName = null;
        String msgRefPath = null;
        String msgRef=null;
        List<GlobalProperties.CrMsgCode> msgCodes = globalPropes.getMsgCodes();
        for (GlobalProperties.CrMsgCode msgCode : msgCodes) {
            if(msgCode.getUrl().endsWith(msgClass)){
                moduleName=msgCode.getBasePath();
                msgRef=msgCode.getUrl();
                msgRefPath=msgRef.replaceAll("\\.", "/");
            }
        }
        if(StringUtils.isBlank(msgRef)){
            throw new RuntimeException("枚举 " + msgClass + "路径msgRef不允许为空");
        }
        if(StringUtils.isBlank(moduleName)){
            throw new RuntimeException("枚举 " + msgClass + "路径moduleName不允许为空");
        }

        String msgCdPath = globalPropes.getBasePath() + File.separator + moduleName + File.separator + msgRefPath + ".java";
        
        // 生成Java代码文件
        generateJavaFile(msgCdPath, msgRef, bizMsgInfoLst);
    }
    
    /**
     * 生成Java代码文件
     * @param filePath 文件路径
     * @param msgRef 消息引用
     * @param bizMsgInfoLst 消息数据列表
     */
    private void generateJavaFile(String filePath, String msgRef, List<BizMsgInfoPO> bizMsgInfoLst) {
        try {
            // 确保目录存在
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // 准备模板数据
            Map<String, Object> dataModel = prepareTemplateData(msgRef, bizMsgInfoLst);
            
            // 使用FreeMarker模板生成代码
            generateFile(cfg, "msg-code.ftl", dataModel, filePath);
            
            System.out.println("代码文件生成成功: " + filePath);
            
        } catch (Exception e) {
            throw new RuntimeException("生成代码文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 准备模板数据
     * @param msgRef 消息引用
     * @param bizMsgInfoLst 消息数据列表
     * @return 模板数据模型
     */
    private Map<String, Object> prepareTemplateData(String msgRef, List<BizMsgInfoPO> bizMsgInfoLst) {
        Map<String, Object> dataModel = new HashMap<>();
        
        // 包名
        String packageName = msgRef.substring(0, msgRef.lastIndexOf('.'));
        dataModel.put("packageName", packageName);
        
        // 类名
        String className = msgRef.substring(msgRef.lastIndexOf('.') + 1);
        dataModel.put("className", className);
        
        // 准备消息信息列表
        List<Map<String, Object>> msgInfoList = new ArrayList<>();
        for (BizMsgInfoPO msgInfo : bizMsgInfoLst) {
            Map<String, Object> msgInfoMap = new HashMap<>();
            
            // 消息键（转换为Java常量格式）
            msgInfoMap.put("msgKeyJava", convertToJavaConstant(msgInfo.getMsgKey()));
            
            // 消息代码
            msgInfoMap.put("msgCd", msgInfo.getMsgCd());
            
            // 中文描述（用于注释）
            String msgDescCn = msgInfo.getMsgDescCn();
            if (msgDescCn == null || msgDescCn.trim().isEmpty()) {
                msgDescCn = "无描述";
            }
            msgInfoMap.put("msgDescCn", msgDescCn);
            
            // 消息描述（优先使用英文描述，如果没有则使用中文描述）
            String msgDesc = msgInfo.getMsgDescEn();
            if (msgDesc == null || msgDesc.trim().isEmpty()) {
                msgDesc = msgInfo.getMsgDescCn();
            }
            if (msgDesc == null || msgDesc.trim().isEmpty()) {
                msgDesc = "No description";
            }
            
            // 转义字符串中的特殊字符
            msgInfoMap.put("msgDescEscaped", escapeJavaString(msgDesc));
            
            msgInfoList.add(msgInfoMap);
        }
        dataModel.put("bizMsgInfoList", msgInfoList);
        
        return dataModel;
    }
    
    /**
     * 使用FreeMarker模板生成文件
     * @param cfg FreeMarker配置
     * @param templateName 模板名称
     * @param dataModel 数据模型
     * @param outputPath 输出文件路径
     * @throws IOException IO异常
     * @throws TemplateException 模板异常
     */
    private void generateFile(Configuration cfg, String templateName, Map<String, Object> dataModel, String outputPath) throws IOException, TemplateException {
        Template template = cfg.getTemplate(templateName);
        File outFile = new File(outputPath);
        if (!outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)) {
            template.process(dataModel, writer);
        }
    }
    
    /**
     * 将消息键转换为Java常量格式
     * @param msgKey 原始消息键
     * @return Java常量格式
     */
    private String convertToJavaConstant(String msgKey) {
        if (msgKey == null || msgKey.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        // 将消息键转换为大写，并替换特殊字符
        String constant = msgKey.toUpperCase()
                .replaceAll("[^A-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        
        // 如果结果为空，返回默认值
        if (constant.isEmpty()) {
            return "UNKNOWN";
        }
        
        return constant;
    }
    
    /**
     * 转义Java字符串中的特殊字符
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private String escapeJavaString(String str) {
        if (str == null) {
            return "";
        }
        
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * 新增消息
     * @param msgInfo 消息信息
     * @return 新增结果
     */
    public boolean addMessage(BizMsgInfoPO msgInfo) {
        msgInfo.setGroupName(globalPropes.getGroupName());
        msgInfo.setProjectName(globalPropes.getProjectName());
        msgInfo.setAppName(globalPropes.getAppName());
        try {
            // 检查消息键是否已存在
            BizMsgInfoPO queryPO = new BizMsgInfoPO();
            queryPO.setGroupName(msgInfo.getGroupName());
            queryPO.setProjectName(msgInfo.getProjectName());
            queryPO.setAppName(msgInfo.getAppName());
            queryPO.setModuleName(msgInfo.getModuleName());
            queryPO.setMsgClass(msgInfo.getMsgClass());
            queryPO.setMsgKey(msgInfo.getMsgKey());
            
            List<BizMsgInfoPO> existingMsgs = bizMsgInfoDao.queryForList(queryPO);
            if (!existingMsgs.isEmpty()) {
                throw new RuntimeException("消息键已存在: " + msgInfo.getMsgClass() + "." + msgInfo.getMsgKey());
            }
            
            // 检查消息代码是否已存在
            queryPO = new BizMsgInfoPO();
            queryPO.setGroupName(msgInfo.getGroupName());
            queryPO.setProjectName(msgInfo.getProjectName());
            queryPO.setAppName(msgInfo.getAppName());
            queryPO.setModuleName(msgInfo.getModuleName());
            queryPO.setMsgCd(msgInfo.getMsgCd());
            
            existingMsgs = bizMsgInfoDao.queryForList(queryPO);
            if (!existingMsgs.isEmpty()) {
                throw new RuntimeException("消息代码已存在: " + msgInfo.getMsgCd());
            }
            
            // 执行新增
            bizMsgInfoDao.insert(msgInfo);
            return true;
            
        } catch (Exception e) {
            throw new RuntimeException("新增消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 修改消息
     * @param msgInfo 消息信息
     * @return 修改结果
     */
    public boolean updateMessage(BizMsgInfoPO msgInfo) {
        try {
            // 参数验证
            if (msgInfo == null) {
                throw new RuntimeException("消息信息不能为空");
            }
            if (msgInfo.getMsgClass() == null || msgInfo.getMsgClass().trim().isEmpty()) {
                throw new RuntimeException("消息分类不能为空");
            }
            if (msgInfo.getMsgKey() == null || msgInfo.getMsgKey().trim().isEmpty()) {
                throw new RuntimeException("消息键不能为空");
            }
            if (msgInfo.getMsgCd() == null || msgInfo.getMsgCd().trim().isEmpty()) {
                throw new RuntimeException("消息代码不能为空");
            }
            
            // 检查消息是否存在
            BizMsgInfoPO queryPO = new BizMsgInfoPO();
            queryPO.setGroupName(msgInfo.getGroupName());
            queryPO.setProjectName(msgInfo.getProjectName());
            queryPO.setAppName(msgInfo.getAppName());
            queryPO.setModuleName(msgInfo.getModuleName());
            queryPO.setMsgClass(msgInfo.getMsgClass());
            queryPO.setMsgKey(msgInfo.getMsgKey());
            queryPO.setMsgCd(msgInfo.getMsgCd());
            
            BizMsgInfoPO existingMsg = bizMsgInfoDao.queryOne(queryPO);
            if (existingMsg == null) {
                throw new RuntimeException("要修改的消息不存在");
            }
            
            // 执行修改
            bizMsgInfoDao.updateByOne(msgInfo, queryPO);
            
            return true;
            
        } catch (Exception e) {
            throw new RuntimeException("修改消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除消息
     * @param msgInfo 消息信息
     * @return 删除结果
     */
    public boolean deleteMessage(BizMsgInfoPO msgInfo) {
        try {
            // 参数验证
            if (msgInfo == null) {
                throw new RuntimeException("消息信息不能为空");
            }
            if (msgInfo.getMsgClass() == null || msgInfo.getMsgClass().trim().isEmpty()) {
                throw new RuntimeException("消息分类不能为空");
            }
            if (msgInfo.getMsgKey() == null || msgInfo.getMsgKey().trim().isEmpty()) {
                throw new RuntimeException("消息键不能为空");
            }
            if (msgInfo.getMsgCd() == null || msgInfo.getMsgCd().trim().isEmpty()) {
                throw new RuntimeException("消息代码不能为空");
            }
            
            // 检查消息是否存在
            BizMsgInfoPO queryPO = new BizMsgInfoPO();
            queryPO.setGroupName(msgInfo.getGroupName());
            queryPO.setProjectName(msgInfo.getProjectName());
            queryPO.setAppName(msgInfo.getAppName());
            queryPO.setModuleName(msgInfo.getModuleName());
            queryPO.setMsgClass(msgInfo.getMsgClass());
            queryPO.setMsgKey(msgInfo.getMsgKey());
            queryPO.setMsgCd(msgInfo.getMsgCd());
            
            BizMsgInfoPO existingMsg = bizMsgInfoDao.queryOne(queryPO);
            if (existingMsg == null) {
                throw new RuntimeException("要删除的消息不存在");
            }
            
            // 执行删除
            bizMsgInfoDao.delete(queryPO);
            
            return true;
            
        } catch (Exception e) {
            throw new RuntimeException("删除消息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取最新消息码
     * 该条sql 获取已存在的序列号seq_No
     */
    public String getLatestMsgCode() {
        try {
            // 构建消息前缀
            String appName = globalPropes.getAppName();
            if (appName == null || appName.trim().isEmpty()) {
                throw new RuntimeException("应用名称不能为空");
            }
            String msgPrefix = appName.toUpperCase() + "P";

            // 使用高效的查询方法获取已存在的序列号
            List<String> existingSeqNos = bizMsgInfoDao.queryExistingSeqNos(
                    globalPropes.getGroupName(),
                    globalPropes.getProjectName(),
                    msgPrefix
            );

            // 找到下一个可用的序列号
            int nextSeqNo = 1;
            for (String seqNoStr : existingSeqNos) {
                try {
                    int seqNo = Integer.parseInt(seqNoStr);
                    if (seqNo >= nextSeqNo) {
                        nextSeqNo = seqNo + 1;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }

            // 检查是否超出范围
            if (nextSeqNo > 9999) {
                throw new RuntimeException("消息码序列号已满，无法生成新的消息码");
            }

            // 格式化序列号（4位数字，前面补0）
            String formattedSeqNo = String.format("%04d", nextSeqNo);

            // 返回完整的消息码
            return msgPrefix + formattedSeqNo;

        } catch (Exception e) {
            throw new RuntimeException("获取最新消息码失败: " + e.getMessage(), e);
        }
    }

    public String toMsgKey(String descCn){
        if (descCn == null || descCn.trim().isEmpty()) {
            return "";
        }

        // 1. 清理输入文本：移除标点符号，转换为大写
        String cleaned = descCn.replaceAll("[^a-zA-Z\\s]", "").toUpperCase().trim();

        // 2. 分割单词
        String[] words = cleaned.split("\\s+");

        // 3. 过滤停用词（常见的不重要词汇）
        String[] stopWords = {"THE", "A", "AN", "AND", "OR", "BUT", "IN", "ON", "AT", "TO", "FOR", "OF", "WITH", "BY", "IS", "ARE", "WAS", "WERE", "BE", "BEEN", "HAVE", "HAS", "HAD", "DO", "DOES", "DID", "WILL", "WOULD", "COULD", "SHOULD", "CAN", "MAY", "MIGHT", "MUST"};
        List<String> filteredWords = new ArrayList<>();

        for (String word : words) {
            if (word.length() > 0 && !Arrays.asList(stopWords).contains(word)) {
                filteredWords.add(word);
            }
        }

        // 4. 生成消息键
        StringBuilder msgKey = new StringBuilder();

        if (filteredWords.isEmpty()) {
            // 如果没有有效单词，使用前几个字符
            String fallback = cleaned.replaceAll("\\s+", "").substring(0, Math.min(20, cleaned.length()));
            return fallback;
        }

        // 5. 构建消息键，使用单词缩写
        for (int i = 0; i < filteredWords.size(); i++) {
            String word = filteredWords.get(i);

            if (i == 0) {
                // 第一个单词：如果长度<=4，全部使用；否则取前4个字符
                if (word.length() <= 4) {
                    msgKey.append(word);
                } else {
                    msgKey.append(word.substring(0, 4));
                }
            } else {
                // 后续单词：取前3个字符
                if (word.length() <= 3) {
                    msgKey.append("_").append(word);
                } else {
                    msgKey.append("_").append(word.substring(0, 3));
                }
            }

            // 检查长度限制
            if (msgKey.length() >= 20) {
                break;
            }
        }

        // 6. 确保长度不超过20
        String result = msgKey.toString();
        if (result.length() > 30) {
            result = result.substring(0, 20);
        }

        return result;
    }
}
