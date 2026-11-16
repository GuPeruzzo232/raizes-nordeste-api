package br.com.raizesdonordeste.api.web.dto;

public record TokenResponse(String token, String tipo, long expiraEmSegundos, Long usuarioId, String perfil, Long unidadeId) {
}
