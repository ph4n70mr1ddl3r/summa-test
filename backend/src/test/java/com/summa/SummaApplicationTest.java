package com.summa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SummaApplication.class)
@ActiveProfiles("test")
class SummaApplicationTest {

    @Test
    void contextLoads() {
        // Unit tests cover the service logic; this just verifies no startup crashes
    }
}
