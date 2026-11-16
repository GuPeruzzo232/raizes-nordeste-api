package br.com.raizesdonordeste.api.security;

import br.com.raizesdonordeste.api.domain.Perfil;

/** Dados do usuário extraídos do token JWT, usados para aplicar escopo por perfil e unidade. */
public record UsuarioAutenticado(Long id, String email, Perfil perfil, Long unidadeId) {

    public boolean is(Perfil... perfis) {
        for (Perfil p : perfis) {
            if (p == perfil) {
                return true;
            }
        }
        return false;
    }

    /** Matriz enxerga todas as unidades; demais perfis de loja só a sua. */
    public boolean podeAcessarUnidade(Long unidade) {
        return perfil == Perfil.MATRIZ || (unidadeId != null && unidadeId.equals(unidade));
    }
}
