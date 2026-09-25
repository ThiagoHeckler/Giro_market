from fastapi.testclient import TestClient

from app.config import Configuracao
from app.main import criar_app


def cliente_sem_llm() -> TestClient:
    return TestClient(criar_app(Configuracao(groq_api_key=None, _env_file=None)))


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
    assert Configuracao(groq_api_key="", _env_file=None).groq_api_key is None
    assert Configuracao(groq_api_key="   ", _env_file=None).groq_api_key is None
