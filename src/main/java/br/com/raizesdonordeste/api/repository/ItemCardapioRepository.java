package br.com.raizesdonordeste.api.repository;

import br.com.raizesdonordeste.api.domain.ItemCardapio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemCardapioRepository extends JpaRepository<ItemCardapio, Long> {

    @Query("select i from ItemCardapio i join fetch i.produto where i.unidade.id = :unidadeId order by i.produto.categoria, i.produto.nome")
    List<ItemCardapio> findByUnidadeComProduto(@Param("unidadeId") Long unidadeId);

    @Query("select i from ItemCardapio i join fetch i.produto where i.unidade.id = :unidadeId and i.produto.id = :produtoId")
    Optional<ItemCardapio> findByUnidadeAndProduto(@Param("unidadeId") Long unidadeId, @Param("produtoId") Long produtoId);
}
