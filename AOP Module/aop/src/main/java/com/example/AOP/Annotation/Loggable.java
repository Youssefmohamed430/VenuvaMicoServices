package com.example.AOP.Annotation;

import java.lang.annotation.*;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {
    String value() default "Operation";
    boolean logArguments() default true;
    boolean logResult() default true;
}