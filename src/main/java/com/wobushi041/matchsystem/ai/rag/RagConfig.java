package com.wobushi041.matchsystem.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.util.List;

/**
 * RAG 检索增强生成配置
 * 加载编程文档 → 切段 → 向量化（DashScope text-embedding-v3） → 存入内存向量库
 */
@Configuration
@Slf4j
public class RagConfig {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 内容检索器
     * 加载 resources/docs/ 下的编程文档，切段后向量化存储
     */
    @Bean
    public ContentRetriever contentRetriever() {
        // 1. 加载文档
        File docsDir;
        try {
            docsDir = ResourceUtils.getFile("classpath:docs");
        } catch (Exception e) {
            log.warn("未找到 RAG 文档目录 classpath:docs，跳过 RAG 初始化");
            return null;
        }

        if (!docsDir.exists() || !docsDir.isDirectory()) {
            log.warn("RAG 文档目录不存在: {}，跳过 RAG 初始化", docsDir.getPath());
            return null;
        }

        List<Document> documents = FileSystemDocumentLoader.loadDocuments(docsDir.toPath());
        if (documents.isEmpty()) {
            log.warn("RAG 文档目录为空，跳过向量化");
            return null;
        }

        // 2. 文档切段：每段最大 1000 字符，重叠 200 字符
        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(1000, 200);

        // 3. 文档加载器：切段 → 拼接文件名 → 向量化 → 存入向量库
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .textSegmentTransformer(segment -> TextSegment.from(
                        segment.metadata().getString("file_name") + "\n" + segment.text(),
                        segment.metadata()))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);
        log.info("RAG 文档加载完成，共 {} 篇文档", documents.size());

        // 4. 内容检索器
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.75)
                .build();
    }
}
