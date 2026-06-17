package com.example.rag.service;

import com.example.rag.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DocumentChunkerTest {

    private DocumentChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new DocumentChunker();
        ReflectionTestUtils.setField(chunker, "properties", new RagProperties(50, 10, 5, 0.6));
    }

    @Test
    void shouldThrowNpeForNullContent() {
        assertThatNullPointerException().isThrownBy(() -> chunker.chunk(null));
    }

    @Test
    void shouldReturnEmptyListForEmptyContent() {
        assertThat(chunker.chunk("")).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForWhitespaceContent() {
        assertThat(chunker.chunk("   \n  \t  ")).isEmpty();
    }

    @Test
    void shouldReturnSingleChunkForShortContent() {
        String content = "这是一段很短的文本，不需要切块。";
        List<String> chunks = chunker.chunk(content);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(content);
    }

    @Test
    void shouldReturnSingleChunkWhenContentEqualsChunkSize() {
        String content = "A".repeat(50);
        List<String> chunks = chunker.chunk(content);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(content);
    }

    @Test
    void shouldChunkLongContentIntoMultiplePieces() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("这是第").append(i).append("段测试文本内容。");
        }
        String content = sb.toString();

        List<String> chunks = chunker.chunk(content);

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> assertThat(c).isNotEmpty());
        assertThat(content).startsWith(chunks.get(0));
        assertThat(content).endsWith(chunks.get(chunks.size() - 1));
    }

    @Test
    void shouldBreakAtNaturalPunctuation() {
        // chunkSize=200 使 adjustEnd 循环能执行（需要 i > start+100）
        ReflectionTestUtils.setField(chunker, "properties", new RagProperties(200, 20, 5, 0.6));
        String prefix = "A".repeat(185);
        String content = prefix + "。后续内容应被切到下一个块。更多文本填充测试数据确保足够长。";

        List<String> chunks = chunker.chunk(content);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0)).endsWith("。");
    }

    @Test
    void shouldBreakAtNewline() {
        // chunkSize=200 使 adjustEnd 循环能执行，在换行处断开
        // 注意：.strip() 会去掉尾部换行符，所以不能直接校验 chunk 以 \n 结尾
        ReflectionTestUtils.setField(chunker, "properties", new RagProperties(200, 20, 5, 0.6));
        String content = "A".repeat(185) + "\n" + "B".repeat(200);

        List<String> chunks = chunker.chunk(content);

        // 验证在换行处被切割：第一个块不应包含 B，第二个块以 B 开头
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks.get(0)).doesNotContain("B");
        assertThat(chunks.get(1)).contains("B");
    }

    @Test
    void shouldHaveOverlapBetweenAdjacentChunks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("这是第").append(i).append("条信息。");
        }
        String content = sb.toString();
        List<String> chunks = chunker.chunk(content);

        if (chunks.size() >= 2) {
            String tailOfFirst = chunks.get(0).substring(Math.max(0, chunks.get(0).length() - 10));
            assertThat(chunks.get(1)).contains(tailOfFirst);
        }
    }

    @Test
    void shouldHandleContentWithOnlyPunctuation() {
        String content = "。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。";
        List<String> chunks = chunker.chunk(content);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(c -> assertThat(c).isNotEmpty());
    }

    @Test
    void chunkSizeShouldBeAtLeastMinimum() {
        ReflectionTestUtils.setField(chunker, "properties", new RagProperties(5, 2, 5, 0.6));
        String content = "A".repeat(200);

        List<String> chunks = chunker.chunk(content);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).length()).isLessThanOrEqualTo(100);
    }
}
