package br.com.raizesdonordeste.api.repository;

import br.com.raizesdonordeste.api.domain.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {
    List<Unidade> findByAtivaTrueOrderByNome();
}
