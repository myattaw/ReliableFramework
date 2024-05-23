package me.rages.reliableframework.files.messages;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MessageConfig {

    String config();
    String[] message() default {};

}