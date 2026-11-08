"""tag-worker: classifica tags e categoria de produtos. Fica fora do caminho crítico da venda."""

import logging
import secrets
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import httpx
from fastapi import Depends, FastAPI, HTTPException, Request

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
    """Fábrica do app (uvicorn --factory): a configuração só é lida ao subir, nunca no import."""
    configuracao = configuracao or Configuracao()
    token_esperado = configuracao.tag_worker_token_servico.get_secret_value().encode()

    def exigir_token_servico(request: Request) -> None:
        """Authorization: Bearer <token do worker>, comparado em tempo constante. 401 sem WWW-Authenticate."""
        esquema, _, recebido = request.headers.get("authorization", "").partition(" ")
        if esquema.lower() != "bearer" or not secrets.compare_digest(recebido.strip().encode(), token_esperado):
            log.warning("token de serviço inválido caminho=%s origem=%s",
                        request.url.path, request.client.host if request.client else "?")
            raise HTTPException(status_code=401, detail="token de serviço ausente ou inválido")

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

    # /saude fica aberta: é o healthcheck do compose e não expõe nada.
    @app.post("/classificar", response_model=Classificacao, dependencies=[Depends(exigir_token_servico)])
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
