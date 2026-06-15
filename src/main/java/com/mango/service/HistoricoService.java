package com.mango.service;

import com.mango.db.ProgressoRepository;
import com.mango.model.LeituraRecente;

import java.util.List;

/** Consulta de histórico e "continuar lendo" (UC3). */
public class HistoricoService {

    private final ProgressoRepository progresso = new ProgressoRepository();

    public List<LeituraRecente> historico() {
        return progresso.listarHistorico();
    }

    public List<LeituraRecente> continuarLendo() {
        return progresso.listarEmAndamento();
    }
}
