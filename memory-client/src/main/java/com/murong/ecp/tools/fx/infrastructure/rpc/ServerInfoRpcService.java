package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ServerInfoPO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 服务器信息 HTTP 门面，SQL 在 memory-service 执行。
 */
@Service
public class ServerInfoRpcService extends HttpDaoSupport<ServerInfoPO> {
    public void save(ServerInfoPO po) {
        super.insert(po);
    }

    public List<ServerInfoPO> queryForList(ServerInfoPO po) {
        return super.queryForList(po);
    }

    public void updateScanFlg(String groupName, String appName, String scanFlg) {
        invokeVoid("updateScanFlg", groupName, appName, scanFlg);
    }
}
