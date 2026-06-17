package com.example.rag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 业务配置入口。
 *
 * <p>负责启用 {@link RagProperties}，让 yml 中的 rag 配置可以注入到业务组件。</p>
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {
}
