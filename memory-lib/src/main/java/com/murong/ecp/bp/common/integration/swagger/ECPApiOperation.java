package com.murong.ecp.bp.common.integration.swagger;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ECPApiOperation {

    String value() default "";
    String notes() default "";

    String[] tags() default {""};

    int position() default 0;

    int code() default 200;
}
