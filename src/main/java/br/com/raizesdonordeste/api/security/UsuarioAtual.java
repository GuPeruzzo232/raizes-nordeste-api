package br.com.raizesdonordeste.api.security;

import br.com.raizesdonordeste.api.domain.Perfil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UsuarioAtual {

    public UsuarioAutenticado obter() {
        return talvez().orElseThrow(() -> new IllegalStateException("Nenhum usuário autenticado no contexto"));
    }

    public Optional<UsuarioAutenticado> talvez() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        Object unidade = jwt.getClaim("unidadeId");
        Long unidadeId = unidade instanceof Number n ? n.longValue() : null;
        return Optional.of(new UsuarioAutenticado(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                Perfil.valueOf(jwt.getClaimAsString("perfil")),
                unidadeId));
    }
}
