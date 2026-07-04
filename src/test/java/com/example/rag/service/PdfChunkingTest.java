package com.example.rag.service;

import com.example.rag.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class PdfChunkingTest {

    private final PdfDocumentParser parser = new PdfDocumentParser();
    private final DocumentChunker chunker = new DocumentChunker();

    @Test
    void testRagTestDoc() throws Exception {
        ReflectionTestUtils.setField(chunker, "properties", new RagProperties(500, 100, 5, 0.6));
        runTest("samples/pdf/rag-test-doc.pdf");
    }

    @Test
    void testClongevalPaper() throws Exception {
        ReflectionTestUtils.setField(chunker, "properties", new RagProperties(500, 100, 5, 0.6));
        runTest("samples/pdf/clongeval-paper.pdf");
    }

    private void runTest(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.out.println("[SKIP] File not found: " + filePath);
            return;
        }

        String filename = path.getFileName().toString();
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PDF: " + filename + "  (" + Files.size(path) + " bytes)");
        System.out.println("=".repeat(70));

        PdfDocumentParser.PdfParseResult result;
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            result = parser.parse(fis);
        }

        System.out.println("Pages: " + result.getPageCount() + " | Scanned: " + result.isScanned());
        System.out.println("Full text length (before merge): " + result.getFullText().length());

        String cleaned = parser.mergeBrokenLines(result.getFullText());
        System.out.println("Full text length (after merge):  " + cleaned.length());

        System.out.println("\n--- Per-page text stats ---");
        for (var page : result.getPages()) {
            String text = page.getText().strip();
            System.out.printf("  Page %2d: %5d chars  %s%n",
                    page.getPageNumber(), text.length(),
                    text.length() == 0 ? "(EMPTY - possibly image)" : "");
        }

        List<ChunkResult> chunks = chunker.chunkWithMetadata(
                cleaned, filename, result.getPages());
        System.out.println("\nTotal chunks: " + chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            ChunkResult cr = chunks.get(i);
            String preview = cr.getText().length() > 80
                    ? cr.getText().substring(0, 80).replace("\n", "\u21B5") + "..."
                    : cr.getText().replace("\n", "\u21B5");
            System.out.printf("  [%2d] page=%s section=%s len=%d | %s%n",
                    i,
                    cr.getMetadata().getOrDefault("page", "?"),
                    cr.getMetadata().getOrDefault("sectionTitle", "?"),
                    cr.getText().length(),
                    preview);
        }
    }
}
