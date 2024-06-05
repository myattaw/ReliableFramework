package me.rages.reliableframework.data.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Id {

    boolean autoIncrement() default false;

}
