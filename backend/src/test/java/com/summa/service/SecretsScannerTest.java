package com.summa.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecretsScannerTest {

    @InjectMocks
    private SecretsScanner scanner;

    @Test
    void scan_findsAwsKey() {
        List<String> findings = scanner.scan("config: AKIAIOSFODNN7EXAMPLE");
        assertFalse(findings.isEmpty());
    }

    @Test
    void scan_findsOpenAiKey() {
        List<String> findings = scanner.scan("sk-proj-abcdefghijklmnopqrstuvwxyz0123456789_ABC");
        assertFalse(findings.isEmpty());
    }

    @Test
    void scan_findsGithubToken() {
        List<String> findings = scanner.scan("token: ghp_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghij");
        assertFalse(findings.isEmpty());
    }

    @Test
    void scan_findsSlackToken() {
        List<String> findings = scanner.scan("token: xoxb-test-12345-67890-abcdefghijklmnopqrstuvwx");
        assertFalse(findings.isEmpty());
    }

    @Test
    void scan_findsPasswordAssignment() {
        List<String> findings = scanner.scan("password = \"supersecret123\"");
        assertFalse(findings.isEmpty());
    }

    @Test
    void scan_findsBearerToken() {
        List<String> findings = scanner.scan("Authorization: \"Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.string\"");
        assertFalse(findings.isEmpty());
    }

    @Test
    void scan_emptyForCleanContent() {
        List<String> findings = scanner.scan("This is normal text with no secrets");
        assertTrue(findings.isEmpty());
    }

    @Test
    void scan_returnsEmptyForNull() {
        List<String> findings = scanner.scan(null);
        assertTrue(findings.isEmpty());
    }

    @Test
    void hasSecrets_trueWhenFindingsExist() {
        assertTrue(scanner.hasSecrets("AKIAIOSFODNN7EXAMPLE"));
    }

    @Test
    void hasSecrets_falseWhenClean() {
        assertFalse(scanner.hasSecrets("nothing sensitive here"));
    }

    @Test
    void hasSecrets_falseForNull() {
        assertFalse(scanner.hasSecrets(null));
    }
}
