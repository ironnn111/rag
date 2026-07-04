package com.example.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rag.entity.TDocumentChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档切块表数据访问接口。
 */
@Mapper
public interface TDocumentChunkMapper extends BaseMapper<TDocumentChunk> {
}
