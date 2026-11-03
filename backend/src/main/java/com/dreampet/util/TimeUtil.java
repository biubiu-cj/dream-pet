package com.dreampet.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 统一时间格式：yyyy-MM-dd HH:mm:ss（东八区由 Jackson 配置保证） */
public final class TimeUtil {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() {
    }

    public static String format(LocalDateTime time) {
        return time == null ? null : FORMATTER.format(time);
    }
}