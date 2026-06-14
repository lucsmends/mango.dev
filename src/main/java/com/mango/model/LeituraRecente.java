package com.mango.model;

/** Entrada de histórico / "continuar lendo" (UC3), com dados para exibição. */
public record LeituraRecente(
        String mangaId,
        String capituloId,
        String titulo,
        String capaUrl,
        String capNumero,
        int paginaAtual,
        int totalPaginas,
        boolean concluido) {

    public String tituloExibicao() {
        return (titulo == null || titulo.isBlank()) ? "(sem título)" : titulo;
    }

    /** Ex.: "Cap. 12 · pág. 3/20" ou "Cap. 12 · concluído". */
    public String legenda() {
        final String cap = "Cap. " + (capNumero == null || capNumero.isBlank() ? "—" : capNumero);
        if (concluido) {
            return cap + " · concluído";
        }
        return cap + " · pág. " + (paginaAtual + 1) + "/" + Math.max(totalPaginas, paginaAtual + 1);
    }
}
