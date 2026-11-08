import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from app.config import Configuracao
from app.main import criar_app
from tests.conftest import AUTORIZADO, PEDIDO_COCA, configuracao


def cliente_sem_llm(headers: dict[str, str] | None = AUTORIZADO) -> TestClient:
    return TestClient(criar_app(configuracao(groq_api_key=None)), headers=headers)


def test_classifica_com_palavras_chave_quando_nao_ha_chave():
    with cliente_sem_llm() as cliente:
        resposta = cliente.post("/classificar", json={"sku": "7894900011517", "descricao": "COCA COLA 2L PET"})

    assert resposta.status_code == 200
    assert resposta.json() == {"tags": ["coca", "cola", "2l", "pet"], "categoria": "bebidas"}


def test_rejeita_sku_fora_do_padrao_gtin():
    with cliente_sem_llm() as cliente:
        resposta = cliente.post("/classificar", json={"sku": "COCA-2L", "descricao": "COCA COLA 2L PET"})

    assert resposta.status_code == 422


def test_rejeita_descricao_vazia():
    with cliente_sem_llm() as cliente:
        resposta = cliente.post("/classificar", json={"sku": "7894900011517", "descricao": ""})

    assert resposta.status_code == 422


def test_saude():
    with cliente_sem_llm() as cliente:
        assert cliente.get("/saude").json() == {"status": "ok"}


def test_chave_vazia_conta_como_ausente():
    assert configuracao(groq_api_key="").groq_api_key is None
    assert configuracao(groq_api_key="   ").groq_api_key is None


@pytest.mark.parametrize("autorizacao", [None, "Bearer token-errado", "Basic dXNlcjpzZW5oYQ==", "Bearer"])
def test_classificar_exige_o_token_de_servico(autorizacao):
    headers = {"Authorization": autorizacao} if autorizacao else None
    with cliente_sem_llm(headers=headers) as cliente:
        resposta = cliente.post("/classificar", json=PEDIDO_COCA)

    assert resposta.status_code == 401
    assert "www-authenticate" not in resposta.headers


def test_saude_nao_exige_token():
    with cliente_sem_llm(headers=None) as cliente:
        assert cliente.get("/saude").status_code == 200


@pytest.mark.parametrize("token", [None, "", "curto-demais"])
def test_nao_sobe_sem_token_forte(token, monkeypatch):
    monkeypatch.delenv("TAG_WORKER_TOKEN_SERVICO", raising=False)
    valores = {} if token is None else {"tag_worker_token_servico": token}
    with pytest.raises(ValidationError, match="(?i)tag_worker_token_servico"):
        Configuracao(_env_file=None, **valores)
