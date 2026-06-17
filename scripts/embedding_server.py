"""本地 Embedding 服务 - 兼容 OpenAI /v1/embeddings 接口。

模型: BAAI/bge-small-zh-v1.5 (512维)
端口: 18080

首次运行会自动下载模型（约 95MB），请耐心等待。
"""

import torch
from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

MODEL_NAME = "BAAI/bge-small-zh-v1.5"

device = "mps" if torch.backends.mps.is_available() else ("cuda" if torch.cuda.is_available() else "cpu")
print(f"Loading model {MODEL_NAME} on {device} ...")
model = SentenceTransformer(MODEL_NAME, device=device)
print(f"Model loaded. Dimension: {model.get_sentence_embedding_dimension()}")

app = FastAPI()


class EmbeddingRequest(BaseModel):
    model: str = MODEL_NAME
    input: str | list[str]
    encoding_format: str = "float"


class EmbeddingData(BaseModel):
    object: str = "embedding"
    index: int
    embedding: list[float]


class EmbeddingUsage(BaseModel):
    prompt_tokens: int = 0
    total_tokens: int = 0


class EmbeddingResponse(BaseModel):
    object: str = "list"
    data: list[EmbeddingData]
    model: str = MODEL_NAME
    usage: EmbeddingUsage = EmbeddingUsage()


@app.post("/v1/embeddings")
async def embeddings(body: EmbeddingRequest):
    texts = body.input if isinstance(body.input, list) else [body.input]
    vectors = model.encode(texts, normalize_embeddings=True)
    data = [
        EmbeddingData(index=i, embedding=vec.tolist())
        for i, vec in enumerate(vectors)
    ]
    return EmbeddingResponse(data=data)


@app.get("/health")
async def health():
    return {"status": "ok", "model": MODEL_NAME, "device": device}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=18080)
