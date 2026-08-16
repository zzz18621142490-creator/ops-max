package com.example.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LogFileReaderService {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".log", ".txt");
    private static final Charset GB18030 = Charset.forName("GB18030");

    private final long maxBytes;

    public LogFileReaderService(@Value("${ai.log-upload.max-bytes:5242880}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public String read(MultipartFile file) {
        validate(file);
        try {
            String content = decode(file.getBytes());
            if (content.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "log file is empty");
            }
            if (content.indexOf('\0') >= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "binary files are not supported");
            }
            return content;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to read log file", exception);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "log file is required");
        }
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "log file exceeds the " + maxBytes + " byte limit");
        }

        String filename = file.getOriginalFilename();
        String normalized = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (SUPPORTED_EXTENSIONS.stream().noneMatch(normalized::endsWith)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only .log and .txt files are supported");
        }
    }

    private static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            return new String(bytes, GB18030);
        }
    }
}
