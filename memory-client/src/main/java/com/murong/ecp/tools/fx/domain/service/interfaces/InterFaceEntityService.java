package com.murong.ecp.tools.fx.domain.service.interfaces;

import com.murong.ecp.tools.fx.domain.entity.InterFaceEntity;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.infrastructure.converter.InterfaceConvert;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.rpc.EnumDictRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.InterfaceDataRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.InterfaceDataHisRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataHisPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterFaceEntityService {
    @Autowired
    InterfaceDataRpcService interfaceDataRpcService;
    @Autowired
    InterfaceDataHisRpcService interfaceDataHisRpcService;
    @Autowired
    EnumDictRpcService enumDictRpcService;
    @Autowired
    GlobalProperties globalProps;
    @Autowired
    JavaCodeService javaCodeService;

    public CrResult deleteByEnums(String appName) {
        enumDictRpcService.batchBackUp(globalProps.getGroupName(), appName);
        EnumDictPO po = new EnumDictPO();
        po.setAppName(appName);
        po.setGroupName(globalProps.getGroupName());
        enumDictRpcService.batchDelete(po);
        return CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
    }

    public CrResult deleteByAppName(String appName) {
        interfaceDataRpcService.batchBackUp(globalProps.getGroupName(), appName);
        InterfaceDataPO po = new InterfaceDataPO();
        po.setAppName(appName);
        po.setGroupName(globalProps.getGroupName());
        interfaceDataRpcService.batchDelete(po);
        return CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
    }

    public CrResult saveInterFaceEntity(InterFaceEntity interFaceEntity) {
        try {
            InterfaceDataPO po = InterfaceConvert.toPO(interFaceEntity);
            po.setAppName(globalProps.getAppName());
            po.setProjectName(globalProps.getProjectName());
            po.setGroupName(globalProps.getGroupName());

            InterfaceDataHisPO hisPO = new InterfaceDataHisPO();
            hisPO.setAppName(po.getAppName());
            hisPO.setProjectName(po.getProjectName());
            hisPO.setGroupName(po.getGroupName());
            hisPO.setClassName(po.getClassName());
            hisPO.setTransName(po.getTransName());

            InterfaceDataHisPO interfaceDataHisPO = interfaceDataHisRpcService.queryInfcDataHis(hisPO);
            if(interfaceDataHisPO != null && (StringUtils.isNotBlank(interfaceDataHisPO.getTransCommentEn())
                    ||StringUtils.isNotBlank(interfaceDataHisPO.getTransCommentZh()))) {
                po.setTransCommentEn(interfaceDataHisPO.getTransCommentEn());
                po.setTransCommentZh(interfaceDataHisPO.getTransCommentZh());
            }

            interfaceDataRpcService.save(po);
            return CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        } catch (Exception e) {
            CrResult crResult = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            crResult.setMsgInf(e.getMessage());
            return crResult;
        }
    }

    public List<String> getApiNameList() {
        String sql = "select trans_name from interface_data where app_name='" + globalProps.getAppName() + "' and group_name='" + globalProps.getGroupName() + "'";
        return interfaceDataRpcService.queryListBySql(sql, String.class);
    }

    public InterFaceEntity getByTransName(String transName,String interfaceName) {
        InterfaceDataPO po = new InterfaceDataPO();
        po.setTransName(transName);
        po.setInterfaceName(interfaceName);
        po.setProjectName(globalProps.getProjectName());
        po.setGroupName(globalProps.getGroupName());
        InterfaceDataPO result = interfaceDataRpcService.queryOne(po);
        return InterfaceConvert.toEntity(result);
    }

    /**
     * 同步接口信息（通用方法，支持本地目录和JAR包）
     * @param sourcePath 源码根目录或JAR文件路径
     * @return 处理结果
     */
    public CrResult syncInterfaces(String sourcePath) {
        try {
            // 1. 备份数据
            deleteByAppName(globalProps.getAppName());
            deleteByEnums(globalProps.getAppName());

            // 2. 调用 JavaCodeService 解析接口（复用通用方法）
            List<InterFaceEntity> entityList = javaCodeService.javaEntityConvInterFace(sourcePath);
            
            // 3. 登记接口对应父类（复用通用方法）
            javaCodeService.registerParentClass(entityList, sourcePath);

            // 4. 存储到数据库
            int successCount = 0;
            if (entityList != null && !entityList.isEmpty()) {
                for (InterFaceEntity entity : entityList) {
                    saveInterFaceEntity(entity);
                    successCount++;
                }
            }
            
            CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
            // 根据数据源类型返回不同的消息
            if (sourcePath.toLowerCase().endsWith(".jar")) {
                result.setMsgInf("导入JAR包接口完成，数量：" + successCount);
            } else {
                result.setMsgInf("同步接口完成，数量：" + successCount);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            CrResult result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            if (sourcePath.toLowerCase().endsWith(".jar")) {
                result.setMsgInf("导入JAR包接口失败: " + e.getMessage());
            } else {
                result.setMsgInf("同步接口失败: " + e.getMessage());
            }
            return result;
        }
    }

    /**
     * 导入JAR包中的接口信息（兼容方法）
     * @param jarFilePath JAR文件路径
     * @return 处理结果
     */
    public CrResult importJarInterfaces(String jarFilePath) {
        return syncInterfaces(jarFilePath);
    }
}
