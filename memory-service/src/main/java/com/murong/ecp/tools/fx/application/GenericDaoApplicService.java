package com.murong.ecp.tools.fx.application;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.infrastructure.repository.GenericJdbcDao;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoRequest;
import com.murong.ecp.tools.fx.infrastructure.rpc.RpcDaoResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@RestController
@RequestMapping("/api/dao")
public class GenericDaoApplicService {

    private static final String DAO_PACKAGE = "com.murong.ecp.tools.fx.infrastructure.repository.dao.";

    private final GenericJdbcDao genericJdbcDao;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    public GenericDaoApplicService(GenericJdbcDao genericJdbcDao, ObjectMapper objectMapper, ApplicationContext applicationContext) {
        this.genericJdbcDao = genericJdbcDao;
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
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

    @PostMapping("/invoke")
    public RpcDaoResponse invoke(@RequestBody RpcDaoRequest request) {
        try {
            String daoType = request.getDaoType();
            if (StringUtils.isBlank(daoType) || !daoType.startsWith(DAO_PACKAGE) || daoType.contains("/")) {
                return RpcDaoResponse.fail("非法 DAO 类型");
            }
            Class<?> daoClass = Class.forName(daoType);
            Object daoBean = applicationContext.getBean(daoClass);
            JsonNode argsNode = request.getMethodArgs();
            int argCount = (argsNode == null || argsNode.isNull() || !argsNode.isArray()) ? 0 : argsNode.size();
            // 必须从原始 DAO 类取方法，CGLIB 代理会丢掉 List<RoleMenuPO> 这类泛型，导致反序列化成 LinkedHashMap
            Method method = findMethod(daoClass, request.getMethodName(), argCount);
            method.setAccessible(true);
            Object result = method.invoke(daoBean, convertArgs(method, argsNode));
            return RpcDaoResponse.ok(objectMapper.valueToTree(result));
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return RpcDaoResponse.fail("调用 DAO 失败: " + cause.getMessage());
        } catch (Exception e) {
            return RpcDaoResponse.fail("调用 DAO 失败: " + e.getMessage());
        }
    }

    private Method findMethod(Class<?> type, String methodName, int argCount) {
        for (Method method : type.getMethods()) {
            if (method.isBridge() || method.isSynthetic()) {
                continue;
            }
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        throw new RuntimeException("找不到方法: " + type.getName() + "." + methodName + "(" + argCount + ")");
    }

    private Object[] convertArgs(Method method, JsonNode argsNode) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        if (parameters.length == 0 || argsNode == null || !argsNode.isArray()) {
            return args;
        }
        for (int i = 0; i < parameters.length; i++) {
            JavaType javaType = objectMapper.getTypeFactory().constructType(parameters[i].getParameterizedType());
            args[i] = objectMapper.convertValue(argsNode.get(i), javaType);
        }
        return args;
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
