package com.example;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.model.AnalysisHistoryRecord;
import com.example.model.LogAnalysisRequest;
import com.example.model.LogAnalysisResponse;
import com.example.service.AnalysisHistoryService;
import com.example.service.AiLogAnalysisService;
import com.example.service.LogSanitizationService;
import com.example.service.LogFileReaderService;

@RestController
@RequestMapping("/api")
public class LogAnalysisController {
    private final AiLogAnalysisService logAnalysisService;
    private final AnalysisHistoryService analysisHistoryService;
    private final LogSanitizationService logSanitizationService;
    private final LogFileReaderService logFileReaderService;

    public LogAnalysisController(AiLogAnalysisService logAnalysisService,
                                 AnalysisHistoryService analysisHistoryService,
                                 LogSanitizationService logSanitizationService,
                                 LogFileReaderService logFileReaderService) {
        this.logAnalysisService = logAnalysisService;
        this.analysisHistoryService = analysisHistoryService;
        this.logSanitizationService = logSanitizationService;
        this.logFileReaderService = logFileReaderService;
    }

    @PostMapping("/analyze-log")
    @ResponseStatus(HttpStatus.OK)
    public AnalysisHistoryRecord analyzeLog(@RequestBody LogAnalysisRequest request) {
        if (request == null || request.logText() == null || request.logText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "logText is required");
        }

        return analyzeAndSave(request);
    }

    @PostMapping(value = "/analyze-log-file", consumes = "multipart/form-data")
    public AnalysisHistoryRecord analyzeLogFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String environment) {
        return analyzeAndSave(new LogAnalysisRequest(serviceName, environment, logFileReaderService.read(file)));
    }

    private AnalysisHistoryRecord analyzeAndSave(LogAnalysisRequest request) {
        LogAnalysisRequest sanitizedRequest = logSanitizationService.sanitize(request);
        LogAnalysisResponse response = logAnalysisService.analyze(sanitizedRequest);
        return analysisHistoryService.save(sanitizedRequest, response);
    }

    @GetMapping("/analysis-history")
    public List<AnalysisHistoryRecord> getAnalysisHistory() {
        return analysisHistoryService.findAll();
    }

    @GetMapping("/analysis-history/{id}")
    public AnalysisHistoryRecord getAnalysisHistoryRecord(@PathVariable String id) {
        return analysisHistoryService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "analysis history not found"));
    }
}
