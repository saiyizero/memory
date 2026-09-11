package com.murong.ecp.tools.fx.infrastructure.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MrSpringContextHolder implements ApplicationContextAware, DisposableBean {
    private static ApplicationContext applicationContext = null;
    private static Cache<String, String> propertyCache = CacheBuilder.newBuilder().maximumSize(200L).build();
    private static Cache<String, String> nullPropertyCache = CacheBuilder.newBuilder().maximumSize(100L).build();

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void registerBean(String name, Object bean) {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory)((ConfigurableApplicationContext)applicationContext).getBeanFactory();
        defaultListableBeanFactory.registerSingleton(name, bean);
    }

    public static void removeBean(String name) {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory)((ConfigurableApplicationContext)applicationContext).getBeanFactory();
        defaultListableBeanFactory.destroySingleton(name);
    }

    public void setApplicationContext(ApplicationContext appContext) {
        applicationContext = appContext;
    }

    public static <T> T getBean(String name) {
        return (T)applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> requiredType) {
        return (T)applicationContext.getBean(requiredType);
    }

    public static <T> T getBean(String name, Class<T> requiredType) {
        return (T)applicationContext.getBean(name, requiredType);
    }

    public static void clearHolder() {
        applicationContext = null;
    }

    public static String getProperty(String name) {
        if (getApplicationContext() == null || getApplicationContext().getEnvironment() == null) {
            return null;
        }
        // 先查null缓存
        String nullCache = nullPropertyCache.getIfPresent(name);
        if (nullCache != null) {
            return null;
        }
        // 再查正常缓存
        String value = propertyCache.getIfPresent(name);
        if (value != null) {
            return value;
        }
        // 读取环境变量
        value = getApplicationContext().getEnvironment().getProperty(name);
        if (value != null) {
            propertyCache.put(name, value);
        } else {
            nullPropertyCache.put(name, "");
        }
        return value;
    }

    public static String getProperty(String name, String defaultValue) {
        if (getApplicationContext() == null) {
            return null;
        } else if (getApplicationContext().getEnvironment() == null) {
            return null;
        } else {
            String value = (String)propertyCache.getIfPresent(name);
            if (value != null) {
                return value;
            } else {
                value = getApplicationContext().getEnvironment().getProperty(name, defaultValue);
                if (value != null) {
                    propertyCache.put(name, value);
                }

                return value;
            }
        }
    }

    public static long getIntProperty(String name) {
        return NumberUtils.toLong(getProperty(name));
    }

    public static long getIntProperty(String name, long defaultValue) {
        return NumberUtils.toLong(getProperty(name, String.valueOf(defaultValue)));
    }

    public static boolean getBooleanProperty(String name) {
        return BooleanUtils.toBoolean(getProperty(name));
    }

    public static boolean getBooleanProperty(String name, boolean defaultValue) {
        return BooleanUtils.toBoolean(getProperty(name, String.valueOf(defaultValue)));
    }

    public static String getApplicationName() {
        return getProperty("spring.application.name");
    }

    public void destroy() throws Exception {
        clearHolder();
    }
}

