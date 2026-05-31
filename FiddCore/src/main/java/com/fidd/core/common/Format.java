package com.fidd.core.common;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Format {
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
}
