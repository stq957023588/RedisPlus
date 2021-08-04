# RedisPlus
Springboot整合Redis的一些扩展

## CacheExpire
设置缓存失效的时间,单位为毫秒,需配合@Cacheable使用

## TtlRedisCacheManager
覆盖默认的RedisCacheManager 使@CacheExpire能够生效

