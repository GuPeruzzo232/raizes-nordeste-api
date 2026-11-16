package br.com.raizesdonordeste.api.repository;

import br.com.raizesdonordeste.api.domain.ContaFidelidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContaFidelidadeRepository extends JpaRepository<ContaFidelidade, Long> {
    Optional<ContaFidelidade> findByUsuarioId(Long usuarioId);
}
