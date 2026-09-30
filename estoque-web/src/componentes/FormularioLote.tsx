import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, type FormEvent } from "react";
import { ErroApi, estoque, SKU_VALIDO, type EntradaLote, type LoteRegistrado } from "../api/estoque";
import { MarcaGiro } from "./Logo";

const VAZIO = { sku: "", descricao: "", ncm: "", codigoLote: "", quantidade: "", validade: "" };

/** Espera o operador parar de digitar antes de consultar o SKU. */
function useAtrasado(valor: string, ms = 300) {
  const [atrasado, setAtrasado] = useState(valor);
  useEffect(() => {
    const espera = setTimeout(() => setAtrasado(valor), ms);
    return () => clearTimeout(espera);
  }, [valor, ms]);
  return atrasado;
}

function mensagemDeSucesso(codigoLote: string, registrado: LoteRegistrado) {
  const partes = [`Lote ${codigoLote} registrado. Saldo do SKU: ${registrado.saldoDisponivel}.`];
  if (registrado.demandasAtendidas > 0) {
    partes.push(registrado.demandasAtendidas === 1
      ? "1 demanda reprimida atendida."
      : `${registrado.demandasAtendidas} demandas reprimidas atendidas.`);
  }
  if (!registrado.classificado) {
    partes.push("Tags pendentes: o produto será reclassificado depois.");
  }
  return partes.join(" ");
}

export function FormularioLote() {
  const [campos, setCampos] = useState(VAZIO);
  const [sucesso, setSucesso] = useState<string | null>(null);
  const consultas = useQueryClient();

  const skuDigitado = campos.sku.trim();
  const skuConsultado = useAtrasado(skuDigitado);
  const skuValido = SKU_VALIDO.test(skuConsultado);
  const produto = useQuery({
    queryKey: ["produto", skuConsultado],
    queryFn: () => estoque.buscarProduto(skuConsultado),
    enabled: skuValido,
  });
  const consultaEmDia = skuValido && skuConsultado === skuDigitado;
  // SKU novo (404) ou consulta falhou: os campos de cadastro aparecem e o backend decide.
  const pedeCadastro = consultaEmDia && (produto.data === null || produto.isError);

  const registrar = useMutation({
    mutationFn: estoque.registrarLote,
    onSuccess: (registrado, entrada) => {
      setSucesso(mensagemDeSucesso(entrada.codigoLote, registrado));
      setCampos(VAZIO);
      consultas.invalidateQueries({ queryKey: ["painel"] });
      consultas.invalidateQueries({ queryKey: ["produto", entrada.sku] });
    },
  });

  function alterar(campo: keyof typeof VAZIO, valor: string) {
    setCampos((atuais) => ({ ...atuais, [campo]: valor }));
    setSucesso(null);
    registrar.reset();
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    const entrada: EntradaLote = {
      sku: skuDigitado,
      codigoLote: campos.codigoLote.trim(),
      quantidade: Number(campos.quantidade),
      ...(campos.validade ? { validade: campos.validade } : {}),
      ...(pedeCadastro ? { descricao: campos.descricao.trim(), ncm: campos.ncm.trim() } : {}),
    };
    registrar.mutate(entrada);
  }

  return (
    <form className="cartao formulario-lote" aria-labelledby="titulo-entrada" id="entrada" onSubmit={enviar}>
      <h2 id="titulo-entrada" className="cartao__titulo">Entrada de lote</h2>

      <div className="campo">
        <label htmlFor="sku">SKU / EAN</label>
        <input
          id="sku"
          className="mono"
          inputMode="numeric"
          autoComplete="off"
          required
          pattern="\d{8}|\d{12,14}"
          value={campos.sku}
          onChange={(e) => alterar("sku", e.target.value)}
          aria-describedby="sku-produto"
        />
        <span id="sku-produto" className="campo__ajuda" aria-live="polite">
          {skuDigitado && !SKU_VALIDO.test(skuDigitado) && "EAN-8 ou GTIN de 12 a 14 dígitos."}
          {consultaEmDia && produto.isFetching && "Procurando…"}
          {consultaEmDia && produto.data && (
            <span className="campo__ajuda--ok">
              {produto.data.descricao} · saldo {produto.data.saldoDisponivel}
            </span>
          )}
          {consultaEmDia && produto.data === null && "SKU novo: informe descrição e NCM."}
          {consultaEmDia && produto.isError && "Não foi possível consultar o SKU."}
        </span>
      </div>

      {pedeCadastro && (
        <>
          <div className="campo">
            <label htmlFor="descricao">Descrição</label>
            <input
              id="descricao"
              required={produto.data === null}
              maxLength={255}
              value={campos.descricao}
              onChange={(e) => alterar("descricao", e.target.value)}
            />
          </div>
          <div className="campo">
            <label htmlFor="ncm">NCM</label>
            <input
              id="ncm"
              className="mono"
              inputMode="numeric"
              required={produto.data === null}
              pattern="\d{8}"
              value={campos.ncm}
              onChange={(e) => alterar("ncm", e.target.value)}
            />
          </div>
        </>
      )}

      <div className="campos-lado-a-lado">
        <div className="campo">
          <label htmlFor="quantidade">Quantidade</label>
          <input
            id="quantidade"
            className="mono"
            type="number"
            min={1}
            step={1}
            required
            value={campos.quantidade}
            onChange={(e) => alterar("quantidade", e.target.value)}
          />
        </div>
        <div className="campo">
          <label htmlFor="codigo-lote">Código do lote</label>
          <input
            id="codigo-lote"
            className="mono"
            required
            maxLength={50}
            value={campos.codigoLote}
            onChange={(e) => alterar("codigoLote", e.target.value)}
          />
        </div>
      </div>

      <div className="campo">
        <label htmlFor="validade">
          Validade <span className="campo__opcional">(opcional)</span>
        </label>
        <input
          id="validade"
          className="mono"
          type="date"
          value={campos.validade}
          onChange={(e) => alterar("validade", e.target.value)}
        />
      </div>

      <button type="submit" className="botao botao--primario botao--grande" disabled={registrar.isPending}>
        {registrar.isPending ? "Registrando…" : "Dar entrada no estoque"}
      </button>

      {sucesso && <p className="aviso aviso--ok" role="status">{sucesso}</p>}
      {registrar.isError && (
        <p className="aviso aviso--erro" role="alert">
          {registrar.error instanceof ErroApi && registrar.error.problema.detail
            ? registrar.error.problema.detail
            : "Não foi possível registrar o lote. Tente de novo."}
        </p>
      )}

      <div className="nota">
        <MarcaGiro tamanho={16} />
        <span>
          Ao dar entrada, o Girô classifica as tags do produto e atende a demanda reprimida deste SKU
          automaticamente.
        </span>
      </div>
    </form>
  );
}
