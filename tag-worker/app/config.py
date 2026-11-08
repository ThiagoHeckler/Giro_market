"""Configuração via variáveis de ambiente (e .env em desenvolvimento)."""

from pydantic import SecretStr, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


TAMANHO_MINIMO_TOKEN = 32


class Configuracao(BaseSettings):
    model_config = SettingsConfigDict(env_file=(".env", "../.env"), extra="ignore")

    # Token que o worker exige em Authorization: Bearer (o estoque manda). Sem ele o worker não sobe.
    tag_worker_token_servico: SecretStr

    # Sem chave, o worker usa o classificador determinístico por palavras-chave.
    groq_api_key: SecretStr | None = None
    groq_modelo: str = "openai/gpt-oss-20b"
    groq_url: str = "https://api.groq.com/openai/v1/chat/completions"
    # Menor que o timeout do estoque (2s): o worker desiste antes de quem o chamou.
    groq_timeout_segundos: float = 1.5

    @field_validator("groq_api_key", mode="before")
    @classmethod
    def vazia_eh_ausente(cls, valor: object) -> object:
        """No compose, variável não definida chega como string vazia — e SecretStr("") seria truthy."""
        return None if isinstance(valor, str) and not valor.strip() else valor

    @field_validator("tag_worker_token_servico")
    @classmethod
    def token_forte(cls, valor: SecretStr) -> SecretStr:
        if len(valor.get_secret_value().strip()) < TAMANHO_MINIMO_TOKEN:
            raise ValueError(
                f"TAG_WORKER_TOKEN_SERVICO precisa de ao menos {TAMANHO_MINIMO_TOKEN} caracteres "
                "(gere com: openssl rand -hex 32)")
        return valor
