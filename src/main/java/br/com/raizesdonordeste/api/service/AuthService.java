package br.com.raizesdonordeste.api.service;

import br.com.raizesdonordeste.api.domain.*;
import br.com.raizesdonordeste.api.exception.ConflitoException;
import br.com.raizesdonordeste.api.exception.CredenciaisInvalidasException;
import br.com.raizesdonordeste.api.exception.RegraNegocioException;
import br.com.raizesdonordeste.api.repository.ConsentimentoRepository;
import br.com.raizesdonordeste.api.repository.ContaFidelidadeRepository;
import br.com.raizesdonordeste.api.repository.UsuarioRepository;
import br.com.raizesdonordeste.api.security.TokenService;
import br.com.raizesdonordeste.api.web.dto.LoginRequest;
import br.com.raizesdonordeste.api.web.dto.RegistroClienteRequest;
import br.com.raizesdonordeste.api.web.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final ConsentimentoRepository consentimentos;
    private final ContaFidelidadeRepository contas;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final String versaoTermo;

    public AuthService(UsuarioRepository usuarios, ConsentimentoRepository consentimentos, ContaFidelidadeRepository contas,
                       PasswordEncoder encoder, TokenService tokenService, @Value("${app.lgpd.versao-termo:2026.1}") String versaoTermo) {
        this.usuarios = usuarios;
        this.consentimentos = consentimentos;
        this.contas = contas;
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.versaoTermo = versaoTermo;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Usuario u = usuarios.findByEmailIgnoreCase(req.email())
                .filter(Usuario::isAtivo)
                .filter(x -> encoder.matches(req.senha(), x.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        return new TokenResponse(tokenService.gerar(u), "Bearer", tokenService.getExpiracaoSegundos(), u.getId(),
                u.getPerfil().name(), u.getUnidade() != null ? u.getUnidade().getId() : null);
    }

    @Transactional
    public TokenResponse registrarCliente(RegistroClienteRequest req) {
        if (!Boolean.TRUE.equals(req.aceiteTermosUso())) {
            throw new RegraNegocioException("É necessário aceitar os termos de uso para criar a conta");
        }
        if (usuarios.existsByEmailIgnoreCase(req.email())) {
            throw new ConflitoException("Já existe uma conta com este e-mail");
        }
        Usuario u = new Usuario(req.nome(), req.email().toLowerCase(), encoder.encode(req.senha()), Perfil.CLIENTE, null);
        u.setCpf(req.cpf());
        u.setTelefone(req.telefone());
        usuarios.save(u);

        // Consentimento explícito por finalidade: o que não foi marcado é registrado como NÃO concedido.
        Map<FinalidadeConsentimento, Boolean> escolhas = req.consentimentos() == null ? Map.of() : req.consentimentos();
        for (FinalidadeConsentimento f : FinalidadeConsentimento.values()) {
            boolean concedido = Boolean.TRUE.equals(escolhas.get(f));
            consentimentos.save(new Consentimento(u, f, concedido, versaoTermo, req.canalOrigem()));
        }
        if (Boolean.TRUE.equals(escolhas.get(FinalidadeConsentimento.PROGRAMA_FIDELIDADE))) {
            contas.save(new ContaFidelidade(u));
        }
        return new TokenResponse(tokenService.gerar(u), "Bearer", tokenService.getExpiracaoSegundos(), u.getId(), u.getPerfil().name(), null);
    }
}
