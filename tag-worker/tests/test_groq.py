import json

import httpx
import pytest
import respx
from fastapi.testclient import TestClient
from pydantic import SecretStr

from app.config import Configuracao
from app.main import criar_app

URL = "https://groq.teste/v1/chat/completions"
PEDIDO = {"sku": "7894900011517", "descricao": "COCA COLA 2L PET"}


def resposta_llm(conteudo: dict | str) -> httpx.Response:
    texto = conteudo if isinstance(conteudo, str) else json.dumps(conteudo)
    return httpx.Response(200, json={"choices": [{"message": {"role": "assistant", "content": texto}}]})


@pytest.fixture
def cliente():
    configuracao = Configuracao(groq_api_key=SecretStr("gsk_teste"), groq_url=URL, _env_file=None)
    with TestClient(criar_app(configuracao)) as c:
        yield c


@respx.mock
def test_usa_o_llm_com_schema_estrito_e_normaliza_a_saida(cliente):
    rota = respx.post(URL).mock(return_value=resposta_llm(
        {"tags": ["Refrigerante", " refrigerante ", "Cola  2L", ""], "categoria": "bebidas"}))

    resposta = cliente.post("/classificar", json=PEDIDO)

    assert resposta.status_code == 200
    assert resposta.json() == {"tags": ["refrigerante", "cola 2l"], "categoria": "bebidas"}
    enviado = json.loads(rota.calls.last.request.content)
    assert rota.calls.last.request.headers["Authorization"] == "Bearer gsk_teste"
    assert enviado["temperature"] == 0
    assert enviado["response_format"]["json_schema"]["strict"] is True
    assert enviado["messages"][-1] == {"role": "user", "content": "COCA COLA 2L PET"}


@respx.mock
def test_categoria_fora_da_lista_vira_503(cliente):
    respx.post(URL).mock(return_value=resposta_llm({"tags": ["bebida"], "categoria": "refrigerantes premium"}))

    assert cliente.post("/classificar", json=PEDIDO).status_code == 503


@respx.mock
def test_json_invalido_do_llm_vira_503(cliente):
    respx.post(URL).mock(return_value=resposta_llm("isso não é json"))

    assert cliente.post("/classificar", json=PEDIDO).status_code == 503


@respx.mock
def test_limite_de_taxa_da_groq_vira_503(cliente):
    respx.post(URL).mock(return_value=httpx.Response(429, json={"error": {"message": "rate limit"}}))

    assert cliente.post("/classificar", json=PEDIDO).status_code == 503


@respx.mock
def test_timeout_da_groq_vira_503(cliente):
    respx.post(URL).mock(side_effect=httpx.ReadTimeout("lento"))

    assert cliente.post("/classificar", json=PEDIDO).status_code == 503
