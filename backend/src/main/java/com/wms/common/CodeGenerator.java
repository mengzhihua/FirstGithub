package com.wms.common;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/** Generates document numbers like ASN20250101103000-0001 */
@Component
public class CodeGenerator {
    private final AtomicInteger seq = new AtomicInteger(0);
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String next(String prefix) {
        int n = seq.updateAndGet(i -> i >= 9999 ? 1 : i + 1);
        return String.format("%s%s-%04d", prefix, LocalDateTime.now().format(F), n);
    }
}
