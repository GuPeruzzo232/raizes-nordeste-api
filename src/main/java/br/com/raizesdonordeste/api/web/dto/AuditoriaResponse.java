package br.com.raizesdonordeste.api.web.dto;

import java.time.Instant;

public record AuditoriaResponse(Long id, Instant dataHora, Long usuarioId, String usuarioEmail, String perfil, Long unidadeId,
                                String acao, String entidade, Long entidadeId, String detalhes, String ip) {
}
