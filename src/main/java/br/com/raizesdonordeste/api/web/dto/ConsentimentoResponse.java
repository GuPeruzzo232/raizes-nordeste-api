package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.FinalidadeConsentimento;

import java.time.Instant;

public record ConsentimentoResponse(FinalidadeConsentimento finalidade, boolean concedido, String versaoTermo, String canalOrigem, Instant registradoEm) {
}
