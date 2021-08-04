package com.fool.redis;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.util.ReflectionUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TtlRedisCacheManager extends RedisCacheManager implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private final Map<String, RedisCacheConfiguration> initialCacheConfiguration = new LinkedHashMap<>();

    /**
     * key serializer pair
     */
    final RedisSerializationContext.SerializationPair<String> keySerializationPair;
    /**
     * value serializer pair
     */
    final RedisSerializationContext.SerializationPair<Object> valueSerializationPair;


    public TtlRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration, RedisSerializationContext.SerializationPair<String> keySerializationPair, RedisSerializationContext.SerializationPair<Object> valueSerializationPair) {
        super(cacheWriter, defaultCacheConfiguration);
        this.keySerializationPair = keySerializationPair;
        this.valueSerializationPair = valueSerializationPair;
    }

    @Override
    protected Collection<RedisCache> loadCaches() {
        return initialCacheConfiguration.entrySet().stream()
                .map(entry -> super.createRedisCache(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public void afterPropertiesSet() {
        Stream.of(applicationContext.getBeanNamesForType(Object.class))
                .forEach(beanName -> add(applicationContext.getType(beanName)));
        super.afterPropertiesSet();
    }

    /**
     * 查询所有@CacheExpire的方法 获取过期时间
     *
     * @param clazz 类
     */
    private void add(final Class<?> clazz) {
        ReflectionUtils.doWithMethods(clazz, method -> {
            ReflectionUtils.makeAccessible(method);

            Cacheable cacheable = AnnotationUtils.findAnnotation(method, Cacheable.class);
            if (cacheable == null) {
                return;
            }
            CacheExpire cacheExpire = AnnotationUtils.findAnnotation(method, CacheExpire.class);
            add(cacheable.cacheNames(), cacheExpire);

        }, method -> null != AnnotationUtils.findAnnotation(method, CacheExpire.class));
    }

    private void add(String[] cacheNames, CacheExpire cacheExpire) {
        for (String cacheName : cacheNames) {
            if (cacheName == null || cacheName.isEmpty()) {
                continue;
            }

            if (cacheExpire == null || cacheExpire.expire() <= 0) {
                continue;
            }

            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMillis(cacheExpire.expire()))
                    .disableCachingNullValues()
                    // .prefixKeysWith(cacheName)
                    .serializeKeysWith(keySerializationPair)
                    .serializeValuesWith(valueSerializationPair);

            initialCacheConfiguration.put(cacheName, config);
        }
    }
}
