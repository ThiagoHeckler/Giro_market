#!/usr/bin/env bash
# Popula uma vitrine de demonstração usando só as APIs públicas dos serviços.
#   1. Entrada de lote no estoque (o tag-worker classifica cada produto).
#   2. Cadastro na vitrine: o produto entra com prateleira vazia e o próprio gatilho min/max
#      pede o primeiro lote ao estoque.
# Pão e sabão recebem lotes já vencidos: o estoque não os expede (FEFO), a vitrine mostra
# "Esgotado" e fica uma demanda reprimida — dê entrada num lote novo e veja voltar sozinho.
set -euo pipefail

ESTOQUE=${ESTOQUE_URL:-http://localhost:18081}
MERCADO=${MERCADO_URL:-http://localhost:18080}
VALIDADE=$(date -d '+120 days' +%F)
VENCIDO=$(date -d '-1 day' +%F)

post() { # post URL JSON -> imprime o status HTTP
  curl -s -o /dev/null -w '%{http_code}' -X POST "$1" -H 'Content-Type: application/json' -d "$2"
}

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
  status=$(post "$ESTOQUE/lotes" "{\"sku\":\"$sku\",\"descricao\":\"$descricao\",\"ncm\":\"$ncm\",\"codigoLote\":\"$lote\",\"quantidade\":60,\"validade\":\"$validade\"}")
  printf '  %-28s lote %-10s → HTTP %s\n' "$descricao" "$lote" "$status"
done

echo "Mercado: cadastro na vitrine (o gatilho pede o primeiro lote)"
for linha in "${PRODUTOS[@]}"; do
  IFS='|' read -r sku _ _ nome preco minimo ideal _ _ <<< "$linha"
  status=$(post "$MERCADO/produtos" "{\"sku\":\"$sku\",\"nome\":\"$nome\",\"preco\":$preco,\"estoqueMinimo\":$minimo,\"estoqueIdeal\":$ideal}")
  printf '  %-28s → HTTP %s\n' "$nome" "$status"
done

echo "Pronto. Em alguns segundos as prateleiras enchem: $MERCADO/produtos"
