package com.murong.ecp.tools.fx.domain.service.interfaces;

import com.murong.ecp.m5.dict.IBasicEnum;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.BizMsgInfoDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.EnumDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizMsgInfoPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.utils.BusinessUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class EnumsCdAnalyService {

    @Autowired
    private EnumDictDao enumDictDao;

    @Autowired
    private BizMsgInfoDao bizMsgInfoDao;

    @Autowired
    private GlobalProperties globalProperties;

    public void analyzeMsgCode(String basePath,GlobalProperties.CrMsgCode crMsgCode) {
        // 正确处理包名到路径的转换
        String packagePath = crMsgCode.getUrl().replace(".", File.separator);
        String msgCdFilePath = basePath + File.separator + crMsgCode.getBasePath() + File.separator +
                packagePath + ".java";
        String moduleName = BusinessUtils.extractModuleName(crMsgCode.getBasePath());

        try {
            System.out.println("开始分析消息代码文件: " + msgCdFilePath);

            // 验证文件是否存在
            File msgCdFile = new File(msgCdFilePath);
            if (!msgCdFile.exists() || !msgCdFile.isFile()) {
                System.out.println("文件不存在或不是有效文件: " + msgCdFilePath);
                return;
            }

            // 创建类加载器 - 需要传递项目根目录
            File projectRoot = findProjectRoot(msgCdFile);
            URLClassLoader classLoader = createClassLoader(projectRoot.getAbsolutePath());

            // 获取类名
            String className = getClassNameFromFile(msgCdFile);
            if (className == null) {
                return;
            }

            System.out.println("分析消息代码类: " + className);

            try {
                // 加载类
                Class<?> clazz = classLoader.loadClass(className);

                // 检查是否实现了IMessageCode接口
                Class<?> iMessageCodeClass = classLoader.loadClass("com.yuangou.ecp.bp.comp.pubatc.IMessageCode");
                if (iMessageCodeClass.isAssignableFrom(clazz) && clazz.isEnum()) {
                    analyzeMessageCodeEnum(moduleName,clazz);
                } else {
                    System.out.println("类 " + className + " 不是实现了IMessageCode接口的枚举类");
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("消息代码分析完成");

        } catch (Exception e) {
            System.err.println("分析消息代码文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void analyzeDict(String basePath,GlobalProperties.CrEnum crEnum) {
        // 正确处理包名到路径的转换
        String packagePath = crEnum.getUrl().replace(".", File.separator);
        String dictPath = basePath + File.separator + crEnum.getBasePath() + File.separator + packagePath;
        String moduleName = BusinessUtils.extractModuleName(crEnum.getBasePath());
        try {
            System.out.println("开始分析枚举目录: " + dictPath);
            
            // 验证目录是否存在
            File dictDir = new File(dictPath);
            if (!dictDir.exists() || !dictDir.isDirectory()) {
                System.out.println("目录不存在或不是有效目录: " + dictPath);
                return;
            }

            // 递归遍历目录下的所有Java文件
            List<File> javaFiles = findJavaFiles(dictDir);
            System.out.println("找到 " + javaFiles.size() + " 个Java文件");

            // 创建类加载器 - 需要传递项目根目录
            File projectRoot = findProjectRoot(dictDir);
            URLClassLoader classLoader = createClassLoader(projectRoot.getAbsolutePath());
            
            // 解析每个Java文件
            for (File javaFile : javaFiles) {
                try {
                    analyzeJavaFile(moduleName,javaFile, classLoader);
                } catch (Exception e) {
                    System.err.println("解析文件失败: " + javaFile.getPath() + ", 错误: " + e.getMessage());
                }
            }

            System.out.println("枚举分析完成");
            
        } catch (Exception e) {
            System.err.println("分析枚举目录时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 递归查找目录下的所有Java文件
     */
    private List<File> findJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            paths.filter(path -> path.toString().endsWith(".java"))
                 .forEach(path -> javaFiles.add(path.toFile()));
        } catch (Exception e) {
            System.err.println("遍历目录时发生错误: " + e.getMessage());
        }
        
        return javaFiles;
    }

    /**
     * 创建类加载器
     */
    private URLClassLoader createClassLoader(String dictPath) throws Exception {
        // 查找项目的target/classes目录
        File dictDir = new File(dictPath);
        File projectRoot = dictDir;
        
        // 向上查找项目根目录（包含pom.xml的目录）
        while (projectRoot != null && !new File(projectRoot, "pom.xml").exists()) {
            projectRoot = projectRoot.getParentFile();
        }
        
        if (projectRoot == null) {
            throw new RuntimeException("无法找到项目根目录");
        }
        
        // 查找target/classes目录
        File targetClasses = new File(projectRoot, "target/classes");
        if (!targetClasses.exists()) {
            throw new RuntimeException("无法找到target/classes目录，请先编译项目");
        }
        
        URL[] urls = {targetClasses.toURI().toURL()};
        return new URLClassLoader(urls, getClass().getClassLoader());
    }

    /**
     * 分析单个Java文件
     */
    private void analyzeJavaFile(String moduleName,File javaFile, URLClassLoader classLoader) throws Exception {
        // 将文件路径转换为类名
        String className = getClassNameFromFile(javaFile);
        if (className == null) {
            return;
        }
        
        System.out.println("分析类: " + className);
        
        try {
            // 加载类
            Class<?> clazz = classLoader.loadClass(className);
            
            // 检查是否实现了IBasicEnum接口
            if (IBasicEnum.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                analyzeEnumClass(moduleName,clazz);
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("无法加载类: " + className + ", 错误: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("分析类时发生错误: " + className + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 从文件路径获取类名
     */
    private String getClassNameFromFile(File javaFile) {
        try {
            String filePath = javaFile.getAbsolutePath();
            System.out.println("处理文件: " + filePath);
            
            // 查找src/main/java目录
            String srcMainJavaPath = findSrcMainJavaPath(javaFile);
            if (srcMainJavaPath == null) {
                System.err.println("无法找到src/main/java目录: " + javaFile.getPath());
                return null;
            }
            
            System.out.println("找到src/main/java路径: " + srcMainJavaPath);
            
            // 从src/main/java开始计算相对路径
            String relativePath = filePath.substring(srcMainJavaPath.length() + 1);
            System.out.println("相对路径: " + relativePath);
            
            if (relativePath.endsWith(".java")) {
                relativePath = relativePath.substring(0, relativePath.length() - 5);
            }
            
            // 将路径分隔符替换为点
            String className = relativePath.replace(File.separator, ".");
            System.out.println("生成的类名: " + className);
            
            return className;
            
        } catch (Exception e) {
            System.err.println("无法从文件路径获取类名: " + javaFile.getPath() + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查找src/main/java目录
     */
    private String findSrcMainJavaPath(File file) {
        System.out.println("开始查找src/main/java目录，从文件: " + file.getPath());
        
        File current = file.getParentFile();
        while (current != null) {
            // 检查当前目录是否是java目录
            if (current.getName().equals("java")) {
                File mainDir = current.getParentFile();
                if (mainDir != null && mainDir.getName().equals("main")) {
                    File srcDir = mainDir.getParentFile();
                    if (srcDir != null && srcDir.getName().equals("src")) {
                        // 返回src/main/java的完整路径
                        String result = current.getAbsolutePath();
                        System.out.println("找到标准src/main/java结构: " + result);
                        return result;
                    }
                }
            }
            current = current.getParentFile();
        }
        
        // 如果找不到标准的src/main/java结构，尝试其他可能的路径
        System.out.println("未找到标准src/main/java结构，尝试查找java目录");
        current = file.getParentFile();
        while (current != null) {
            // 检查是否有java目录
            if (current.getName().equals("java")) {
                String result = current.getAbsolutePath();
                return result;
            }
            current = current.getParentFile();
        }
        return null;
    }

    /**
     * 查找项目根目录
     */
    private File findProjectRoot(File file) {
        File current = file.getParentFile();
        while (current != null) {
            if (new File(current, "pom.xml").exists()) {
                return current;
            }
            current = current.getParentFile();
        }
        return file.getParentFile(); // 如果找不到pom.xml，返回父目录
    }

    /**
     * 分析枚举类
     */
    private void analyzeEnumClass(String moduleName,Class<?> enumClass) {
        try {
            System.out.println("分析枚举类: " + enumClass.getName());
            
            // 获取枚举常量
            Object[] enumConstants = enumClass.getEnumConstants();
            if (enumConstants == null) {
                return;
            }
            
            // 获取包信息
            String packageName = enumClass.getPackage() != null ? enumClass.getPackage().getName() : "";
            String className = enumClass.getSimpleName();
            
            // 分析每个枚举常量
            for (Object enumConstant : enumConstants) {
                if (enumConstant instanceof IBasicEnum) {
                    IBasicEnum basicEnum = (IBasicEnum) enumConstant;
                    saveEnumToDatabase(moduleName,enumClass, basicEnum, packageName);
                }
            }
            
        } catch (Exception e) {
            System.err.println("分析枚举类时发生错误: " + enumClass.getName() + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 保存枚举到数据库
     */
    private void saveEnumToDatabase(String moduleName,Class<?> enumClass, IBasicEnum basicEnum, String packageName) {
        try {
            EnumDictPO enumDictPO = new EnumDictPO();
            
            // 设置基本信息
            enumDictPO.setGroupName(globalProperties.getGroupName());
            enumDictPO.setProjectName(globalProperties.getProjectName());
            enumDictPO.setAppName(globalProperties.getAppName());
            enumDictPO.setModuleName(moduleName);
            
            // 设置枚举信息
            enumDictPO.setEnumNme(enumClass.getSimpleName());
            enumDictPO.setEnumRef(packageName + "." + enumClass.getSimpleName());
            enumDictPO.setEnumCd(((Enum<?>) basicEnum).name()); // 使用枚举常量的name()作为enum_cd
            enumDictPO.setEnumVal(basicEnum.getValue());
            enumDictPO.setDescCn(basicEnum.getDesc());
            enumDictPO.setDescEn(basicEnum.getDesc()); // 暂时使用中文描述作为英文描述
            
            // 设置数据库名称 - 使用BusinessUtils.enumNameToDbName方法
            enumDictPO.setDbName(BusinessUtils.enumNameToDbName(enumClass.getSimpleName()));
            
            // 保存到数据库
            enumDictDao.upsert(enumDictPO);

        } catch (Exception e) {
            System.err.println("保存枚举到数据库时发生错误: " + e.getMessage());
        }
    }

    /**
     * 分析实现了IMessageCode接口的枚举类
     */
    private void analyzeMessageCodeEnum(String moduleName,Class<?> msgCodeEnumClass) {
        try {
            System.out.println("分析消息代码枚举类: " + msgCodeEnumClass.getName());
            
            // 获取包信息
            String packageName = msgCodeEnumClass.getPackage() != null ? msgCodeEnumClass.getPackage().getName() : "";
            String className = msgCodeEnumClass.getSimpleName();
            
            // 获取枚举常量
            Object[] enumConstants = msgCodeEnumClass.getEnumConstants();
            if (enumConstants == null) {
                return;
            }
            
            // 分析每个枚举常量
            for (Object enumConstant : enumConstants) {
                try {
                    // 通过反射调用getMsgCod()和getMsgInf()方法
                    Method getMsgCodMethod = msgCodeEnumClass.getMethod("getMsgCod");
                    Method getMsgInfMethod = msgCodeEnumClass.getMethod("getMsgInf");
                    
                    String msgCd = (String) getMsgCodMethod.invoke(enumConstant);
                    String msgInf = (String) getMsgInfMethod.invoke(enumConstant);
                    String msgKey = ((Enum<?>) enumConstant).name();
                    
                    // 保存到数据库
                    saveMessageCode(moduleName,msgCodeEnumClass, msgKey, msgCd, msgInf, packageName);
                    
                } catch (Exception e) {
                    System.err.println("解析枚举常量失败: " + enumConstant + ", 错误: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("分析消息代码枚举类时发生错误: " + msgCodeEnumClass.getName() + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 保存消息代码到数据库
     */
    private void saveMessageCode(String moduleName,Class<?> msgCodeClass,
                                           String msgKey, String msgCd, String msgInf, String packageName) {
        try {
            BizMsgInfoPO bizMsgInfoPO = new BizMsgInfoPO();
            
            // 设置基本信息
            bizMsgInfoPO.setGroupName(globalProperties.getGroupName());
            bizMsgInfoPO.setProjectName(globalProperties.getProjectName());
            bizMsgInfoPO.setAppName(globalProperties.getAppName());
            
            // 设置消息信息
            bizMsgInfoPO.setMsgClass(msgCodeClass.getSimpleName());
            bizMsgInfoPO.setMsgRef(packageName + "." + msgCodeClass.getSimpleName());
            bizMsgInfoPO.setMsgKey(msgKey);
            bizMsgInfoPO.setMsgCd(msgCd);
            bizMsgInfoPO.setMsgDescCn(msgInf);
            bizMsgInfoPO.setMsgDescEn(msgInf); // 暂时使用中文描述作为英文描述
            
            // 设置模块信息（从包名推断）
            bizMsgInfoPO.setModuleName(moduleName);
            
            // 设置更新信息
            bizMsgInfoPO.setUpdateBy("system");
            bizMsgInfoPO.setUpdateTime(java.time.LocalDateTime.now().toString());
            
            // 保存到数据库（使用upsert避免重复插入）
            bizMsgInfoDao.upsert(bizMsgInfoPO);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
