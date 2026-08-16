package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "ai.model.enabled=false")
class AiOpsToolApplicationTests {
    @Test
    void contextLoads() {
    }
}
