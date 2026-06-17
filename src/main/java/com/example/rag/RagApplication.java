package com.example.rag;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RAG 后端应用入口。
 *
 * <p>通过 {@link MapperScan} 扫描 MyBatis-Plus Mapper，启动后提供文档入库、
 * 向量检索问答和调用日志查询能力。</p>
 */
@MapperScan("com.example.rag.mapper")
@SpringBootApplication
public class RagApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagApplication.class, args);
    }
}
