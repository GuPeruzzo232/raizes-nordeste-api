package br.com.raizesdonordeste.api.web.dto;

import java.time.Instant;
import java.util.List;

public record ClienteResponse(Long id, String nome, String email, String cpf, String telefone, boolean anonimizado,
                              Instant criadoEm, Integer pontosFidelidade, List<ConsentimentoResponse> consentimentos) {
}
