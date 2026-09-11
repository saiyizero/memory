package com.murong.ecp.tools.fx.infrastructure.cache;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.BizDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * BizDictPO全局内存集合缓存服务
 * 用于管理业务字典数据的全局内存缓存，提高查询性能
 */
@Component
public class BizDictCache {
    
    @Autowired
    private BizDictDao bizDictDao;
    
    @Autowired
    private GlobalProperties globalProperties;
    
    // 全局内存集合，用于缓存BizDictPO数据
    private final ConcurrentMap<String, BizDictPO> globalBizDictCache = new ConcurrentHashMap<>();
    
    /**
     * 检查字段是否存在于全局内存集合中
     * @param po BizDictPO对象
     * @return 如果存在返回true，否则返回false
     */
    public boolean existsInPublicBizDict(BizDictPO po) {
        if (po == null) return false;
        if(globalBizDictCache.size()<=0){
            initializeGlobalCache();
        }

        String cacheKey = generateCacheKey(po);
        return globalBizDictCache.containsKey(cacheKey);
    }

    public BizDictPO getPublicBizDict(BizDictPO po){
        if (po == null) return null;
        String cacheKey = generateCacheKey(po);
        return globalBizDictCache.get(cacheKey);
    }
    
    /**
     * 初始化全局内存集合（从数据库加载数据）
     */
    public void initializeGlobalCache() {
        BizDictPO queryPO = new BizDictPO();
        queryPO.setAppName("pub");
        queryPO.setGroupName(globalProperties.getGroupName());
        List<BizDictPO> allBizDicts = bizDictDao.queryForList(queryPO);
        
        // 清空现有缓存
        globalBizDictCache.clear();
        
        // 将数据库中的数据加载到缓存中
        for (BizDictPO po : allBizDicts) {
            addToGlobalCache(po);
        }
    }

    // 缓存键生成方法
    private String generateCacheKey(BizDictPO po) {
        if (po == null) return null;
        return String.format("%s:%s",
                po.getGroupName(),
                po.getNameCamel());
    }

    /**
     * 将BizDictPO添加到全局内存集合
     * @param po BizDictPO对象
     */
    private void addToGlobalCache(BizDictPO po) {
        if (po == null) return;
        String cacheKey = generateCacheKey(po);
        globalBizDictCache.put(cacheKey, po);
    }
}
