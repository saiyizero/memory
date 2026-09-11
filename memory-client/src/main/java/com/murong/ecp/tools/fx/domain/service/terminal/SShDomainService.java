package com.murong.ecp.tools.fx.domain.service.terminal;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.ServiceInfoEntity;
import com.murong.ecp.tools.fx.infrastructure.rpc.ServerInfoRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ServerInfoPO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SShDomainService {
    @Autowired
    ServerInfoRpcService serverInfoRpcService;
    @Autowired
    GlobalProperties globalPropes;

    public List<ServiceInfoEntity> getServiceList(String groupName){
        ServerInfoPO query = new ServerInfoPO();
        query.setScanFlg(1);
        query.setGroupName(groupName);
        List<ServerInfoPO> poList = serverInfoRpcService.queryForList(query);
        // 按 groupName+ip 分组
        Map<String, ServiceInfoEntity> groupMap = new HashMap<>();
        for (ServerInfoPO po : poList) {
            String key = po.getGroupName() + "@" + po.getIp();
            ServiceInfoEntity entity = groupMap.get(key);
            if (entity == null) {
                entity = new ServiceInfoEntity();
                entity.setGroupName(po.getGroupName());
                entity.setEnvName(po.getEnvName());
                entity.setIp(po.getIp());
                entity.setUsername(po.getUsername());
                entity.setPassword(po.getPassword());
                entity.setPort(po.getPort());
                entity.setUpdateBy(po.getUpdateBy());
                entity.setUpdateTime(po.getUpdateTime());
                entity.setMicroServiceList(new ArrayList<>());
                groupMap.put(key, entity);
            }
            ServiceInfoEntity.MicroServiceInfo ms = new ServiceInfoEntity.MicroServiceInfo();
            ms.setProjectName(po.getProjectName());
            ms.setAppName(po.getAppName());
            ms.setAppPort(po.getAppPort());
            ms.setAppPath(po.getAppPath());
            ms.setAppProp(po.getAppProp());
            entity.getMicroServiceList().add(ms);
        }
        return new ArrayList<>(groupMap.values());
    }

    // 进度回调接口
    public interface ProgressCallback {
        void onProgress(double progress);
    }

    // 日志文件搜索
    public List<Map<String, String>> searchLogFiles(List<ServerInfoPO> serverList, String keyword, String searchType, int dayCount, ProgressCallback callback) {
        Map<String, List<ServerInfoPO>> ipMap = new HashMap<>();
        for (ServerInfoPO server : serverList) {
            ipMap.computeIfAbsent(server.getIp(), k -> new ArrayList<>()).add(server);
        }
        List<Map<String, String>> fileLst = new ArrayList<>();
        // 统计总任务数
        int totalTask = 0;
        for (int i = dayCount; i > 0; i--) {
            for (List<ServerInfoPO> ipServers : ipMap.values()) {
                totalTask += ipServers.size();
            }
        }
        int finished = 0;
        for (int i = dayCount; i > 0; i--) {
            String day = java.time.LocalDate.now().minusDays(i).plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")).substring(6,8);
            if ("按文件名搜索".equals(searchType)) {
                for (Map.Entry<String, List<ServerInfoPO>> entry : ipMap.entrySet()) {
                    String ip = entry.getKey();
                    List<ServerInfoPO> ipServers = entry.getValue();
                    if (ipServers.isEmpty()) continue;
                    ServerInfoPO first = ipServers.get(0);
                    try {
                        System.out.println("[扫描] 开始连接服务器: " + ip + " 用户: " + first.getUsername());
                        net.schmizz.sshj.SSHClient ssh = new net.schmizz.sshj.SSHClient();
                        ssh.addHostKeyVerifier(new net.schmizz.sshj.transport.verification.PromiscuousVerifier());
                        ssh.connect(ip, Integer.parseInt(first.getPort()));
                        ssh.authPassword(first.getUsername(), first.getPassword());
                        net.schmizz.sshj.sftp.SFTPClient sftp = ssh.newSFTPClient();
                        for (ServerInfoPO serverInfo : ipServers) {
                            String appPath = serverInfo.getAppPath() + java.io.File.separator + day;
                            System.out.println("[扫描] 扫描目录: " + appPath);
                            try (net.schmizz.sshj.connection.channel.direct.Session session = ssh.startSession()) {
                                String cmd = "cd '" + appPath + "' && ls | grep '" + keyword + "' 2>/dev/null";
                                System.out.println("[扫描] 执行命令: " + cmd);
                                net.schmizz.sshj.connection.channel.direct.Session.Command cmdExec = session.exec(cmd);
                                String result = new String(cmdExec.getInputStream().readAllBytes());
                                cmdExec.close();
                                if (!result.isEmpty()) {
                                    String[] files = result.split("\\r?\\n");
                                    for (String fname : files) {
                                        if (fname.trim().isEmpty()) continue;
                                        String absPath = appPath + java.io.File.separator + fname.trim();
                                        long mtime = 0;
                                        try { mtime = sftp.stat(absPath).getMtime(); } catch (Exception ignore) {}
                                        Map<String, String> strMap = new HashMap<>();
                                        strMap.put("ip", ip);
                                        strMap.put("path", absPath);
                                        strMap.put("filename", fname.trim());
                                        strMap.put("appName", serverInfo.getAppName() == null ? "" : serverInfo.getAppName());
                                        strMap.put("mtime", String.valueOf(mtime));
                                        fileLst.add(strMap);
                                    }
                                }
                            } catch (Exception ex) {
                                System.out.println("[扫描] 目录扫描异常: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                            finished++;
                            if (callback != null && totalTask > 0) {
                                callback.onProgress((double) finished / totalTask);
                            }
                        }
                        sftp.close();
                        ssh.disconnect();
                    } catch (Exception ex) {
                        System.out.println("[扫描] 服务器连接异常: " + ex.getMessage());
                        finished += ipServers.size();
                        if (callback != null && totalTask > 0) {
                            callback.onProgress((double) finished / totalTask);
                        }
                        ex.printStackTrace();
                    }
                }
            } else {
                for (Map.Entry<String, List<ServerInfoPO>> entry : ipMap.entrySet()) {
                    String ip = entry.getKey();
                    List<ServerInfoPO> ipServers = entry.getValue();
                    if (ipServers.isEmpty()) continue;
                    ServerInfoPO first = ipServers.get(0);
                    try {
                        System.out.println("[扫描] 开始连接服务器: " + ip + " 用户: " + first.getUsername());
                        net.schmizz.sshj.SSHClient ssh = new net.schmizz.sshj.SSHClient();
                        ssh.addHostKeyVerifier(new net.schmizz.sshj.transport.verification.PromiscuousVerifier());
                        ssh.connect(ip, Integer.parseInt(first.getPort()));
                        ssh.authPassword(first.getUsername(), first.getPassword());
                        net.schmizz.sshj.sftp.SFTPClient sftp = ssh.newSFTPClient();
                        for (ServerInfoPO serverInfo : ipServers) {
                            String appPath = serverInfo.getAppPath() + java.io.File.separator + day;
                            System.out.println("[扫描] 扫描目录: " + appPath);
                            try (net.schmizz.sshj.connection.channel.direct.Session session = ssh.startSession()) {
                                String cmd = "cd '" + appPath + "' && grep -l '" + keyword + "' * 2>/dev/null";
                                System.out.println("[扫描] 执行命令: " + cmd);
                                net.schmizz.sshj.connection.channel.direct.Session.Command cmdExec = session.exec(cmd);
                                String result = new String(cmdExec.getInputStream().readAllBytes());
                                cmdExec.close();
                                if (!result.isEmpty()) {
                                    String[] files = result.split("\\r?\\n");
                                    for (String fname : files) {
                                        if (fname.trim().isEmpty()) continue;
                                        String absPath = appPath + java.io.File.separator + fname.trim();
                                        long mtime = 0;
                                        try { mtime = sftp.stat(absPath).getMtime(); } catch (Exception ignore) {}
                                        Map<String, String> strMap = new HashMap<>();
                                        strMap.put("ip", ip);
                                        strMap.put("path", absPath);
                                        strMap.put("filename", fname.trim());
                                        strMap.put("appName", serverInfo.getAppName() == null ? "" : serverInfo.getAppName());
                                        strMap.put("mtime", String.valueOf(mtime));
                                        fileLst.add(strMap);
                                    }
                                }
                            } catch (Exception ex) {
                                System.out.println("[扫描] 目录扫描异常: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                            finished++;
                            if (callback != null && totalTask > 0) {
                                callback.onProgress((double) finished / totalTask);
                            }
                        }
                        sftp.close();
                        ssh.disconnect();
                    } catch (Exception ex) {
                        System.out.println("[扫描] 服务器连接异常: " + ex.getMessage());
                        finished += ipServers.size();
                        if (callback != null && totalTask > 0) {
                            callback.onProgress((double) finished / totalTask);
                        }
                        ex.printStackTrace();
                    }
                }
            }
        }
        return fileLst;
    }

    // 服务器同步
    public void syncServer(String ip, String port, String user, String pwd, String scanPath, String groupName) {
        try {
            net.schmizz.sshj.SSHClient ssh = new net.schmizz.sshj.SSHClient();
            ssh.addHostKeyVerifier(new net.schmizz.sshj.transport.verification.PromiscuousVerifier());
            ssh.connect(ip, Integer.parseInt(port));
            ssh.authPassword(user, pwd);
            net.schmizz.sshj.sftp.SFTPClient sftp = ssh.newSFTPClient();
            for (net.schmizz.sshj.sftp.RemoteResourceInfo info : sftp.ls(scanPath)) {
                String baseDir = info.getName();
                String appsPath = scanPath + (scanPath.endsWith("/") ? "" : "/") + baseDir + "/apps";
                try {
                    net.schmizz.sshj.sftp.FileAttributes attrs = sftp.statExistence(appsPath);
                    if (attrs != null && attrs.getType() == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY) {
                        for (net.schmizz.sshj.sftp.RemoteResourceInfo warFile : sftp.ls(appsPath)) {
                            String fileName = warFile.getName();
                            if (fileName.endsWith(".war")) {
                                String microName = fileName.substring(0, fileName.length() - 4);
                                String appNme = baseDir.substring(baseDir.length() - 3);
                                ServerInfoPO whereInfo = new ServerInfoPO();
                                whereInfo.setIp(ip);
                                whereInfo.setProjectName(microName);
                                whereInfo.setGroupName(groupName);
                                whereInfo.setAppName(appNme);
                                ServerInfoPO serverInfoRsp = serverInfoRpcService.queryOne(whereInfo);

                                ServerInfoPO serverInfo = new ServerInfoPO();
                                String logPath = scanPath + (scanPath.endsWith("/") ? "" : "/") + baseDir + java.io.File.separator + microName + java.io.File.separator + "trc";
                                String propPath = scanPath + (scanPath.endsWith("/") ? "" : "/") + baseDir + java.io.File.separator + "conf" + java.io.File.separator + appNme + java.io.File.separator + "application.properties";
                                serverInfo.setGroupName(groupName);
                                serverInfo.setAppPath(logPath);
                                serverInfo.setAppProp(propPath);
                                serverInfo.setAppName(appNme);
                                serverInfo.setPort(port);
                                serverInfo.setUsername(user);
                                serverInfo.setPassword(pwd);

                                try {
                                    java.io.File tempFile = java.io.File.createTempFile("application", ".properties");
                                    String localTempPath = tempFile.getAbsolutePath();
                                    sftp.get(propPath, localTempPath);
                                    java.util.Properties prop = new java.util.Properties();
                                    try (java.io.InputStream in = new java.io.FileInputStream(localTempPath)) {
                                        prop.load(in);
                                        String appPort = prop.getProperty("server.port");
                                        if (appPort != null) {
                                            serverInfo.setAppPort(appPort);
                                        }
                                    }
                                    tempFile.delete();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }

                                if (serverInfoRsp != null) {
                                    serverInfoRpcService.updateByOne(serverInfo, whereInfo);
                                } else {
                                    serverInfo.setIp(ip);
                                    serverInfo.setProjectName(microName);
                                    serverInfoRpcService.save(serverInfo);
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {
                }
            }
            sftp.close();
            ssh.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("SSH登录失败: " + ex.getMessage());
        }
    }

    // 读取远程文件内容
    public String readRemoteFile(String ip, String port, String user, String pwd, String remotePath) {
        try {
            net.schmizz.sshj.SSHClient ssh = new net.schmizz.sshj.SSHClient();
            ssh.addHostKeyVerifier(new net.schmizz.sshj.transport.verification.PromiscuousVerifier());
            ssh.connect(ip, Integer.parseInt(port));
            ssh.authPassword(user, pwd);
            net.schmizz.sshj.sftp.SFTPClient sftp = ssh.newSFTPClient();
            java.io.File tempFile = java.io.File.createTempFile("remote", ".tmp");
            String localTempPath = tempFile.getAbsolutePath();
            sftp.get(remotePath, localTempPath);
            String content = java.nio.file.Files.readString(java.nio.file.Paths.get(localTempPath));
            tempFile.delete();
            sftp.close();
            ssh.disconnect();
            return content;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("读取远程文件失败: " + ex.getMessage());
        }
    }

    // 下载远程文件
    public void downloadRemoteFile(String ip, String port, String user, String pwd, String remotePath, String localPath) {
        try {
            net.schmizz.sshj.SSHClient ssh = new net.schmizz.sshj.SSHClient();
            ssh.addHostKeyVerifier(new net.schmizz.sshj.transport.verification.PromiscuousVerifier());
            ssh.connect(ip, Integer.parseInt(port));
            ssh.authPassword(user, pwd);
            net.schmizz.sshj.sftp.SFTPClient sftp = ssh.newSFTPClient();
            sftp.get(remotePath, localPath);
            sftp.close();
            ssh.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("下载远程文件失败: " + ex.getMessage());
        }
    }

    //获取该日志将该日志存入指定目录（SFTP方式）
    public String ftpGetLogTrc(String ip, String fileName, String localPath) {
        ServerInfoPO po = new ServerInfoPO();
        po.setIp(ip);
        po.setProjectName(globalPropes.getProjectName());
        ServerInfoPO serverInfo = serverInfoRpcService.queryOne(po);
        String remotePath = serverInfo.getAppPath() + "/"+ MrDateUtils.getCurrentDay() + "/";
        String username = serverInfo.getUsername();
        String password = serverInfo.getPassword();
        SSHClient ssh = new SSHClient();
        try {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(ip, 22);
            ssh.authPassword(username, password);
            SFTPClient sftp = ssh.newSFTPClient();
            sftp.get(remotePath + fileName, localPath);
            sftp.close();
            ssh.disconnect();
            return localPath;
        } catch (Exception e) {
            try { ssh.disconnect(); } catch (Exception ignore) {}
            throw new RuntimeException("SFTP下载日志文件异常: " + e.getMessage(), e);
        }
    }
}
