package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class LogFileReaderServiceTests {
    private final LogFileReaderService service = new LogFileReaderService(1024);

    @Test
    void readsGb18030EncodedLog() {
        byte[] content = "ERROR 数据库连接超时".getBytes(Charset.forName("GB18030"));
        MockMultipartFile file = new MockMultipartFile("file", "app.log", "text/plain", content);

        assertThat(service.read(file)).isEqualTo("ERROR 数据库连接超时");
    }
}
