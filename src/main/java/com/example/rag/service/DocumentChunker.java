package com.example.rag.service;

import com.example.rag.config.RagProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档切块组件。
 *
 * <p>根据 yml 中的 chunk 配置把长文档拆成适合 embedding 的片段，
 * 并尽量在标点或换行处结束切块，减少语义截断。</p>
 */
@Component
public class DocumentChunker {

    @Autowired
    private RagProperties properties;

    /**
     * 将原始文档内容切分为多个可向量化片段。
     */
    public List<String> chunk(String content) {
        String normalized = content.strip();
        if (normalized.isEmpty()) {
            return List.of();
        }

        int chunkSize = Math.max(properties.chunkSize(), 100);
        int overlap = Math.max(0, Math.min(properties.chunkOverlap(), chunkSize / 2));
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
}
