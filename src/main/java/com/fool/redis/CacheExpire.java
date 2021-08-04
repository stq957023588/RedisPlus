package com.fool.redis;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheExpire {

    long expire() default 1000 * 60 * 50 * 24;


}
