package com.example.rag.service;

import com.example.rag.config.RagProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 文档切块组件。
 *
 * <p>提供两种切分策略：</p>
 * <ul>
 *   <li>{@link #chunk(String)} — 滑动窗口切分，用于纯文本，向后兼容</li>
 *   <li>{@link #chunkWithMetadata} — 结构化切分，用于 PDF 等有结构的文档</li>
 * </ul>
 *
 * <p>结构化切分优先级：标题/章节 → 自然段 → 句子 → 固定长度兜底。</p>
 * <p>特殊内容处理：表格整表成块、FAQ 一问一答成块、步骤流按步骤组切。</p>
 */
@Component
public class DocumentChunker {

    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(第[一二三四五六七八九十百千]+[章节篇]|\\d+[\\.、．]?\\s*|第\\d+条|[一二三四五六七八九十百千]+[、．]|\\([一二三四五六七八九十百千]+\\)|（[一二三四五六七八九十百千]+）|[IVX]+[\\.、]|\\w+[\\.、])"
    );
    private static final Pattern ENGLISH_HEADING = Pattern.compile(
            "^\\d+(\\.\\d+)*\\s+[A-Z][A-Za-z].{0,50}$"
    );
    private static final Pattern FAQ_Q_PATTERN = Pattern.compile("^[Q问]\\d*[：:．\\s]");
    private static final Pattern FAQ_A_PATTERN = Pattern.compile("^[A答]\\d*[：:．\\s]");
    private static final Pattern FAQ_FLEX_PATTERN = Pattern.compile("^(Q|问)[\\s.]*\\d*[：:．、]|^\\d+[\\.、]\\s*(Q|问|问题)");
    private static final Pattern STEP_PATTERN = Pattern.compile("^(步骤\\s*\\d+|第\\s*\\d+\\s*步|\\d+[\\.、．])\\s*");
    private static final Pattern SENTENCE_END = Pattern.compile("[。！？.!?；;]$");
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("^\\|.*\\|$");

    @Autowired
    private RagProperties properties;

    // ==================== 简单滑动窗口切分（向后兼容） ====================

    /**
     * 滑动窗口切分，在标点或换行处断开。
     *
     * <p>用于纯文本 / Word 文档，行为与旧版一致。</p>
     */
    public List<String> chunk(String content) {
        String normalized = content.strip();
        if (normalized.isEmpty()) {
            return List.of();
        }

        int chunkSize = Math.max(properties.getChunkSize(), 100);
        int overlap = Math.max(0, Math.min(properties.getChunkOverlap(), chunkSize / 2));
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            int adjustedEnd = adjustEnd(normalized, start, end);
            chunks.add(normalized.substring(start, adjustedEnd).strip());
            if (adjustedEnd == normalized.length()) {
                break;
            }
            start = Math.max(adjustedEnd - overlap, start + 1);
        }

        return chunks;
    }

    /**
     * 尝试把切块终点调整到自然断句位置。
     */
    private int adjustEnd(String content, int start, int proposedEnd) {
        if (proposedEnd == content.length()) {
            return proposedEnd;
        }
        int best = -1;
        for (int i = proposedEnd; i > start + 100; i--) {
            char c = content.charAt(i - 1);
            if (c == '\n' || c == '。' || c == '.' || c == '!' || c == '?' || c == '；' || c == ';') {
                best = i;
                break;
            }
        }
        return best > 0 ? best : proposedEnd;
    }

    // ==================== 结构化切分（PDF / 带结构文档） ====================

    /**
     * 结构化切块，返回带元数据的 chunk 列表。
     *
     * @param content    清洗后的全文
     * @param docTitle   文档标题
     * @param pageInfos  逐页信息（可为 null）
     */
    public List<ChunkResult> chunkWithMetadata(String content, String docTitle, List<PdfDocumentParser.PageInfo> pageInfos) {
        String normalized = content.strip();
        if (normalized.isEmpty()) {
            return List.of();
        }

        int chunkSize = Math.max(properties.getChunkSize(), 100);
        int overlap = Math.max(0, Math.min(properties.getChunkOverlap(), chunkSize / 2));

        // 第一步：尝试按标题/章节切分
        List<Section> sections = splitByHeadings(normalized);

        // 第二步：每个章节内按自然段切分并构建切块
        List<ChunkResult> results = new ArrayList<>();
        int chunkIndex = 0;

        for (Section section : sections) {
            List<String> paragraphs = splitParagraphs(section.getBody());

            List<String> sectionChunks = buildChunksFromParagraphs(
                    paragraphs, chunkSize, overlap);

            for (String chunkText : sectionChunks) {
                Map<String, Object> metadata = buildMetadata(
                        docTitle, pageInfos, section.getTitle(), section.getSubtitle(), chunkIndex, chunkText);
                results.add(new ChunkResult(chunkText, metadata));
                chunkIndex++;
            }
        }

        return results;
    }

    // ==================== 结构切分 ====================

    private List<Section> splitByHeadings(String text) {
        List<Section> sections = new ArrayList<>();
        String[] lines = text.split("\n");

        StringBuilder currentTitle = new StringBuilder("正文");
        StringBuilder currentBody = new StringBuilder();
        String currentSubtitle = null;

        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                currentBody.append("\n");
                continue;
            }

            if (isHeadingLine(trimmed)) {
                if (!currentBody.toString().strip().isEmpty()) {
                    sections.add(new Section(
                            currentTitle.toString().strip(),
                            currentSubtitle,
                            currentBody.toString().strip()
                    ));
                }
                currentTitle = new StringBuilder(trimmed);
                currentBody = new StringBuilder();
                currentSubtitle = null;
            } else if (isSubHeadingLine(trimmed) && !sections.isEmpty()) {
                if (!currentBody.toString().strip().isEmpty()) {
                    sections.add(new Section(
                            currentTitle.toString().strip(),
                            currentSubtitle,
                            currentBody.toString().strip()
                    ));
                    currentBody = new StringBuilder();
                }
                currentSubtitle = trimmed;
            } else {
                currentBody.append(trimmed).append("\n");
            }
        }

        if (!currentBody.toString().strip().isEmpty()) {
            sections.add(new Section(
                    currentTitle.toString().strip(),
                    currentSubtitle,
                    currentBody.toString().strip()
            ));
        }

        return sections.isEmpty()
                ? List.of(new Section("正文", null, text))
                : sections;
    }

    private boolean isHeadingLine(String line) {
        if (line.length() > 60) return false;
        if (HEADING_PATTERN.matcher(line).find()) return true;
        if (ENGLISH_HEADING.matcher(line).find()) return true;
        return line.length() <= 25 && !SENTENCE_END.matcher(line).find()
                && !line.startsWith("•") && !line.startsWith("-");
    }

    private boolean isSubHeadingLine(String line) {
        return line.length() <= 30 && HEADING_PATTERN.matcher(line).find();
    }

    // ==================== 段落切分 ====================

    private List<String> splitParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        String[] blocks = text.split("\n{2,}");

        for (String block : blocks) {
            String trimmed = block.strip();
            if (trimmed.isEmpty()) continue;

            if (isFaqContent(trimmed)) {
                paragraphs.addAll(splitFaq(trimmed));
            } else if (isStepContent(trimmed)) {
                paragraphs.addAll(splitSteps(trimmed));
            } else if (isTableContent(trimmed)) {
                paragraphs.add(trimmed);
            } else {
                paragraphs.add(trimmed.replace("\n", ""));
            }
        }

        return paragraphs;
    }

    // ==================== 切块构建 ====================

    private List<String> buildChunksFromParagraphs(List<String> paragraphs, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (paragraph.length() > chunkSize * 2) {
                if (!current.isEmpty()) {
                    chunks.add(current.toString().strip());
                    int keepStart = Math.max(0, current.length() - overlap);
                    current = new StringBuilder(current.substring(keepStart));
                }
                List<String> sentenceChunks = splitBySentences(paragraph, chunkSize, overlap);
                chunks.addAll(sentenceChunks);
                continue;
            }

            int projectedLength = current.length() + paragraph.length() + 1;

            if (projectedLength > chunkSize && !current.isEmpty()) {
                chunks.add(current.toString().strip());
                int keepLen = Math.min(overlap, current.length());
                String keepText = current.substring(Math.max(0, current.length() - keepLen));
                current = new StringBuilder(keepText);
            }

            if (!current.isEmpty()) {
                current.append("\n");
            }
            current.append(paragraph);
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().strip());
        }

        if (chunks.isEmpty() && !current.isEmpty()) {
            chunks.add(current.toString().strip());
        }

        return chunks;
    }

    // ==================== 句子切分（兜底细切） ====================

    private List<String> splitBySentences(String text, int chunkSize, int overlap) {
        List<String> sentences = new ArrayList<>();
        StringBuilder sentence = new StringBuilder();

        for (char c : text.toCharArray()) {
            sentence.append(c);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?' || c == '；' || c == ';' || c == '\n') {
                String s = sentence.toString().strip();
                if (!s.isEmpty()) {
                    sentences.add(s);
                }
                sentence = new StringBuilder();
            }
        }
        if (!sentence.toString().strip().isEmpty()) {
            sentences.add(sentence.toString().strip());
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String s : sentences) {
            if (current.length() + s.length() > chunkSize && !current.isEmpty()) {
                chunks.add(current.toString().strip());
                if (overlap > 0 && current.length() > overlap) {
                    current = new StringBuilder(current.substring(current.length() - overlap));
                } else {
                    current = new StringBuilder();
                }
            }
            if (!current.isEmpty()) {
                current.append(" ");
            }
            current.append(s);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().strip());
        }

        return chunks.isEmpty() ? List.of(text) : chunks;
    }

    // ==================== 特殊内容识别 ====================

    private boolean isFaqContent(String text) {
        String[] lines = text.split("\n");
        int qaCount = 0;
        for (String line : lines) {
            String s = line.strip();
            if (FAQ_Q_PATTERN.matcher(s).find() || FAQ_A_PATTERN.matcher(s).find()) {
                qaCount++;
            } else if (FAQ_FLEX_PATTERN.matcher(s).find()) {
                qaCount++;
            } else if (s.startsWith("Q") && (s.contains(":") || s.contains("：") || s.length() < 40)) {
                qaCount++;
            }
        }
        return qaCount >= 2;
    }

    private boolean isStepContent(String text) {
        String[] lines = text.split("\n");
        int stepCount = 0;
        for (String line : lines) {
            if (STEP_PATTERN.matcher(line.strip()).find()) {
                stepCount++;
            }
        }
        return stepCount >= 3;
    }

    private boolean isTableContent(String text) {
        String[] lines = text.split("\n");
        int tableRowCount = 0;
        for (String line : lines) {
            if (TABLE_ROW_PATTERN.matcher(line.strip()).find()) {
                tableRowCount++;
            }
        }
        return tableRowCount >= 2;
    }

    // ==================== 特殊内容切分 ====================

    private List<String> splitFaq(String text) {
        List<String> qaChunks = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder currentQa = new StringBuilder();
        boolean inQA = false;

        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) continue;

            if (FAQ_Q_PATTERN.matcher(trimmed).find()) {
                if (inQA && !currentQa.isEmpty()) {
                    qaChunks.add(currentQa.toString().strip());
                }
                currentQa = new StringBuilder(trimmed);
                inQA = true;
            } else if (FAQ_A_PATTERN.matcher(trimmed).find() && inQA) {
                currentQa.append("\n").append(trimmed);
            } else if (inQA) {
                currentQa.append("\n").append(trimmed);
            }
        }
        if (!currentQa.isEmpty()) {
            qaChunks.add(currentQa.toString().strip());
        }

        return qaChunks.isEmpty() ? List.of(text) : qaChunks;
    }

    private List<String> splitSteps(String text) {
        List<String> stepGroups = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder currentGroup = new StringBuilder();
        int stepCount = 0;

        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                if (stepCount >= 3 && !currentGroup.isEmpty()) {
                    stepGroups.add(currentGroup.toString().strip());
                    currentGroup = new StringBuilder();
                    stepCount = 0;
                } else if (!currentGroup.isEmpty()) {
                    currentGroup.append("\n");
                }
                continue;
            }

            if (STEP_PATTERN.matcher(trimmed).find()) {
                if (stepCount >= 4) {
                    stepGroups.add(currentGroup.toString().strip());
                    currentGroup = new StringBuilder();
                    stepCount = 0;
                }
                stepCount++;
            }

            if (!currentGroup.isEmpty()) {
                currentGroup.append("\n");
            }
            currentGroup.append(trimmed);
        }

        if (!currentGroup.isEmpty()) {
            stepGroups.add(currentGroup.toString().strip());
        }

        return stepGroups.isEmpty() ? List.of(text) : stepGroups;
    }

    // ==================== 元数据构建 ====================

    private Map<String, Object> buildMetadata(
            String docTitle,
            List<PdfDocumentParser.PageInfo> pageInfos,
            String sectionTitle,
            String subtitle,
            int chunkIndex,
            String chunkText) {

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("documentTitle", docTitle);
        meta.put("sectionTitle", sectionTitle != null ? sectionTitle : "正文");
        meta.put("chunkIndex", chunkIndex);

        if (subtitle != null) {
            meta.put("subtitle", subtitle);
        }

        if (pageInfos != null && !pageInfos.isEmpty()) {
            int page = estimatePage(chunkText, pageInfos);
            meta.put("page", page);
        }

        return meta;
    }

    private int estimatePage(String chunkText, List<PdfDocumentParser.PageInfo> pageInfos) {
        String firstSentence = chunkText.length() > 50 ? chunkText.substring(0, 50) : chunkText;
        for (PdfDocumentParser.PageInfo page : pageInfos) {
            if (page.getText().contains(firstSentence)) {
                return page.getPageNumber();
            }
        }
        String[] words = chunkText.split("\\s+");
        String searchTerm = words.length > 3
                ? String.join(" ", Arrays.copyOf(words, 3))
                : chunkText.substring(0, Math.min(30, chunkText.length()));
        for (PdfDocumentParser.PageInfo page : pageInfos) {
            if (page.getText().contains(searchTerm)) {
                return page.getPageNumber();
            }
        }
        return 1;
    }

    // ==================== 内部数据结构 ====================

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class Section {
        private String title;
        private String subtitle;
        private String body;
    }
}
