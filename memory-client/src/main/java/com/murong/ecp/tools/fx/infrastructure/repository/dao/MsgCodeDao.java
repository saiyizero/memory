package com.murong.ecp.tools.fx.infrastructure.repository.dao;

import com.murong.ecp.tools.fx.infrastructure.repository.HttpDaoSupport;
import com.murong.ecp.tools.fx.infrastructure.repository.po.MsgCodePO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MsgCodeDao extends HttpDaoSupport<MsgCodePO> {

    public void save(MsgCodePO po) {
        super.insert(po);
    }

    public List<MsgCodePO> queryForList(MsgCodePO po) {
        return super.queryForList(po,"update_time desc");
    }
}