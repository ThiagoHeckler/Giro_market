from app.config import Configuracao

TOKEN = "token-do-worker-nos-testes-0123456789abcdef"
AUTORIZADO = {"Authorization": f"Bearer {TOKEN}"}


def configuracao(**valores) -> Configuracao:
    """Configuração de teste: nada lido do .env, token fixo."""
    return Configuracao(_env_file=None, **{"tag_worker_token_servico": TOKEN, **valores})

PEDIDO_COCA = {"sku": "7894900011517", "descricao": "COCA COLA 2L PET"}
