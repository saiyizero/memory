package com.murong.ecp.tools.fx.domain.service.interfaces;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.InterFaceEntity;
import com.murong.ecp.tools.fx.domain.entity.TableEntity;
import com.murong.ecp.tools.fx.enums.DataStatusEnum;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.infrastructure.cache.BizDictCache;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.CommonClassDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.EnumDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.BizDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import com.murong.ecp.tools.fx.infrastructure.utils.BusinessUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.JsonFormatUtil;
import com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaCodeService {

    @Autowired
    private GlobalProperties globalPropes;
    @Autowired
    private EnumDictDao enumDictDao;
    @Autowired
    private CommonClassDao commonClassDao;
    @Autowired
    private BizDictDao bizDictDao;
    @Autowired
    BizDictCache bizDictCache;

    /**
     * 编译本地目录的Java代码
     * @param sourcePath 源码根目录
     * @return 编译结果
     */
    private CrResult compileJavaCode(String sourcePath) {
        try {
            File sourceDir = new File(sourcePath);
            if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf("源码目录不存在: " + sourcePath);
                return result;
            }

            // 检查是否存在pom.xml文件
            File pomFile = new File(sourcePath, "pom.xml");
            if (!pomFile.exists()) {
                CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf("未找到pom.xml文件，无法编译: " + sourcePath);
                return result;
            }

            // 检查Maven是否可用
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("mvn", "--version");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf("Maven不可用，请确保已安装Maven并配置环境变量");
                return result;
            }

            // 执行Maven编译
            System.out.println("开始编译模块: " + sourcePath);
            processBuilder = new ProcessBuilder();
            processBuilder.directory(sourceDir);
            processBuilder.command("mvn", "compile", "-DskipTests", "-q");
            
            // 重定向错误输出到标准输出
            processBuilder.redirectErrorStream(true);
            
            Process compileProcess = processBuilder.start();
            
            // 读取编译输出
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(compileProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int compileExitCode = compileProcess.waitFor();
            
            if (compileExitCode == 0) {
                System.out.println("编译成功: " + sourcePath);
                CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
                result.setMsgInf("编译成功");
                return result;
            } else {
                String errorMsg = "编译失败: " + sourcePath + "\n" + output.toString();
                System.err.println(errorMsg);
                CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
                result.setMsgInf(errorMsg);
                return result;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("编译过程中发生异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 检查是否需要编译
     * @param sourcePath 源码根目录
     * @return 是否需要编译
     */
    private boolean needsCompilation(String sourcePath) {
        try {
            String classRoot=BusinessUtils.getClassPath(sourcePath);

            File classDir = new File(classRoot);
            
            // 如果target/classes目录不存在，需要编译
            if (!classDir.exists()) {
                return true;
            }
            
            // 检查是否有Java源文件
            File srcDir = new File(sourcePath, "src/main/java");
            if (!srcDir.exists()) {
                return false; // 没有源码，不需要编译
            }
            
            // 检查Java源文件的最新修改时间
            long latestSourceTime = getLatestModificationTime(srcDir);
            long latestClassTime = getLatestModificationTime(classDir);
            
            // 如果源文件比class文件新，需要编译
            return latestSourceTime > latestClassTime;
            
        } catch (Exception e) {
            e.printStackTrace();
            return true; // 出错时保守起见，选择编译
        }
    }
    
    /**
     * 获取目录下所有文件的最新修改时间
     * @param dir 目录
     * @return 最新修改时间
     */
    private long getLatestModificationTime(File dir) {
        final long[] latestTime = {0};
        if (dir.exists() && dir.isDirectory()) {
            try {
                Files.walk(dir.toPath())
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            long fileTime = Files.getLastModifiedTime(path).toMillis();
                            if (fileTime > latestTime[0]) {
                                latestTime[0] = fileTime;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return latestTime[0];
    }

    /**
     * 登记接口父类
     */
    public CrResult registerParentClass(List<InterFaceEntity> entityList, String sourcePath) {
        try {
            URL[] urls;
            // 判断是JAR文件还是本地目录
            File sourceFile = new File(sourcePath);
            if (sourcePath.toLowerCase().endsWith(".jar") && sourceFile.exists()) {
                // JAR文件
                urls = new URL[]{sourceFile.toURI().toURL()};
            } else {
                // 本地目录 - 确保使用最新的class文件
                if (needsCompilation(sourcePath)) {
                    System.out.println("检测到源码更新，开始编译: " + sourcePath);
                    CrResult compileResult = compileJavaCode(sourcePath);
                    if (!compileResult.isSucess()) {
                        System.err.println("编译失败: " + compileResult.getMsgInf());
                        // 编译失败时抛出异常，停止处理
                        throw new RuntimeException("编译失败: " + compileResult.getMsgInf());
                    }
                }

                String classRoot=BusinessUtils.getClassPath(sourcePath);
                urls = new URL[]{new File(classRoot).toURI().toURL()};
            }
            try (URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader())) {
                for (InterFaceEntity entity : entityList) {
                    String reqParentClass = entity.getReqParentClass();
                    String rspParentClass = entity.getRspParentClass();

                    // 处理请求父类
                    if (StringUtils.isNotBlank(reqParentClass)) {
                        try {
                            Class<?> reqClass = classLoader.loadClass(reqParentClass);
                            CommonClassPO reqParentClassPO = new CommonClassPO();
                            reqParentClassPO.setClassName(reqClass.getSimpleName());
                            reqParentClassPO.setClassType("PCLS");
                            reqParentClassPO.setClassPath(reqClass.getName());
                            reqParentClassPO.setGroupName(globalPropes.getGroupName());
                            reqParentClassPO.setProjectName(globalPropes.getProjectName());
                            reqParentClassPO.setAppName(globalPropes.getAppName());
                            CommonClassPO exist = commonClassDao.queryOne(reqParentClassPO);
                            // 注释（中文/英文）
                            String commentCn = "";
                            String commentEn = "";
                            if (reqClass.isAnnotationPresent(Deprecated.class)) {
                                commentCn = "已废弃";
                                commentEn = "Deprecated";
                            }
                            reqParentClassPO.setClassCommentCn(commentCn);
                            reqParentClassPO.setClassCommentEn(commentEn);
                            // 字段信息
                            Field[] fields = reqClass.getDeclaredFields();
                            List<RxField> fieldList = new ArrayList<>();
                            for (Field f : fields) {
                                if (Modifier.isStatic(f.getModifiers())) continue;
                                RxField rxField = buildRxField(f, classLoader, 1, 3);
                                fieldList.add(rxField);
                                // 新增：基础类型登记BizDictPO
                                if (MrStringUtils.isJavaBasicType(f.getType().getSimpleName())) {
                                    BizDictPO po = new BizDictPO();
                                    po.setGroupName(globalPropes.getGroupName());
                                    po.setProjectName(globalPropes.getProjectName());
                                    po.setAppName(globalPropes.getAppName());
                                    po.setNameCamel(rxField.getNameCamel());
                                    po.setNameSnake(rxField.getNameSnake());
                                    po.setType(rxField.getType());
                                    po.setDbTyp(rxField.getDbTyp());
                                    po.setNotNull(null);
                                    po.setEnumNme(rxField.getEnumNme());
                                    po.setEnumRef(rxField.getEnumRef());
                                    po.setDefaultValue(rxField.getDefaultValue());
                                    po.setCommentCn(rxField.getCommentCn());
                                    po.setCommentEn(rxField.getCommentEn());
                                    po.setLength(rxField.getLength());
                                    if(!bizDictCache.existsInPublicBizDict(po)){
                                        BizDictPO rspPO = bizDictDao.save(po);
                                        rxField.setNameCamel(rspPO.getNameCamel());
                                        rxField.setNameSnake(rspPO.getNameSnake());
                                        rxField.setType(rspPO.getType());
                                        rxField.setDbTyp(rspPO.getDbTyp());
                                        rxField.setEnumNme(rspPO.getEnumNme());
                                        rxField.setEnumRef(rspPO.getEnumRef());
                                        rxField.setDefaultValue(rspPO.getDefaultValue());
                                        rxField.setCommentCn(rspPO.getCommentCn());
                                        rxField.setCommentEn(rspPO.getCommentEn());
                                        rxField.setLength(rspPO.getLength());
                                    }else {
                                        BizDictPO rspPO = bizDictCache.getPublicBizDict(po);
                                        rxField.setNameCamel(rspPO.getNameCamel());
                                        rxField.setNameSnake(rspPO.getNameSnake());
                                        rxField.setType(rspPO.getType());
                                        rxField.setDbTyp(rspPO.getDbTyp());
                                        rxField.setEnumNme(rspPO.getEnumNme());
                                        rxField.setEnumRef(rspPO.getEnumRef());
                                        rxField.setDefaultValue(rspPO.getDefaultValue());
                                        rxField.setCommentCn(rspPO.getCommentCn());
                                        rxField.setCommentEn(rspPO.getCommentEn());
                                        rxField.setLength(rspPO.getLength());
                                    }
                                }
                            }
                            reqParentClassPO.setFieldsJson(JsonFormatUtil.toJson(fieldList));
                            // 先查是否存在，存在则更新，不存在则插入

                            if (exist == null) {
                                reqParentClassPO.setCompletedFlg(DataStatusEnum.PENDING.getCode());
                                commonClassDao.save(reqParentClassPO);
                            } else if(!StringUtils.equals(exist.getCompletedFlg(), DataStatusEnum.REVIEW.getCode())) {
                                commonClassDao.updateByOne(reqParentClassPO, exist);
                            }
                        } catch (Throwable e) {
                            if(!StringUtils.equals(e.getMessage(),"com/yuangou/ecp/biz/transengine/sqlsession/YGPageEntity"))
                            e.printStackTrace();
                        }
                    }

                    // 处理响应父类
                    if (StringUtils.isNotBlank(rspParentClass)) {
                        try {
                            Class<?> rspClass = classLoader.loadClass(rspParentClass);
                            CommonClassPO rspParentClassPO = new CommonClassPO();
                            rspParentClassPO.setClassName(rspClass.getSimpleName());
                            rspParentClassPO.setClassType("PCLS");
                            rspParentClassPO.setClassPath(rspClass.getName());
                            CommonClassPO exist = commonClassDao.queryOne(rspParentClassPO);
                            // 注释（中文/英文）
                            String commentCn = "";
                            String commentEn = "";
                            if (rspClass.isAnnotationPresent(Deprecated.class)) {
                                commentCn = "已废弃";
                                commentEn = "Deprecated";
                            }
                            rspParentClassPO.setClassCommentCn(commentCn);
                            rspParentClassPO.setClassCommentEn(commentEn);
                            // 字段信息
                            Field[] fields = rspClass.getDeclaredFields();
                            List<RxField> fieldList = new ArrayList<>();
                            for (Field f : fields) {
                                if (Modifier.isStatic(f.getModifiers())) continue;
                                RxField rxField = buildRxField(f, classLoader, 1, 3);
                                fieldList.add(rxField);
                                // 新增：基础类型登记BizDictPO
                                if (MrStringUtils.isJavaBasicType(f.getType().getSimpleName())) {
                                    BizDictPO po = new BizDictPO();
                                    po.setGroupName(globalPropes.getGroupName());
                                    po.setProjectName(globalPropes.getProjectName());
                                    po.setAppName(globalPropes.getAppName());
                                    po.setNameCamel(rxField.getNameCamel());
                                    po.setNameSnake(rxField.getNameSnake());
                                    po.setType(rxField.getType());
                                    po.setDbTyp(rxField.getDbTyp());
                                    po.setEnumNme(rxField.getEnumNme());
                                    po.setEnumRef(rxField.getEnumRef());
                                    po.setNotNull(null);
                                    po.setDefaultValue(rxField.getDefaultValue());
                                    po.setCommentCn(rxField.getCommentCn());
                                    po.setCommentEn(rxField.getCommentEn());
                                    po.setLength(rxField.getLength());
                                    if(!bizDictCache.existsInPublicBizDict(po)){
                                        BizDictPO rspPO = bizDictDao.save(po);
                                        rxField.setNameCamel(rspPO.getNameCamel());
                                        rxField.setNameSnake(rspPO.getNameSnake());
                                        rxField.setType(rspPO.getType());
                                        rxField.setDbTyp(rspPO.getDbTyp());
                                        rxField.setEnumNme(rspPO.getEnumNme());
                                        rxField.setEnumRef(rspPO.getEnumRef());
                                        rxField.setDefaultValue(rspPO.getDefaultValue());
                                        rxField.setCommentCn(rspPO.getCommentCn());
                                        rxField.setCommentEn(rspPO.getCommentEn());
                                        rxField.setLength(rspPO.getLength());
                                    }else {
                                        BizDictPO rspPO = bizDictCache.getPublicBizDict(po);
                                        rxField.setNameCamel(rspPO.getNameCamel());
                                        rxField.setNameSnake(rspPO.getNameSnake());
                                        rxField.setType(rspPO.getType());
                                        rxField.setDbTyp(rspPO.getDbTyp());
                                        rxField.setEnumNme(rspPO.getEnumNme());
                                        rxField.setEnumRef(rspPO.getEnumRef());
                                        rxField.setDefaultValue(rspPO.getDefaultValue());
                                        rxField.setCommentCn(rspPO.getCommentCn());
                                        rxField.setCommentEn(rspPO.getCommentEn());
                                        rxField.setLength(rspPO.getLength());
                                    }
                                }
                            }
                            rspParentClassPO.setFieldsJson(JsonFormatUtil.toJson(fieldList));
                            // 先查是否存在，存在则更新，不存在则插入
                            if (exist == null) {
                                rspParentClassPO.setCompletedFlg(DataStatusEnum.PENDING.getCode());
                                commonClassDao.save(rspParentClassPO);
                            } else if(!StringUtils.equals(exist.getCompletedFlg(), DataStatusEnum.REVIEW.getCode())) {
                                commonClassDao.updateByOne(rspParentClassPO, exist);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
    }

    // 构建RxField，支持递归children
    private RxField buildRxField(Field f, ClassLoader classLoader, int depth, int maxDepth) {
        RxField rxField = new RxField();
        rxField.setNameCamel(f.getName());
        rxField.setNameSnake(MrStringUtils.toUnderline(f.getName()));
        rxField.setType(f.getType().getSimpleName());
        // 递归children
        if (depth < maxDepth && !f.getType().isPrimitive() && !f.getType().getName().startsWith("java.")) {
            try {
                Field[] subFields = f.getType().getDeclaredFields();
                List<RxField> children = new ArrayList<>();
                for (Field sub : subFields) {
                    if (Modifier.isStatic(sub.getModifiers())) continue;
                    children.add(buildRxField(sub, classLoader, depth + 1, maxDepth));
                }
                if (!children.isEmpty()) {
                    rxField.setChildren(children);
                }
            } catch (Throwable ignore) {}
        }
        return rxField;
    }

    /**
     * 使用反射方式递归解析class文件，获取接口方法、请求参数、响应参数，递归深度最多5层
     * @param sourcePath 源码根目录或JAR文件路径
     * @return 接口实体列表
     */
    public List<InterFaceEntity> javaEntityConvInterFace(String sourcePath) {
        List<InterFaceEntity> result = new ArrayList<>();
        try {
            java.util.List<Class<?>> interfaceClasses = new ArrayList<>();
            // 1. 加载所有class文件
            java.util.List<String> classNames = new ArrayList<>();
            URL[] urls;
            
            // 判断是JAR文件还是本地目录
            File sourceFile = new File(sourcePath);
            if (sourcePath.toLowerCase().endsWith(".jar") && sourceFile.exists()) {
                // JAR文件
                collectClassNames(sourcePath, "", classNames);
                urls = new URL[]{sourceFile.toURI().toURL()};
            } else {
                // 本地目录 - 先检查是否需要编译
                if (needsCompilation(sourcePath)) {
                    System.out.println("检测到源码更新，开始编译: " + sourcePath);
                    CrResult compileResult = compileJavaCode(sourcePath);
                    if (!compileResult.isSucess()) {
                        System.err.println("编译失败: " + compileResult.getMsgInf());
                        // 编译失败时返回null，停止处理
                        return null;
                    }
                } else {
                    System.out.println("源码未更新，跳过编译: " + sourcePath);
                }

                String classRoot=BusinessUtils.getClassPath(sourcePath);
                collectClassNames(new File(classRoot), "", classNames);
                urls = new URL[]{new File(classRoot).toURI().toURL()};
            }
            
            // 使用类加载器
            try (URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader())) {
                for (String className : classNames) {
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (clazz.isInterface()) {
                            interfaceClasses.add(clazz);
                        }
                    } catch (Throwable e) {
                        if(StringUtils.isNotBlank(e.getMessage()) && StringUtils.contains(e.getMessage(), "com/murong/ecp/m5/dict")){
                        }else  {
                            e.printStackTrace();
                        }
                    }
                }
                // 2. 处理每个接口
                for (Class<?> iface : interfaceClasses) {
                    InterFaceEntity entity = new InterFaceEntity();
                    entity.setInterfaceName(iface.getSimpleName());
                    entity.setClassName(iface.getName());
                    entity.setTransClass(iface.getSimpleName());
                    File sourceModNmeFile = new File(sourcePath);
                    entity.setModuleName(sourceModNmeFile.getName());
                    // 解析@RequestMapping注解
                    String interfaceUrl = null;
                    try {
                        Class<?> reqMappingClass = classLoader.loadClass("org.springframework.web.bind.annotation.RequestMapping");
                        Annotation ann = iface.getAnnotation((Class<Annotation>) reqMappingClass);
                        if (ann != null) {
                            Object value = ann.annotationType().getMethod("value").invoke(ann);
                            if (value instanceof String[] && ((String[]) value).length > 0) {
                                interfaceUrl = ((String[]) value)[0];
                            }
                        }
                    } catch (Throwable ignore) {
                        ignore.printStackTrace();
                    }
                    entity.setInterfaceUrl(interfaceUrl);
                    // 3. 处理方法
                    for (Method method : iface.getDeclaredMethods()) {
                        InterFaceEntity methodEntity = new InterFaceEntity();
                        methodEntity.setInterfaceName(entity.getInterfaceName());
                        methodEntity.setClassName(entity.getClassName());
                        methodEntity.setTransClass(entity.getTransClass());
                        methodEntity.setInterfaceUrl(entity.getProperties().getInterfaceUrl());
                        methodEntity.setSimpleName(method.getName());
                        methodEntity.setTransName(method.getName());
                        methodEntity.setModuleName(entity.getModuleName());
                        methodEntity.setLableName(BusinessUtils.getLabelByInterfaceName(entity.getInterfaceName()));
                        // 新增：解析ECPApiOperation注解
                        try {
                            Class<?> ecpApiOperationClass = classLoader.loadClass("com.murong.ecp.bp.common.integration.swagger.ECPApiOperation");
                            Annotation ann = method.getAnnotation((Class<Annotation>) ecpApiOperationClass);
                            if (ann != null) {
                                // value -> transCommentZh
                                try {
                                    Object value = ann.annotationType().getMethod("value").invoke(ann);
                                    if (value != null) {
                                        methodEntity.setTransCommentZh(value.toString());
                                    }
                                } catch (Exception ignore) {
                                    ignore.printStackTrace();
                                }
                                // notes -> transCommentEn
                                try {
                                    Object notes = ann.annotationType().getMethod("notes").invoke(ann);
                                    if (notes != null) {
                                        methodEntity.setTransCommentEn(notes.toString());
                                    }
                                } catch (Exception ignore) {
                                    ignore.printStackTrace();
                                }
                            }
                        } catch (Throwable ignore) {
                            ignore.printStackTrace();
                        }
                        // 解析方法上的@PostMapping/@RequestMapping注解
                        String methodUrl = null;
                        try {
                            Class<?> postMappingClass = classLoader.loadClass("org.springframework.web.bind.annotation.PostMapping");
                            Annotation ann = method.getAnnotation((Class<Annotation>) postMappingClass);
                            if (ann != null) {
                                Object value = ann.annotationType().getMethod("value").invoke(ann);
                                if (value instanceof String[] && ((String[]) value).length > 0) {
                                    methodUrl = ((String[]) value)[0];
                                }
                            }
                        } catch (Throwable ignore) {
                            ignore.printStackTrace();
                        }
                        if (methodUrl == null) {
                            try {
                                Class<?> reqMappingClass = classLoader.loadClass("org.springframework.web.bind.annotation.RequestMapping");
                                Annotation ann = method.getAnnotation((Class<Annotation>) reqMappingClass);
                                if (ann != null) {
                                    Object value = ann.annotationType().getMethod("value").invoke(ann);
                                    if (value instanceof String[] && ((String[]) value).length > 0) {
                                        methodUrl = ((String[]) value)[0];
                                    }
                                }
                            } catch (Throwable ignore) {
                                ignore.printStackTrace();
                            }
                        }
                        methodEntity.setMethodUrl(methodUrl);
                        // 4. 解析入参
                        List<RxField> requestRxFields = new ArrayList<>();
                        Parameter[] parameters = method.getParameters();
                        String requestPackage = null;
                        String requestClass = null;
                        String reqParentClass = null;
                        if (parameters.length > 0) {
                            Class<?> reqType = parameters[0].getType();
                            requestPackage = reqType.getPackage() != null ? reqType.getPackage().getName() : null;
                            requestClass = reqType.getSimpleName();
                            Class<?> reqSuper = reqType.getSuperclass();
                            if (reqSuper != null && !"java.lang.Object".equals(reqSuper.getName())) {
                                reqParentClass = reqSuper.getName();
                            }
                        }
                        for (Parameter param : parameters) {
                            // 顶层参数对象字段扁平化，不带参数名前缀
                            parseFieldsReflect(param.getParameterizedType(), null, 1, 5, requestRxFields, classLoader, new HashSet<>());
                        }
                        methodEntity.setRequest(requestRxFields);
                        // 新增：基础类型登记BizDictPO（入参）
                        for (RxField rxField : requestRxFields) {
                            if (MrStringUtils.isJavaBasicType(rxField.getType())) {
                                BizDictPO po = new BizDictPO();
                                po.setGroupName(globalPropes.getGroupName());
                                po.setProjectName(globalPropes.getProjectName());
                                po.setAppName(globalPropes.getAppName());
                                po.setNameCamel(rxField.getNameCamel());
                                po.setNameSnake(rxField.getNameSnake());
                                po.setType(rxField.getType());
                                po.setDbTyp(rxField.getDbTyp());
                                po.setEnumNme(rxField.getEnumNme());
                                po.setEnumRef(rxField.getEnumRef());
                                po.setNotNull(Boolean.toString(rxField.isNotNull()));
                                po.setDefaultValue(rxField.getDefaultValue());
                                po.setCommentCn(rxField.getCommentCn());
                                po.setCommentEn(rxField.getCommentEn());
                                if(rxField.getLength()!=null){
                                    po.setLength(rxField.getLength());
                                }
                                if(!bizDictCache.existsInPublicBizDict(po)){
                                    BizDictPO rspPO = bizDictDao.save(po);
                                    rxField.setNameCamel(rspPO.getNameCamel());
                                    rxField.setNameSnake(rspPO.getNameSnake());
                                    rxField.setType(rspPO.getType());
                                    rxField.setDbTyp(rspPO.getDbTyp());
                                    rxField.setEnumNme(rspPO.getEnumNme());
                                    rxField.setEnumRef(rspPO.getEnumRef());
                                    rxField.setDefaultValue(rspPO.getDefaultValue());
                                    rxField.setCommentCn(rspPO.getCommentCn());
                                    rxField.setCommentEn(rspPO.getCommentEn());
                                    rxField.setLength(rspPO.getLength());
                                }else {
                                    BizDictPO rspPO = bizDictCache.getPublicBizDict(po);
                                    rxField.setNameCamel(rspPO.getNameCamel());
                                    rxField.setNameSnake(rspPO.getNameSnake());
                                    rxField.setType(rspPO.getType());
                                    rxField.setDbTyp(rspPO.getDbTyp());
                                    rxField.setEnumNme(rspPO.getEnumNme());
                                    rxField.setEnumRef(rspPO.getEnumRef());
                                    rxField.setDefaultValue(rspPO.getDefaultValue());
                                    rxField.setCommentCn(rspPO.getCommentCn());
                                    rxField.setCommentEn(rspPO.getCommentEn());
                                    rxField.setLength(rspPO.getLength());
                                }
                            }
                        }
                        // 5. 解析出参
                        List<RxField> responseRxFields = new ArrayList<>();
                        Class<?> respType = method.getReturnType();
                        String responsePackage = respType.getPackage() != null ? respType.getPackage().getName() : null;
                        String responseClass = respType.getSimpleName();
                        String rspParentClass = null;
                        Class<?> rspSuper = respType.getSuperclass();
                        if (rspSuper != null && !"java.lang.Object".equals(rspSuper.getName())) {
                            rspParentClass = rspSuper.getName();
                        }
                        parseFieldsReflect(method.getGenericReturnType(), null, 1, 5, responseRxFields, classLoader, new java.util.HashSet<>());
                        methodEntity.setResponse(responseRxFields);
                        methodEntity.setReqParentClass(reqParentClass);
                        methodEntity.setRspParentClass(rspParentClass);
                        // 设置properties
                        InterFaceEntity.Properties prop = methodEntity.getProperties();
                        if (prop == null) {
                            prop = new InterFaceEntity.Properties();
                            methodEntity.setProperties(prop);
                        }
                        if (requestPackage != null) prop.setRequestPackage(requestPackage);
                        if (requestClass != null) prop.setRequestClass(requestClass);
                        if (responsePackage != null) prop.setResponsePackage(responsePackage);
                        if (responseClass != null) prop.setResponseClass(responseClass);
                        // 新增：收集object/List<object>和enums
                        java.util.Set<String> associatEntitySet = new java.util.HashSet<>();
                        java.util.Set<String> associatEnumSet = new java.util.HashSet<>();
                        java.util.function.Consumer<java.util.List<RxField>> collectAssociat = (fields) -> {
                            for (RxField f : fields) {
                                // object 或 List
                                if (("object".equals(f.getType()) || "List".equals(f.getType())) && f.getTypeRef() != null && !f.getTypeRef().isEmpty()) {
                                    associatEntitySet.add(f.getTypeRef());
                                }
                                // enums
                                if (f.getEnumNme() != null && !f.getEnumNme().isEmpty()) {
                                    associatEnumSet.add(f.getEnumNme());
                                }
                            }
                        };
                        collectAssociat.accept(requestRxFields);
                        collectAssociat.accept(responseRxFields);
                        if (!associatEntitySet.isEmpty()) {
                            methodEntity.setAssociatEntity(String.join(",", associatEntitySet));
                        }
                        if (!associatEnumSet.isEmpty()) {
                            methodEntity.setAssociatEnum(String.join(",", associatEnumSet));
                        }
                        result.add(methodEntity);
                    }
                }
            }catch (Throwable e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("反射解析失败: " + e.getMessage());
        }
        return result;
    }



    /**
     * 收集class文件名，支持JAR包和本地目录
     */
    private void collectClassNames(Object source, String pkg, List<String> classNames) {
        if (source instanceof String && ((String) source).toLowerCase().endsWith(".jar")) {
            // JAR包
            String jarFilePath = (String) source;
            try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarFilePath)) {
                java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(".class") && !name.contains("$")) {
                        // 将路径转换为包名
                        String className = name.replace("/", ".").replace(".class", "");
                        classNames.add(className);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("读取JAR包失败: " + e.getMessage());
            }
        } else if (source instanceof File) {
            // 本地目录
            File dir = (File) source;
            if (!dir.exists()) return;
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    collectClassNames(file, pkg + (pkg.isEmpty() ? "" : ".") + file.getName(), classNames);
                } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                    String className = pkg + (pkg.isEmpty() ? "" : ".") + file.getName().replace(".class", "");
                    classNames.add(className);
                }
            }
        }
    }

    // 递归解析字段，支持基础类型、List、对象，最多递归maxDepth层
    private void parseFieldsReflect(java.lang.reflect.Type type, String prefix, int depth, int maxDepth, java.util.List<RxField> resultRxFields, ClassLoader classLoader, java.util.Set<String> visited) {
        if (depth > maxDepth || type == null) return;
        String typeName = type.getTypeName();
        if (visited.contains(typeName)) return;
        visited.add(typeName);
        // 基础类型
        if (MrStringUtils.isJavaBasicType(typeName)) {
            RxField rxField = new RxField();
            rxField.setNameCamel(prefix == null ? "value" : prefix);
            rxField.setNameSnake(com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils.toUnderline(prefix == null ? "value" : prefix));
            // 输出简单名
            String simpleType = typeName.contains(".") ? typeName.substring(typeName.lastIndexOf('.') + 1) : typeName;
            rxField.setType(simpleType);
            resultRxFields.add(rxField);
            return;
        }
        // List类型
        if (type instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) type;
            if (pt.getRawType().getTypeName().startsWith("java.util.List")) {
                RxField listRxField = new RxField();
                listRxField.setNameCamel(prefix == null ? "list" : prefix);
                listRxField.setNameSnake(com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils.toUnderline(prefix == null ? "list" : prefix));
                listRxField.setType("List");
                // 设置真实类型
                java.lang.reflect.Type[] args = pt.getActualTypeArguments();
                if (args.length > 0) {
                    String realTypeName = args[0].getTypeName();
                    if (realTypeName.contains(".")) {
                        listRxField.setTypeRef(realTypeName.substring(realTypeName.lastIndexOf('.') + 1));
                    } else {
                        listRxField.setTypeRef(realTypeName);
                    }
                }
                resultRxFields.add(listRxField);
                if (args.length > 0) {
                    // 递归元素类型时，前缀始终保持为List字段名，不拼接.item
                    parseFieldsReflect(args[0], prefix, depth + 1, maxDepth, resultRxFields, classLoader, visited);
                }
                return;
            }
        }
        // 其他对象类型
        try {
            Class<?> clazz = null;
            if (type instanceof Class) {
                clazz = (Class<?>) type;
            } else if (type instanceof java.lang.reflect.ParameterizedType) {
                clazz = (Class<?>) ((java.lang.reflect.ParameterizedType) type).getRawType();
            } else {
                clazz = classLoader.loadClass(typeName);
            }
            if (clazz == null || clazz.isPrimitive() || clazz.getName().startsWith("java.")) {
                RxField rxField = new RxField();
                rxField.setNameCamel(prefix == null ? typeName : prefix);
                rxField.setNameSnake(com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils.toUnderline(prefix == null ? typeName : prefix));
                rxField.setType("object");
                // 设置真实类型
                if (typeName.contains(".")) {
                    rxField.setTypeRef(typeName.substring(typeName.lastIndexOf('.') + 1));
                } else {
                    rxField.setTypeRef(typeName);
                }
                resultRxFields.add(rxField);
                return;
            }
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (java.lang.reflect.Field f : fields) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                // 只有对象类型递归时才拼接字段名
                String fullFieldName = prefix == null ? f.getName() : prefix + "." + f.getName();
                String fTypeName = f.getType().getTypeName();
                // 新增：所有字段都处理ECPDict
                RxField rxFieldObj = null;
                if (MrStringUtils.isJavaBasicType(fTypeName)) {
                    rxFieldObj = new RxField();
                    rxFieldObj.setNameCamel(fullFieldName);
                    rxFieldObj.setNameSnake(com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils.toUnderline(fullFieldName));
                    String simpleType = fTypeName.contains(".") ? fTypeName.substring(fTypeName.lastIndexOf('.') + 1) : fTypeName;
                    rxFieldObj.setType(simpleType);
                    // 解析@ECPDict注解（包括enums）
                    handleEnumDict(f, rxFieldObj, classLoader);
                    // 其他注解解析（如notNull/length/desc）
                    try {
                        Class<?> ecpDictClass = classLoader.loadClass("com.murong.ecp.bp.common.dict.ECPDict");
                        Annotation ann = f.getAnnotation((Class<Annotation>) ecpDictClass);
                        if (ann != null) {
                            // required
                            try {
                                Boolean required = (Boolean) ann.annotationType().getMethod("required").invoke(ann);
                                rxFieldObj.setNotNull(required);
                            } catch (Exception ignore) {}
                            // length
                            try {
                                Integer length = (Integer) ann.annotationType().getMethod("length").invoke(ann);
                                if (length != null && length < 0) {
                                    rxFieldObj.setLength(null);
                                } else {
                                    rxFieldObj.setLength(length);
                                }
                            } catch (Exception ignore) {}
                            // desc
                            try {
                                String desc = (String) ann.annotationType().getMethod("desc").invoke(ann);
                                String[] remarks = desc.split("\\|");
                                String remark_cn = remarks.length > 0 ? remarks[0].trim() : "";
                                String remark_en = remarks.length > 1 ? remarks[1].trim() : "";
                                rxFieldObj.setCommentCn(remark_cn);
                                rxFieldObj.setCommentEn(remark_en);
                            } catch (Exception ignore) {}
                        }
                    } catch (Throwable ignore) {}
                    resultRxFields.add(rxFieldObj);
                    continue;
                } else if (f.getType().isAssignableFrom(java.util.List.class) || fTypeName.startsWith("java.util.List")) {
                    rxFieldObj = new RxField();
                    rxFieldObj.setNameCamel(fullFieldName);
                    rxFieldObj.setNameSnake(com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils.toUnderline(fullFieldName));
                    rxFieldObj.setType("List");
                    // 设置真实类型
                    java.lang.reflect.Type fGenericType = f.getGenericType();
                    if (fGenericType instanceof java.lang.reflect.ParameterizedType) {
                        java.lang.reflect.Type[] args = ((java.lang.reflect.ParameterizedType) fGenericType).getActualTypeArguments();
                        if (args.length > 0) {
                            String realTypeName = args[0].getTypeName();
                            if (realTypeName.contains(".")) {
                                rxFieldObj.setTypeRef(realTypeName.substring(realTypeName.lastIndexOf('.') + 1));
                            } else {
                                rxFieldObj.setTypeRef(realTypeName);
                            }
                        }
                    }
                    // 解析@ECPDict注解（包括enums）
                    handleEnumDict(f, rxFieldObj, classLoader);
                    // 其他注解解析（如notNull/length/desc）
                    try {
                        Class<?> ecpDictClass = classLoader.loadClass("com.murong.ecp.bp.common.dict.ECPDict");
                        Annotation ann = f.getAnnotation((Class<Annotation>) ecpDictClass);
                        if (ann != null) {
                            // required
                            try {
                                Boolean required = (Boolean) ann.annotationType().getMethod("required").invoke(ann);
                                rxFieldObj.setNotNull(required);
                            } catch (Exception ignore) {}
                            // length
                            try {
                                Integer length = (Integer) ann.annotationType().getMethod("length").invoke(ann);
                                if (length != null && length < 0) {
                                    rxFieldObj.setLength(null);
                                } else {
                                    rxFieldObj.setLength(length);
                                }
                            } catch (Exception ignore) {}
                            // desc
                            try {
                                String desc = (String) ann.annotationType().getMethod("desc").invoke(ann);
                                String[] remarks = desc.split("\\|");
                                String remark_cn = remarks.length > 0 ? remarks[0].trim() : "";
                                String remark_en = remarks.length > 1 ? remarks[1].trim() : "";
                                rxFieldObj.setCommentCn(remark_cn);
                                rxFieldObj.setCommentEn(remark_en);
                            } catch (Exception ignore) {}
                        }
                    } catch (Throwable ignore) {}
                    resultRxFields.add(rxFieldObj);
                    // 递归List元素类型时，前缀保持为List字段名，不拼接.item
                    java.lang.reflect.Type genericType = f.getGenericType();
                    if (genericType instanceof java.lang.reflect.ParameterizedType) {
                        java.lang.reflect.Type[] args = ((java.lang.reflect.ParameterizedType) genericType).getActualTypeArguments();
                        if (args.length > 0) {
                            parseFieldsReflect(args[0], fullFieldName, depth + 1, maxDepth, resultRxFields, classLoader, visited);
                        }
                    }
                    continue;
                } else {
                    rxFieldObj = new RxField();
                    rxFieldObj.setNameCamel(fullFieldName);
                    rxFieldObj.setNameSnake(com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils.toUnderline(fullFieldName));
                    rxFieldObj.setType("object");
                    // 设置真实类型
                    if (fTypeName.contains(".")) {
                        rxFieldObj.setTypeRef(fTypeName.substring(fTypeName.lastIndexOf('.') + 1));
                    } else {
                        rxFieldObj.setTypeRef(fTypeName);
                    }
                    // 解析@ECPDict注解（包括enums）
                    handleEnumDict(f, rxFieldObj, classLoader);
                    // 其他注解解析（如notNull/length/desc）
                    try {
                        Class<?> ecpDictClass = classLoader.loadClass("com.murong.ecp.bp.common.dict.ECPDict");
                        Annotation ann = f.getAnnotation((Class<Annotation>) ecpDictClass);
                        if (ann != null) {
                            // required
                            try {
                                Boolean required = (Boolean) ann.annotationType().getMethod("required").invoke(ann);
                                rxFieldObj.setNotNull(required);
                            } catch (Exception ignore) {}
                            // length
                            try {
                                Integer length = (Integer) ann.annotationType().getMethod("length").invoke(ann);
                                if (length != null && length < 0) {
                                    rxFieldObj.setLength(null);
                                } else {
                                    rxFieldObj.setLength(length);
                                }
                            } catch (Exception ignore) {}
                            // desc
                            try {
                                String desc = (String) ann.annotationType().getMethod("desc").invoke(ann);
                                String[] remarks = desc.split("\\|");
                                String remark_cn = remarks.length > 0 ? remarks[0].trim() : "";
                                String remark_en = remarks.length > 1 ? remarks[1].trim() : "";
                                rxFieldObj.setCommentCn(remark_cn);
                                rxFieldObj.setCommentEn(remark_en);
                            } catch (Exception ignore) {}
                        }
                    } catch (Throwable ignore) {}
                    resultRxFields.add(rxFieldObj);
                    parseFieldsReflect(f.getGenericType(), fullFieldName, depth + 1, maxDepth, resultRxFields, classLoader, visited);
                }
            }
        } catch (Throwable e) {
            // 采用方案2：遇到找不到的类等异常时兜底为object类型
            if (!(e instanceof ClassNotFoundException) && !(e instanceof NoClassDefFoundError)) {
                e.printStackTrace();
            }
            RxField rxField = new RxField();
            rxField.setNameCamel(prefix == null ? typeName : prefix);
            rxField.setNameSnake(com.murong.ecp.tools.fx.infrastructure.utils.MrStringUtils.toUnderline(prefix == null ? typeName : prefix));
            rxField.setType("object");
            // 设置真实类型
            if (typeName.contains(".")) {
                rxField.setTypeRef(typeName.substring(typeName.lastIndexOf('.') + 1));
            } else {
                rxField.setTypeRef(typeName);
            }
            resultRxFields.add(rxField);
        }
    }

    /**
     * 处理字段上的ECPDict注解，解析enums并存储到EnumDictPO
     */
    private void handleEnumDict(Field f, RxField rxField, ClassLoader classLoader) {
        try {
            Class<?> ecpDictClass = classLoader.loadClass("com.murong.ecp.bp.common.dict.ECPDict");
            Annotation ann = f.getAnnotation((Class<Annotation>) ecpDictClass);
            if (ann != null) {
                // enums
                try {
                    Object enumsValue = ann.annotationType().getMethod("enums").invoke(ann);
                    if (enumsValue != null) {
                        Class<?> enumsClass = (Class<?>) enumsValue;
                        if (!"com.murong.ecp.bp.common.dict.MrEnumValue".equals(enumsClass.getName())) {
                            // 检查是否继承IBasicEnum
                            try {
                                Class<?> ibasicEnumClass = classLoader.loadClass("com.murong.ecp.m5.dict.IBasicEnum");
                                if (ibasicEnumClass.isAssignableFrom(enumsClass)) {
                                    rxField.setEnumRef(enumsClass.getPackage().getName());
                                    rxField.setEnumNme(enumsClass.getSimpleName());
                                    // 反射获取枚举项
                                    Object[] enumConstants = enumsClass.getEnumConstants();
                                    if (enumConstants != null) {
                                        for (Object enumConst : enumConstants) {
                                            String enumCd = null;
                                            String enumVal = null;
                                            String descCn = null;
                                            String descEn = null;
                                            try {
                                                enumCd = ((Enum<?>) enumConst).name();
                                            } catch (Exception ignore) {ignore.printStackTrace();}
                                            try {
                                                java.lang.reflect.Method getValue = enumsClass.getMethod("getValue");
                                                Object val = getValue.invoke(enumConst);
                                                enumVal = val != null ? val.toString() : null;
                                            } catch (Exception ignore) {ignore.printStackTrace();}
                                            try {
                                                java.lang.reflect.Method getDesc = enumsClass.getMethod("getDesc");
                                                Object val = getDesc.invoke(enumConst);
                                                if (val != null) {
                                                    String desc = val.toString();
                                                    String[] remarks = desc.split("\\|");
                                                    descCn = remarks.length > 0 ? remarks[0].trim() : "";
                                                    descEn = remarks.length > 1 ? remarks[1].trim() : "";
                                                }
                                            } catch (NoSuchMethodException e) {
                                                // 兼容getDescCn/getDescEn
                                                try {
                                                    java.lang.reflect.Method getDescCn = enumsClass.getMethod("getDescCn");
                                                    Object valCn = getDescCn.invoke(enumConst);
                                                    descCn = valCn != null ? valCn.toString() : null;
                                                } catch (Exception ignore2) {}
                                                try {
                                                    java.lang.reflect.Method getDescEn = enumsClass.getMethod("getDescEn");
                                                    Object valEn = getDescEn.invoke(enumConst);
                                                    descEn = valEn != null ? valEn.toString() : null;
                                                } catch (Exception ignore2) {ignore2.printStackTrace();}
                                            } catch (Exception ignore) {ignore.printStackTrace();}
                                            // 构造EnumDictPO
                                            EnumDictPO po = new EnumDictPO();
                                            po.setGroupName(globalPropes.getGroupName());
                                            po.setProjectName(globalPropes.getProjectName());
                                            po.setEnumNme(enumsClass.getSimpleName());
                                            po.setAppName(globalPropes.getAppName());
                                            po.setDbName(BusinessUtils.enumNameToDbName(enumsClass.getSimpleName()));
                                            po.setEnumRef(enumsClass.getPackage().getName());
                                            po.setEnumCd(enumCd);
                                            po.setEnumVal(enumVal);
                                            po.setDescCn(descCn);
                                            po.setDescEn(descEn);
                                            if(StringUtils.isBlank(descEn) && BusinessUtils.isAllEnglish(descCn)) {
                                                po.setDescEn(descCn);
                                            }

                                            EnumDictPO query = new EnumDictPO();
                                            query.setGroupName(globalPropes.getGroupName());
                                            query.setAppName(globalPropes.getAppName());
                                            query.setEnumNme(enumsClass.getSimpleName());
                                            query.setEnumRef(enumsClass.getPackage().getName());
                                            query.setEnumVal(enumVal);
                                            EnumDictPO enumDictPO = enumDictDao.queryInfcDataHis(query);
                                            if(enumDictPO!=null){
                                                po.setEnumCd(enumDictPO.getEnumCd());
                                                po.setDescCn(enumDictPO.getDescCn());
                                                po.setDescEn(enumDictPO.getDescEn());
                                                po.setDbName(enumDictPO.getDbName());
                                            }
                                            enumDictDao.upsert(po);
                                        }
                                    }
                                }
                            } catch (Exception ignore) {
                                if(ignore.getCause()!=null && StringUtils.contains(ignore.getCause().getMessage(),"com.murong.ecp.cbp.dict")){
                                }else  {
                                    ignore.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {
                    if(ignore.getCause()!=null && StringUtils.contains(ignore.getCause().getMessage(),"com.murong.ecp.cbp.dict")){
                    }else  {
                        ignore.printStackTrace();
                    }
                }
            }
        } catch (Throwable ignore) {
            ignore.printStackTrace();
        }
    }

    /**
     * 解析单独的java文件转换成table
     **/
    public TableEntity javaEntityConvTable(String scanPath) {
        Path javaFile = Paths.get(scanPath);
        TableEntity tableEntity = new TableEntity();
        String className = javaFile.getFileName().toString().replace(".java", "");
        String classPath = javaFile.toAbsolutePath().toString().replace(File.separator, ".");
        classPath=classPath.replace(javaFile.getFileName().toString(),"");
        String prefix = "src.main.java.";
        classPath=classPath.substring(classPath.indexOf(prefix)+prefix.length(), classPath.length());
        try {
            List<String> lines = Files.readAllLines(javaFile);
            String field = null;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("@ECPDict")) {
                    StringBuilder ann = new StringBuilder(line);
                    while (!line.endsWith(")")) {
                        i++;
                        line = lines.get(i).trim();
                        ann.append(line);
                    }
                    while (++i < lines.size()) {
                        String next = lines.get(i).trim();
                        if (!next.isEmpty() && !next.startsWith("//")) {
                            field = next;
                            break;
                        }
                    }
                    if (field != null) {
                        addField(tableEntity,classPath,className, ann.toString(), field);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析失败: " + javaFile + ", " + e.getMessage());
        }
        return tableEntity;
    }

    private void addField(TableEntity tableEntity,String classPath,String className, String ann, String field) {
        // 解析注解参数
        Map<String, String> map = new HashMap<>();
        Matcher m = Pattern.compile("(\\w+)\\s*=\\s*(\"[^\"]*\"|[^,\\)\"]+)").matcher(ann);
        while (m.find()) {
            String value = m.group(2);
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            map.put(m.group(1), value);
        }
        // 字段名
        String fieldName = field.replaceAll(".*\\s(\\w+)\\s*;.*", "$1");
        String fieldType = field.replaceAll(".*\\s(\\w+)\\s+\\w+\\s*;.*", "$1");
        // 备注中英文
        String desc = map.getOrDefault("desc", "|");
        String[] remarks = desc.split("\\|");
        String remark_cn = remarks.length > 0 ? remarks[0].trim() : "";
        String remark_en = remarks.length > 1 ? remarks[1].trim() : "";
        remark_cn=remark_cn.replace("'", "''");
        remark_en=remark_en.replace("'", "''");
        String lengthStr = map.get("length");
        String fieldNameSnake= MrStringUtils.toUnderline(fieldName);

        tableEntity.addField(fieldName,fieldNameSnake,fieldType,fieldType,lengthStr == null ? 0 : Integer.parseInt(lengthStr),remark_cn,remark_en);
    }

}
