package com.murong.ecp.tools.fx.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.infrastructure.repository.GenericJdbcDao;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoRequest;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dao")
public class GenericDaoController {

    private final GenericJdbcDao genericJdbcDao;
    private final ObjectMapper objectMapper;

    public GenericDaoController(GenericJdbcDao genericJdbcDao, ObjectMapper objectMapper) {
        this.genericJdbcDao = genericJdbcDao;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/insert")
    public RpcDaoResponse insert(@RequestBody RpcDaoRequest request) {
        genericJdbcDao.insert(toEntity(request));
        return RpcDaoResponse.ok(null);
    }

    @PostMapping("/delete")
    public RpcDaoResponse delete(@RequestBody RpcDaoRequest request) {
        genericJdbcDao.delete(toEntity(request));
        return RpcDaoResponse.ok(null);
    }

    @PostMapping("/update")
    public RpcDaoResponse update(@RequestBody RpcDaoRequest request) {
        genericJdbcDao.updateByOne(toEntity(request), toWhereEntity(request));
        return RpcDaoResponse.ok(null);
    }

    @PostMapping("/queryOne")
    public RpcDaoResponse queryOne(@RequestBody RpcDaoRequest request) {
        Object result = genericJdbcDao.queryOne(toEntity(request));
        return RpcDaoResponse.ok(objectMapper.valueToTree(result));
    }

    @PostMapping("/queryForList")
    public RpcDaoResponse queryForList(@RequestBody RpcDaoRequest request) {
        Object result = genericJdbcDao.queryForList(toEntity(request), request.getOrderBy());
        return RpcDaoResponse.ok(objectMapper.valueToTree(result));
    }

    @PostMapping("/updateBySql")
    public RpcDaoResponse updateBySql(@RequestBody RpcDaoRequest request) {
        Object[] params = toParams(request);
        int count = genericJdbcDao.updateBySql(request.getSql(), params);
        return RpcDaoResponse.ok(objectMapper.valueToTree(count));
    }

    @PostMapping("/queryOneBySql")
    public RpcDaoResponse queryOneBySql(@RequestBody RpcDaoRequest request) {
        Class<?> resultType = resolveClass(StringUtils.defaultIfBlank(request.getResultType(), request.getEntityType()));
        Object result = genericJdbcDao.queryOneBySql(request.getSql(), resultType, toParams(request));
        return RpcDaoResponse.ok(objectMapper.valueToTree(result));
    }

    @PostMapping("/queryListBySql")
    public RpcDaoResponse queryListBySql(@RequestBody RpcDaoRequest request) {
        Class<?> resultType = resolveClass(StringUtils.defaultIfBlank(request.getResultType(), request.getEntityType()));
        Object result = genericJdbcDao.queryListBySql(request.getSql(), resultType, toParams(request));
        return RpcDaoResponse.ok(objectMapper.valueToTree(result));
    }

    @PostMapping("/queryCount")
    public RpcDaoResponse queryCount(@RequestBody RpcDaoRequest request) {
        Integer count = genericJdbcDao.queryCount(request.getSql(), toParams(request));
        return RpcDaoResponse.ok(objectMapper.valueToTree(count));
    }

    @PostMapping("/queryForMaps")
    public RpcDaoResponse queryForMaps(@RequestBody RpcDaoRequest request) {
        Object result = genericJdbcDao.queryForMaps(request.getSql(), toParams(request));
        return RpcDaoResponse.ok(objectMapper.valueToTree(result));
    }

    private Object toEntity(RpcDaoRequest request) {
        return objectMapper.convertValue(request.getEntity(), resolveClass(request.getEntityType()));
    }

    private Object toWhereEntity(RpcDaoRequest request) {
        return objectMapper.convertValue(request.getWhereEntity(), resolveClass(request.getEntityType()));
    }

    private Object[] toParams(RpcDaoRequest request) {
        if (request.getParams() == null) {
            return new Object[0];
        }
        return request.getParams().toArray();
    }

    private Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("无法加载类型: " + className, e);
        }
    }
}
