package br.com.raizesdonordeste.api.repository;

import br.com.raizesdonordeste.api.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
