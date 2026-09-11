package com.murong.ecp.bp.common.dict;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface ECPDict {
    boolean required() default true;

    String name() default "";

    String message() default "";

    String desc() default "";

    int length() default -1;

    int minLength() default -1;

    int precision() default 0;

    String errorCode() default "";

    int maxValue() default -1;

    int minValue() default -1;

    MrDictType type() default MrDictType.CHAR;

    String regex() default "";

    String mask() default "";

    Class<?> enums() default MrEnumValue.class;
}
