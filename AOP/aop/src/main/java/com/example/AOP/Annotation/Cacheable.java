package com.example.AOP.Annotation;

import java.lang.annotation.*;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {
    String key() default "";
    long duration() default 300; // بالثواني
}