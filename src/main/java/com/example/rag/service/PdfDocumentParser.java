package com.example.rag.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class PdfDocumentParser {

    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("^\\s*(—\\s*)?\\d{1,4}(\\s*—)?\\s*$");
    private static final Pattern TOC_LINE_PATTERN = Pattern.compile(".*\\.{5,}\\s*\\d+\\s*$");
    private static final int HEADER_FOOTER_MIN_REPEAT = 3;

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PdfParseResult {
        private String fullText;
        private List<PageInfo> pages;
        private boolean isScanned;
        private int pageCount;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PageInfo {
        private int pageNumber;
        private String text;
    }

    public PdfParseResult parse(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            int pageCount = document.getNumberOfPages();

            // 逐页提取文本
            List<String> rawPages = new ArrayList<>();
            for (int i = 0; i < pageCount; i++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                stripper.setSortByPosition(true);
                stripper.setAddMoreFormatting(false);
                String pageText = stripper.getText(document);
                rawPages.add(pageText);
            }

            boolean isScanned = detectScanned(rawPages);
            Set<String> headerFooterLines = detectHeaderFooterLines(rawPages);

            List<PageInfo> cleanedPages = new ArrayList<>();
            StringBuilder fullText = new StringBuilder();

            for (int i = 0; i < rawPages.size(); i++) {
                String cleaned = cleanPage(rawPages.get(i), headerFooterLines, i == 0);
                cleanedPages.add(new PageInfo(i + 1, cleaned));
                fullText.append(cleaned).append("\n");
            }

            return new PdfParseResult(fullText.toString().strip(), cleanedPages, isScanned, pageCount);
        }
    }

    private boolean detectScanned(List<String> rawPages) {
        int checkPages = Math.min(5, rawPages.size());
        int totalLen = 0;
        for (int i = 0; i < checkPages; i++) {
            totalLen += rawPages.get(i).replaceAll("\\s+", "").length();
        }
        return (double) totalLen / checkPages < 50;
    }

    private Set<String> detectHeaderFooterLines(List<String> rawPages) {
        if (rawPages.size() < HEADER_FOOTER_MIN_REPEAT) {
            return Set.of();
        }

        Map<String, Integer> lineFrequency = new HashMap<>();

        for (String page : rawPages) {
            String[] lines = page.split("\r?\n");
            if (lines.length == 0) continue;

            int topCheck = Math.min(3, lines.length);
            for (int i = 0; i < topCheck; i++) {
                String trimmed = lines[i].strip();
                if (trimmed.length() > 1 && trimmed.length() < 200) {
                    lineFrequency.merge(trimmed, 1, Integer::sum);
                }
            }

            int bottomCheck = Math.min(3, lines.length);
            for (int i = lines.length - bottomCheck; i < lines.length; i++) {
                String trimmed = lines[i].strip();
                if (trimmed.length() > 1 && trimmed.length() < 200) {
                    lineFrequency.merge(trimmed, 1, Integer::sum);
                }
            }
        }

        // 在至少 HEADER_FOOTER_MIN_REPEAT 个页面中出现的行视为页眉页脚
        Set<String> result = new HashSet<>();
        for (var entry : lineFrequency.entrySet()) {
            if (entry.getValue() >= HEADER_FOOTER_MIN_REPEAT) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private String cleanPage(String rawText, Set<String> headerFooterLines, boolean isFirstPage) {
        String[] lines = rawText.split("\r?\n");
        StringBuilder cleaned = new StringBuilder();
        boolean inToc = isFirstPage;

        for (String line : lines) {
            String trimmed = line.strip();

            if (trimmed.isEmpty()) {
                cleaned.append("\n");
                continue;
            }

            if (headerFooterLines.contains(trimmed)) {
                continue;
            }

            if (PAGE_NUMBER_PATTERN.matcher(trimmed).matches()) {
                continue;
            }

            if (TOC_LINE_PATTERN.matcher(trimmed).matches()) {
                inToc = true;
                continue;
            }

            if (inToc && trimmed.length() > 20 && !trimmed.contains("...")) {
                inToc = false;
            }
            if (inToc) {
                continue;
            }

            trimmed = repairLineBreaks(trimmed);
            cleaned.append(trimmed).append("\n");
        }

        return cleaned.toString();
    }

    private String repairLineBreaks(String text) {
        // 把连续的非空行合并，除非行尾有明显段落结束标志
        // 这里做轻量处理：去掉行内多余的独立换行符
        return text.replace("\r", "");
    }

    public String mergeBrokenLines(String pageText) {
        String[] lines = pageText.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty()) {
                result.append("\n\n");
                continue;
            }

            // 如果上一行不是以句末标点结束，且本行以小写/中文开头，则合并
            boolean prevEndsWithPunct = i > 0 && lines[i - 1].strip().matches(".*[。！？.!?；;：:]$");
            boolean curStartsNormal = !line.startsWith("•") && !line.startsWith("-") && !line.startsWith("·");

            if (i > 0 && !prevEndsWithPunct && curStartsNormal && !lines[i - 1].strip().isEmpty()) {
                result.append(line);
            } else {
                if (i > 0) {
                    result.append("\n");
                }
                result.append(line);
            }
        }

        return result.toString();
    }
}
