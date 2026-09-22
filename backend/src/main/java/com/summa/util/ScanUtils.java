package com.summa.util;

import com.summa.exception.ConflictException;
import com.summa.service.AuditService;
import com.summa.service.SecretsScanner;
import java.util.stream.Collectors;

public final class ScanUtils {
    private ScanUtils() {}

    public static void scanForSecrets(String content, String actor, String objectType, String objectId,
                                       SecretsScanner secretsScanner, AuditService auditService) {
        if (content != null && secretsScanner.hasSecrets(content)) {
            java.util.List<String> findings = secretsScanner.scan(content);
            auditService.logSystem("SECRET_DETECTED", objectType, objectId,
                String.format("{\"actor\":%s,\"findings\":[%s]}", JsonHelpers.jsonString(actor),
                    findings != null ? findings.stream().map(f -> "\"" + f + "\"").collect(Collectors.joining(",")) : ""));
            throw new ConflictException("Content contains secrets and cannot be written");
        }
    }
}
