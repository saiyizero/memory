package com.murong.ecp.tools.fx.domain.service.structure;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.ProjectFolderDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectFolderPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ProjectSettingPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StructureService {
    @Autowired
    ProjectFolderDao projectFolderDao;
    @Autowired
    GlobalProperties globalPropes;

    public List<ProjectFolderPO> synchronousPath(ProjectSettingPO projectSetting) {
        if (projectSetting == null||
                StringUtils.isEmpty(projectSetting.getProjectName())||
                StringUtils.isEmpty(projectSetting.getGroupName())) {
            throw new RuntimeException("ProjectSetting can not be null");
        }

        List<ProjectFolderPO> projectFolders = scanProjectDir(projectSetting.getBasePath());
        if(!CollectionUtils.isEmpty(projectFolders)){
            ProjectFolderPO wherePo = new ProjectFolderPO();
            wherePo.setGroupName(projectSetting.getGroupName());
            wherePo.setProjectName(projectSetting.getProjectName());
            projectFolderDao.delete(wherePo);
            for (ProjectFolderPO projectFolder : projectFolders) {
                projectFolderDao.save(projectFolder);
            }
        }
        return projectFolders;
    }

    /**
     * 扫描项目跟目录
     * 根据跟目录生成项目路径配置
     **/
    public List<ProjectFolderPO> scanProjectDir(String scanDir) {
        List<ProjectFolderPO> result = new ArrayList<>();
        
        if (scanDir == null || scanDir.trim().isEmpty()) {
            return result;
        }
        
        File projectDir = new File(scanDir);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            return result;
        }
        
        // 1. 扫描所有模块
        Set<String> modules = scanModules(projectDir);
        
        // 2. 为每个模块扫描不同类型的目录
        for (String moduleName : modules) {
            String modulePath = scanDir + "/" + moduleName;
            File moduleDir = new File(modulePath);
            if (!moduleDir.exists() || !moduleDir.isDirectory()) {
                continue;
            }
            
            // 扫描各种类型的目录
            scanInterfaceDirectories(moduleDir, moduleName, result);
            scanRequestDirectories(moduleDir, moduleName, result);
            scanResponseDirectories(moduleDir, moduleName, result);
            scanEnumDirectories(moduleDir, moduleName, result);
            scanActionDirectories(moduleDir, moduleName, result);
            scanControllerDirectories(moduleDir, moduleName, result);
            scanDomainDirectories(moduleDir, moduleName, result);
            scanMapperDirectories(moduleDir, moduleName, result);
            scanEntityDirectories(moduleDir, moduleName, result);
            scanMsgCodeDirectories(moduleDir, moduleName, result);
            scanPropPathFiles(moduleDir, moduleName, result);
            scanXmlDirectories(moduleDir, moduleName, result);
        }
        
        // 3. 合并相同模块和类型的路径
        return mergePaths(result);
    }
    
    /**
     * 扫描项目中的所有模块
     */
    private Set<String> scanModules(File projectDir) {
        Set<String> modules = new HashSet<>();
        File[] files = projectDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 检查是否是模块目录（包含src/main/java或src/main/resources）
                    if (isModuleDirectory(file)) {
                        modules.add(file.getName());
                    }
                }
            }
        }
        return modules;
    }
    
    /**
     * 判断是否是模块目录
     */
    private boolean isModuleDirectory(File dir) {
        File srcDir = new File(dir, "src");
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            return false;
        }
        
        File mainDir = new File(srcDir, "main");
        if (!mainDir.exists() || !mainDir.isDirectory()) {
            return false;
        }
        
        File javaDir = new File(mainDir, "java");
        File resourcesDir = new File(mainDir, "resources");
        
        return javaDir.exists() || resourcesDir.exists();
    }
    
    /**
     * 扫描interface类型的目录
     */
    private void scanInterfaceDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        scanJavaDirectories(moduleDir, moduleName, "interface", result, (file) -> {
            try {
                String content = Files.readString(file.toPath());
                return content.contains("@ECPClient");
            } catch (IOException e) {
                return false;
            }
        });
    }
    
    /**
     * 扫描request类型的目录
     */
    private void scanRequestDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        scanJavaDirectories(moduleDir, moduleName, "request", result, (file) -> {
            try {
                String content = Files.readString(file.toPath());
                String fileName = file.getName();
                return fileName.endsWith("ReqBO.java") && 
                       (content.contains("extends AbstractBaseGDA") || 
                        content.contains("AbstractBaseGDA"));
            } catch (IOException e) {
                return false;
            }
        });
    }
    
    /**
     * 扫描response类型的目录
     */
    private void scanResponseDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        scanJavaDirectories(moduleDir, moduleName, "response", result, (file) -> {
            try {
                String content = Files.readString(file.toPath());
                String fileName = file.getName();
                return fileName.endsWith("RspBO.java") && 
                       (content.contains("extends AbstractBaseGDA") || 
                        content.contains("AbstractBaseGDA"));
            } catch (IOException e) {
                return false;
            }
        });
    }
    
    /**
     * 扫描enums类型的目录
     */
    private void scanEnumDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        scanJavaDirectories(moduleDir, moduleName, "enums", result, (file) -> {
            try {
                String content = Files.readString(file.toPath());
                return content.contains("enum ") && content.contains("implements IBasicEnum");
            } catch (IOException e) {
                return false;
            }
        });
    }
    
    /**
     * 扫描action类型的目录
     */
    private void scanActionDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        scanJavaDirectories(moduleDir, moduleName, "action", result, (file) -> {
            try {
                String content = Files.readString(file.toPath());
                return content.contains("extends MrTransaction");
            } catch (IOException e) {
                return false;
            }
        });
    }
    
    /**
     * 扫描controller类型的目录
     */
    private void scanControllerDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        scanJavaDirectories(moduleDir, moduleName, "controller", result, (file) -> {
            try {
                String content = Files.readString(file.toPath());
                return content.contains("@ECPService");
            } catch (IOException e) {
                return false;
            }
        });
    }
    
    /**
     * 扫描domain类型的目录
     */
    private void scanDomainDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        File javaDir = new File(moduleDir, "src/main/java");
        if (!javaDir.exists()) {
            return;
        }
        
        // 查找名为"domain"的目录
        findDirectoriesByName(javaDir, "domain", moduleName, "domain", result, javaDir);
    }
    
    /**
     * 扫描mapper类型的目录
     */
    private void scanMapperDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        // 首先查找application.properties文件中的mapper配置
        File resourcesDir = new File(moduleDir, "src/main/resources");
        if (!resourcesDir.exists()) {
            return;
        }
        
        Set<String> mapperPaths = new HashSet<>();
        findApplicationPropertiesFiles(resourcesDir).forEach(propFile -> {
            try {
                String content = Files.readString(propFile.toPath());
                Pattern pattern = Pattern.compile("ecp\\.datasource\\.mapper\\s*=\\s*(.+)");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    String mapperPathConfig = matcher.group(1).trim();
                    // 处理多个mapper路径（用逗号分隔）
                    String[] paths = mapperPathConfig.split(",");
                    for (String path : paths) {
                        mapperPaths.add(path.trim());
                    }
                }
            } catch (IOException e) {
                // 忽略错误
            }
        });
        
        // 检查mapper路径下是否有interface类
        for (String mapperPath : mapperPaths) {
            // 尝试在当前模块中查找
            checkMapperPathInModule(moduleDir, moduleName, mapperPath, result);
            
            // 如果当前模块是web模块，也尝试在其他模块中查找
            if (moduleName.endsWith("-web")) {
                // 获取项目根目录
                File projectRoot = moduleDir.getParentFile();
                if (projectRoot != null && projectRoot.exists()) {
                    File[] siblingModules = projectRoot.listFiles(File::isDirectory);
                    if (siblingModules != null) {
                        for (File siblingModule : siblingModules) {
                            if (!siblingModule.getName().equals(moduleName) && 
                                siblingModule.getName().startsWith("remittance-")) {
                                checkMapperPathInModule(siblingModule, siblingModule.getName(), mapperPath, result);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 在指定模块中检查mapper路径
     */
    private void checkMapperPathInModule(File moduleDir, String moduleName, String mapperPath, List<ProjectFolderPO> result) {
        String[] pathParts = mapperPath.split("\\.");
        File currentDir = new File(moduleDir, "src/main/java");
        
        // 构建完整路径
        for (String part : pathParts) {
            currentDir = new File(currentDir, part);
        }
        
        if (currentDir.exists() && currentDir.isDirectory()) {
            File[] files = currentDir.listFiles((dir, name) -> name.endsWith(".java"));
            if (files != null && files.length > 0) {
                for (File file : files) {
                    try {
                        String content = Files.readString(file.toPath());
                        if (content.contains("interface ")) {
                            String packagePath = getPackagePath(currentDir, new File(moduleDir, "src/main/java"));
                            addProjectFolder(result, moduleName, "src/main/java", "mapper", packagePath);
                            return; // 找到一个interface就足够了
                        }
                    } catch (IOException e) {
                        // 忽略错误
                    }
                }
            }
        }
    }
    
    /**
     * 扫描entity类型的目录
     */
    private void scanEntityDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        // 先找到mapper目录
        File javaDir = new File(moduleDir, "src/main/java");
        if (!javaDir.exists()) {
            return;
        }
        
        // 查找mapper接口中引用的PO/DO类
        findJavaFiles(javaDir).forEach(file -> {
            try {
                String content = Files.readString(file.toPath());
                if (content.contains("interface ")) {
                    // 查找import语句中的PO/DO类
                    Pattern importPattern = Pattern.compile("import\\s+([^;]+);");
                    Matcher matcher = importPattern.matcher(content);
                    while (matcher.find()) {
                        String importPath = matcher.group(1);
                        if (importPath.endsWith("PO") || importPath.endsWith("DO")) {
                            String[] pathParts = importPath.split("\\.");
                            String className = pathParts[pathParts.length - 1];
                            if (className.endsWith("PO") || className.endsWith("DO")) {
                                // 找到PO/DO类所在的目录
                                String packagePath = importPath.substring(0, importPath.lastIndexOf('.'));
                                addProjectFolder(result, moduleName, "src/main/java", "entity", packagePath);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // 忽略错误
            }
        });
    }
    
    /**
     * 扫描msgcode类型的文件
     */
    private void scanMsgCodeDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        File javaDir = new File(moduleDir, "src/main/java");
        if (!javaDir.exists()) {
            return;
        }
        
        findJavaFiles(javaDir).forEach(file -> {
            try {
                String content = Files.readString(file.toPath());
                if (content.contains("implements IMessageCode")) {
                    // 对于msgcode类型，我们需要完整的包路径包括文件名
                    String packagePath = getPackagePath(file, javaDir);
                    // 将文件路径转换为包路径格式（用点替换斜杠，去掉.java扩展名）
                    String fullPackagePath = packagePath.replace(File.separator, ".")
                                                      .replace(".java", "");
                    addProjectFolder(result, moduleName, "src/main/java", "msgcode", fullPackagePath);
                }
            } catch (IOException e) {
                // 忽略错误
            }
        });
    }
    
    /**
     * 扫描prop-path类型的文件
     */
    private void scanPropPathFiles(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
        File resourcesDir = new File(moduleDir, "src/main/resources");
        if (!resourcesDir.exists()) {
            return;
        }
        
        File propFile = new File(resourcesDir, "application.properties");
        if (propFile.exists() && propFile.isFile()) {
            addProjectFolder(result, moduleName, "src/main/resources", "prop-path", "application.properties");
        }
    }
    
                    /**
                 * 扫描xml类型的目录
                 */
                private void scanXmlDirectories(File moduleDir, String moduleName, List<ProjectFolderPO> result) {
                    File resourcesDir = new File(moduleDir, "src/main/resources");
                    if (!resourcesDir.exists()) {
                        return;
                    }
                    List<File> xmlFiles = findXmlFiles(resourcesDir);
                    // 使用Set来避免重复添加相同的目录
                    Set<String> addedPaths = new HashSet<>();

                    xmlFiles.forEach(xmlFile -> {
                        try {
                            // 使用UTF-8编码读取文件内容
                            String content = Files.readString(xmlFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                            // 使用多种方式检查mapper标签
                            boolean hasMapperTag = content.contains("<mapper>") || 
                                                  content.contains("<mapper ") || 
                                                  content.indexOf("<mapper") >= 0;

                            
                            if (hasMapperTag) {
                                String packagePath = getPackagePath(xmlFile.getParentFile(), resourcesDir);
                                if (!addedPaths.contains(packagePath)) {
                                    addProjectFolder(result, moduleName, "src/main/resources", "xml", packagePath);
                                    addedPaths.add(packagePath);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
    
    /**
     * 通用的Java文件扫描方法
     */
    private void scanJavaDirectories(File moduleDir, String moduleName, String dirType, 
                                   List<ProjectFolderPO> result, FilePredicate predicate) {
        File javaDir = new File(moduleDir, "src/main/java");
        if (!javaDir.exists()) {
            return;
        }
        
        findJavaFiles(javaDir).forEach(file -> {
            if (predicate.test(file)) {
                String packagePath = getPackagePath(file.getParentFile(), javaDir);
                addProjectFolder(result, moduleName, "src/main/java", dirType, packagePath);
            }
        });
    }
    
    /**
     * 查找指定名称的目录
     */
    private void findDirectoriesByName(File dir, String targetName, String moduleName, 
                                     String dirType, List<ProjectFolderPO> result, File baseDir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (file.getName().equals(targetName)) {
                        String packagePath = getPackagePath(file, baseDir);
                        addProjectFolder(result, moduleName, "src/main/java", dirType, packagePath);
                    } else {
                        findDirectoriesByName(file, targetName, moduleName, dirType, result, baseDir);
                    }
                }
            }
        }
    }
    
    /**
     * 查找所有Java文件
     */
    private List<File> findJavaFiles(File dir) {
        List<File> javaFiles = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) {
            return javaFiles;
        }
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(findJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
        return javaFiles;
    }
    
    /**
     * 查找所有XML文件
     */
    private List<File> findXmlFiles(File dir) {
        List<File> xmlFiles = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) {
            return xmlFiles;
        }
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    xmlFiles.addAll(findXmlFiles(file));
                } else if (file.getName().endsWith(".xml")) {
                    xmlFiles.add(file);
                }
            }
        }
        return xmlFiles;
    }
    
    /**
     * 查找application.properties文件
     */
    private List<File> findApplicationPropertiesFiles(File dir) {
        List<File> propFiles = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) {
            return propFiles;
        }
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    propFiles.addAll(findApplicationPropertiesFiles(file));
                } else if (file.getName().startsWith("application") && file.getName().endsWith(".properties")) {
                    propFiles.add(file);
                }
            }
        }
        return propFiles;
    }
    
    /**
     * 获取包路径
     */
    private String getPackagePath(File file, File baseDir) {
        try {
            String absolutePath = file.getAbsolutePath();
            String basePath = baseDir.getAbsolutePath();
            
            if (absolutePath.startsWith(basePath)) {
                String relativePath = absolutePath.substring(basePath.length());
                if (relativePath.startsWith(File.separator)) {
                    relativePath = relativePath.substring(1);
                }
                return relativePath.replace(File.separator, ".");
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 添加项目文件夹记录
     */
    private void addProjectFolder(List<ProjectFolderPO> result, String moduleName, 
                                String dirBase, String dirType, String dirPath) {
        ProjectFolderPO po = new ProjectFolderPO();
        po.setGroupName(globalPropes.getGroupName());
        po.setProjectName(globalPropes.getProjectName());
        po.setAppName(globalPropes.getAppName());
        po.setModuleName(moduleName);
        po.setDirBase(dirBase);
        po.setDirType(dirType);
        po.setDirPath(dirPath);
        po.setMainFlg("Y"); // 临时设置为Y，后续会在mergePaths中重新设置
        po.setUpdateBy(globalPropes.getOperator().getUsername());
        po.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.add(po);
    }
    
    /**
     * 合并相同模块和类型的路径，并设置主次关系
     */
    private List<ProjectFolderPO> mergePaths(List<ProjectFolderPO> folders) {
        // 1. 先按模块和类型合并路径
        Map<String, ProjectFolderPO> mergedMap = new HashMap<>();
        
        for (ProjectFolderPO folder : folders) {
            String key = folder.getModuleName() + ":" + folder.getDirType();
            ProjectFolderPO existing = mergedMap.get(key);
            
            if (existing == null) {
                mergedMap.put(key, folder);
            } else {
                // 合并路径，找到公共前缀
                String mergedPath = mergePackagePaths(existing.getDirPath(), folder.getDirPath());
                existing.setDirPath(mergedPath);
            }
        }
        
        List<ProjectFolderPO> mergedFolders = new ArrayList<>(mergedMap.values());
        
        // 2. 按dir_type分组，设置主次关系
        Map<String, List<ProjectFolderPO>> typeGroups = new HashMap<>();
        
        for (ProjectFolderPO folder : mergedFolders) {
            String dirType = folder.getDirType();
            typeGroups.computeIfAbsent(dirType, k -> new ArrayList<>()).add(folder);
        }
        
        // 3. 为每个类型设置主次关系
        List<ProjectFolderPO> result = new ArrayList<>();
        for (List<ProjectFolderPO> typeGroup : typeGroups.values()) {
            for (int i = 0; i < typeGroup.size(); i++) {
                ProjectFolderPO folder = typeGroup.get(i);
                // 第一个设置为Y（主），其他设置为N（次）
                folder.setMainFlg(i == 0 ? "Y" : "N");
                result.add(folder);
            }
        }
        
        return result;
    }
    
    /**
     * 合并包路径，找到公共前缀
     */
    private String mergePackagePaths(String path1, String path2) {
        if (path1 == null || path1.isEmpty()) return path2;
        if (path2 == null || path2.isEmpty()) return path1;
        
        String[] parts1 = path1.split("\\.");
        String[] parts2 = path2.split("\\.");
        
        StringBuilder commonPath = new StringBuilder();
        int minLength = Math.min(parts1.length, parts2.length);
        
        for (int i = 0; i < minLength; i++) {
            if (parts1[i].equals(parts2[i])) {
                if (commonPath.length() > 0) {
                    commonPath.append(".");
                }
                commonPath.append(parts1[i]);
            } else {
                break;
            }
        }
        
        return commonPath.toString();
    }
    
    /**
     * 文件谓词接口
     */
    @FunctionalInterface
    private interface FilePredicate {
        boolean test(File file);
    }
}
