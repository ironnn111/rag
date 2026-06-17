package com.example.rag.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Word 文档文本提取组件。
 *
 * <p>支持 .docx（基于 XWPF）和 .doc（基于 HWPF）格式。</p>
 */
@Component
public class WordDocumentParser {

    /**
     * 从输入流中提取 Word 文档的纯文本内容。
     *
     * @param inputStream Word 文档输入流
     * @param fileName    文件名，用于判断格式（.docx 或 .doc）
     * @return 提取的纯文本
     */
    public String extractText(InputStream inputStream, String fileName) throws IOException {
        if (fileName != null && fileName.toLowerCase().endsWith(".docx")) {
            return extractFromDocx(inputStream);
        }
        return extractFromDoc(inputStream);
    }

    private String extractFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractFromDoc(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }
}
