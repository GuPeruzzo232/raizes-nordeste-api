package br.com.raizesdonordeste.api.web.dto;

import br.com.raizesdonordeste.api.domain.FinalidadeConsentimento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Cadastro de cliente. Os consentimentos são explícitos: cada finalidade vem marcada
 * true/false pelo próprio titular; nada é presumido.
 */
public record RegistroClienteRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72) String senha,
        @Pattern(regexp = "\\d{11}", message = "deve conter 11 dígitos") String cpf,
        @Size(max = 20) String telefone,
        @NotNull Boolean aceiteTermosUso,
        Map<FinalidadeConsentimento, Boolean> consentimentos,
        String canalOrigem) {
}
