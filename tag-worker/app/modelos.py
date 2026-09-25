"""Contrato HTTP do worker. Tags só posicionam o produto na vitrine — nunca são identidade."""

import re
import unicodedata
from enum import StrEnum
from typing import Annotated

from pydantic import BaseModel, Field, field_validator

MAXIMO_TAGS = 8
TAMANHO_MAXIMO_TAG = 30


class Categoria(StrEnum):
    BEBIDAS = "bebidas"
    LATICINIOS = "laticinios"
    MERCEARIA = "mercearia"
    HORTIFRUTI = "hortifruti"
    PADARIA = "padaria"
    CARNES = "carnes"
    CONGELADOS = "congelados"
    LIMPEZA = "limpeza"
    HIGIENE = "higiene"
    PET = "pet"
    OUTROS = "outros"


class PedidoClassificacao(BaseModel):
    sku: Annotated[str, Field(pattern=r"^([0-9]{8}|[0-9]{12,14})$")]
    descricao: Annotated[str, Field(min_length=1, max_length=255)]


class Classificacao(BaseModel):
    tags: list[str]
    categoria: Categoria

    @field_validator("tags")
    @classmethod
    def normalizar(cls, tags: list[str]) -> list[str]:
        """Minúsculas, espaços colapsados, sem vazias nem repetidas, no máximo MAXIMO_TAGS."""
        vistas: list[str] = []
        for tag in tags:
            limpa = re.sub(r"\s+", " ", unicodedata.normalize("NFC", tag)).strip().lower()
            if limpa and len(limpa) <= TAMANHO_MAXIMO_TAG and limpa not in vistas:
                vistas.append(limpa)
        if not vistas:
            raise ValueError("classificação sem nenhuma tag válida")
        return vistas[:MAXIMO_TAGS]
