package com.decisionhub.service.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportResult {
    private byte[] data;
    private String filename;
    private String contentType;
}
