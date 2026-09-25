"""tag-worker: classifica tags e categoria de produtos. Fica fora do caminho crítico da venda."""

import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import httpx
from fastapi import FastAPI, HTTPException, Request

from app.classificadores import (
    ClassificacaoIndisponivel,
    Classificador,
    ClassificadorGroq,
    ClassificadorPalavrasChave,
)
from app.config import Configuracao
from app.modelos import Classificacao, PedidoClassificacao

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
log = logging.getLogger("tag_worker")


def criar_app(configuracao: Configuracao | None = None) -> FastAPI:
    configuracao = configuracao or Configuracao()

    @asynccontextmanager
    async def ciclo_de_vida(app: FastAPI) -> AsyncIterator[None]:
        async with httpx.AsyncClient() as cliente:
            if configuracao.groq_api_key:
                app.state.classificador = ClassificadorGroq(
                    cliente, configuracao.groq_url, configuracao.groq_api_key,
                    configuracao.groq_modelo, configuracao.groq_timeout_segundos)
                log.info("classificador=groq modelo=%s", configuracao.groq_modelo)
            else:
                app.state.classificador = ClassificadorPalavrasChave()
                log.info("classificador=palavras-chave (GROQ_API_KEY ausente)")
            yield

    app = FastAPI(title="tag-worker", version="0.1.0", lifespan=ciclo_de_vida)

    @app.post("/classificar", response_model=Classificacao)
    async def classificar(pedido: PedidoClassificacao, request: Request) -> Classificacao:
        classificador: Classificador = request.app.state.classificador
        try:
            classificacao = await classificador.classificar(pedido.sku, pedido.descricao)
        except ClassificacaoIndisponivel as e:
            raise HTTPException(status_code=503, detail=str(e)) from e
        log.info("classificado sku=%s categoria=%s tags=%s", pedido.sku, classificacao.categoria, classificacao.tags)
        return classificacao

    @app.get("/saude")
    async def saude() -> dict[str, str]:
        return {"status": "ok"}

    return app


app = criar_app()
