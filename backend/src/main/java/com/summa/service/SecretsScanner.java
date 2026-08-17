package com.summa.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

@Service
public class SecretsScanner {
    private static final List<Pattern> SECRETS_PATTERNS = List.of(
        Pattern.compile("AKIA[0-9A-Z]{16}"),  // AWS access keys
        Pattern.compile("sk-[0-9a-zA-Z]{48}"), // OpenAI keys
        Pattern.compile("ghp_[0-9a-zA-Z]{36}"), // GitHub tokens
        Pattern.compile("xox[baprs]-[0-9a-zA-Z_-]+"), // Slack tokens
        Pattern.compile("(?i)password\\s*[:=]\\s*['\"][^'\"]{8,}") // password assignments
    );

    public List<String> scan(String content) {
        List<String> findings = new ArrayList<>();
        if (content == null) return findings;
        
        for (Pattern pattern : SECRETS_PATTERNS) {
            if (pattern.matcher(content).find()) {
                findings.add(pattern.pattern());
            }
        }
        return findings;
    }

    public boolean hasSecrets(String content) {
        return !scan(content).isEmpty();
    }
}
