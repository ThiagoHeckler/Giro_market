#!/usr/bin/env bash
# Popula uma vitrine de demonstração usando só as APIs públicas dos serviços.
#   1. Entrada de lote no estoque (o tag-worker classifica cada produto).
#   2. Cadastro na vitrine: o produto entra com prateleira vazia e o próprio gatilho min/max
#      pede o primeiro lote ao estoque.
# Pão e sabão recebem lotes já vencidos: o estoque não os expede (FEFO), a vitrine mostra
# "Esgotado" e fica uma demanda reprimida — dê entrada num lote novo e veja voltar sozinho.
# Entra como operador nos dois serviços (OPERADOR_USUARIO/OPERADOR_SENHA, do ambiente ou do .env).
set -euo pipefail

RAIZ=$(cd "$(dirname "$0")/.." && pwd)
if [[ -f "$RAIZ/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$RAIZ/.env"
  set +a
fi
: "${OPERADOR_USUARIO:?defina OPERADOR_USUARIO no .env}"
: "${OPERADOR_SENHA:?defina OPERADOR_SENHA no .env}"

ESTOQUE=${ESTOQUE_URL:-http://localhost:${ESTOQUE_PORTA:-18081}}
MERCADO=${MERCADO_URL:-http://localhost:${MERCADO_PORTA:-18080}}
VALIDADE=$(date -d '+120 days' +%F)
VENCIDO=$(date -d '-1 day' +%F)

# Uma sessão (cookie jar) por serviço: cada um tem o seu cookie de sessão e de token CSRF.
SESSOES=$(mktemp -d)
trap 'rm -rf "$SESSOES"' EXIT

csrf() { # csrf JAR COOKIE -> valor do token CSRF guardado no cookie jar
  awk -F'\t' -v nome="$2" '$6 == nome { print $7 }' "$1"
}

entrar() { # entrar URL JAR COOKIE_CSRF
  curl -s -o /dev/null -c "$2" -b "$2" "$1/sessao"   # entrega o cookie do token CSRF
  local status
  status=$(curl -s -o /dev/null -w '%{http_code}' -c "$2" -b "$2" -X POST "$1/sessao" \
    -H "X-XSRF-TOKEN: $(csrf "$2" "$3")" \
    --data-urlencode "usuario=$OPERADOR_USUARIO" --data-urlencode "senha=$OPERADOR_SENHA")
  if [[ "$status" != 204 ]]; then
    echo "Login de operador em $1 falhou (HTTP $status). Confira OPERADOR_USUARIO/OPERADOR_SENHA." >&2
    exit 1
  fi
  curl -s -o /dev/null -c "$2" -b "$2" "$1/sessao"   # o login troca o token: busca o novo
}

post() { # post URL JAR COOKIE_CSRF JSON -> imprime o status HTTP
  curl -s -o /dev/null -w '%{http_code}' -c "$2" -b "$2" -X POST "$1" \
    -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(csrf "$2" "$3")" -d "$4"
}

entrar "$ESTOQUE" "$SESSOES/estoque" XSRF-ESTOQUE
entrar "$MERCADO" "$SESSOES/mercado" XSRF-MERCADO

# sku | descrição no estoque | ncm | nome na vitrine | preço | mínimo | ideal | lote | validade
PRODUTOS=(
  "7894900011517|COCA COLA 2L PET|22021000|Coca-Cola 2L PET|8.99|6|24|L-COCA-01|$VALIDADE"
  "2000000000114|BANANA PRATA KG|08039000|Banana Prata (kg)|5.49|5|20|L-BANA-01|$VALIDADE"
  "7891022100105|DETERGENTE NEUTRO 500ML|34022000|Detergente Neutro 500ml|2.79|10|40|L-DETE-01|$VALIDADE"
  "7891962047205|PAO DE FORMA INTEGRAL 400G|19059090|Pão de Forma Integral|7.90|4|12|L-PAO-01|$VENCIDO"
  "7896005800027|CAFE TORRADO E MOIDO 500G|09012100|Café Torrado e Moído 500g|15.90|5|20|L-CAFE-01|$VALIDADE"
  "7891000100103|LEITE UHT INTEGRAL 1L|04012010|Leite Integral 1L|4.59|12|48|L-LEIT-01|$VALIDADE"
  "7896006716112|ARROZ BRANCO TIPO 1 5KG|10063021|Arroz Branco 5kg|24.90|4|16|L-ARRO-01|$VALIDADE"
  "7891150061729|SABAO EM PO 1KG|34022000|Sabão em Pó 1kg|12.49|4|12|L-SABA-01|$VENCIDO"
)

echo "Estoque: entrada de lotes (classificação via tag-worker)"
for linha in "${PRODUTOS[@]}"; do
  IFS='|' read -r sku descricao ncm _ _ _ _ lote validade <<< "$linha"
  status=$(post "$ESTOQUE/lotes" "$SESSOES/estoque" XSRF-ESTOQUE "{\"sku\":\"$sku\",\"descricao\":\"$descricao\",\"ncm\":\"$ncm\",\"codigoLote\":\"$lote\",\"quantidade\":60,\"validade\":\"$validade\"}")
  printf '  %-28s lote %-10s → HTTP %s\n' "$descricao" "$lote" "$status"
done

echo "Mercado: cadastro na vitrine (o gatilho pede o primeiro lote)"
for linha in "${PRODUTOS[@]}"; do
  IFS='|' read -r sku _ _ nome preco minimo ideal _ _ <<< "$linha"
  status=$(post "$MERCADO/produtos" "$SESSOES/mercado" XSRF-MERCADO "{\"sku\":\"$sku\",\"nome\":\"$nome\",\"preco\":$preco,\"estoqueMinimo\":$minimo,\"estoqueIdeal\":$ideal}")
  printf '  %-28s → HTTP %s\n' "$nome" "$status"
done

echo "Pronto. Em alguns segundos as prateleiras enchem: $MERCADO/produtos"
