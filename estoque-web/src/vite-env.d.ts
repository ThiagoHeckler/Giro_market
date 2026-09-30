interface ImportMetaEnv {
  /** Endereço da vitrine para o link "Ver a vitrine". */
  readonly VITE_VITRINE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
