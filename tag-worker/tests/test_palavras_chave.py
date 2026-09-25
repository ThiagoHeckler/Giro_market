import asyncio

from app.classificadores import ClassificadorPalavrasChave
from app.modelos import Categoria


def classificar(descricao: str):
    return asyncio.run(ClassificadorPalavrasChave().classificar("7894900011517", descricao))


def test_descricao_conhecida_cai_na_categoria_certa():
    assert classificar("LEITE INTEGRAL 1L").categoria == Categoria.LATICINIOS
    assert classificar("DETERGENTE NEUTRO 500ML").categoria == Categoria.LIMPEZA


def test_descricao_desconhecida_cai_em_outros():
    resultado = classificar("PARAFUSO SEXTAVADO")

    assert resultado.categoria == Categoria.OUTROS
    assert resultado.tags == ["parafuso", "sextavado"]


def test_mesma_entrada_mesma_saida():
    assert classificar("ARROZ TIPO 1 5KG") == classificar("ARROZ TIPO 1 5KG")
