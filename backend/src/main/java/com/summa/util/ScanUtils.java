package com.summa.util;

import com.summa.service.AuditService;
import com.summa.service.SecretsScanner;
import java.util.stream.Collectors;

public final class ScanUtils {
    private ScanUtils() {}

    public static void scanForSecrets(String content, String actor, String objectType, String objectId,
                                       SecretsScanner secretsScanner, AuditService auditService) {
        if (content != null && secretsScanner.hasSecrets(content)) {
            auditService.logSystem("SECRET_DETECTED", objectType, objectId,
                String.format("{\"actor\":\"%s\",\"findings\":[%s]}", actor,
                    secretsScanner.scan(content).stream().map(f -> "\"" + f + "\"").collect(Collectors.joining(","))));
            throw new IllegalStateException("Content contains secrets and cannot be written");
        }
    }
}
