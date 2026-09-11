package com.murong.ecp.tools.fx.infrastructure.rpc;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommonClassRpcService extends HttpDaoSupport<CommonClassPO> {

    public CommonClassPO queryByClassName(String className){
        CommonClassPO po = new CommonClassPO();
        po.setClassName(className);
        return queryOne(po);
    }

    public CommonClassPO queryByClassPath(String classPath){
        CommonClassPO po = new CommonClassPO();
        po.setClassPath(classPath);
        return queryOne(po);
    }

    public void save(CommonClassPO po) {
        super.insert(po);
    }

    public List<CommonClassPO> queryForList (CommonClassPO po) {
        return super.queryForList(po);
    }
}
