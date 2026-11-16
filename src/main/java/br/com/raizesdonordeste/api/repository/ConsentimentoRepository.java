package br.com.raizesdonordeste.api.repository;

import br.com.raizesdonordeste.api.domain.Consentimento;
import br.com.raizesdonordeste.api.domain.FinalidadeConsentimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentimentoRepository extends JpaRepository<Consentimento, Long> {
    List<Consentimento> findByUsuarioIdOrderByRegistradoEmDescIdDesc(Long usuarioId);
    Optional<Consentimento> findFirstByUsuarioIdAndFinalidadeOrderByRegistradoEmDescIdDesc(Long usuarioId, FinalidadeConsentimento finalidade);
}
