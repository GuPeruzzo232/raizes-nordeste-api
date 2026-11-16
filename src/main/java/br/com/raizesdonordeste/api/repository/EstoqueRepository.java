package br.com.raizesdonordeste.api.repository;

import br.com.raizesdonordeste.api.domain.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    @Query("select e from Estoque e where e.unidade.id = :unidadeId and e.produto.id = :produtoId")
    Optional<Estoque> findByUnidadeAndProduto(@Param("unidadeId") Long unidadeId, @Param("produtoId") Long produtoId);

    @Query("select e from Estoque e join fetch e.produto where e.unidade.id = :unidadeId order by e.produto.nome")
    List<Estoque> findByUnidade(@Param("unidadeId") Long unidadeId);

    /**
     * Baixa atômica: o UPDATE só acontece se houver saldo suficiente. Em pico de demanda,
     * dois pedidos simultâneos nunca deixam o estoque negativo (o banco serializa a linha).
     * Retorna 1 se baixou, 0 se não havia saldo.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Estoque e set e.quantidade = e.quantidade - :qtd, e.atualizadoEm = :agora " +
            "where e.unidade.id = :unidadeId and e.produto.id = :produtoId and e.quantidade >= :qtd")
    int baixar(@Param("unidadeId") Long unidadeId, @Param("produtoId") Long produtoId, @Param("qtd") int qtd, @Param("agora") Instant agora);

    @Modifying(flushAutomatically = true)
    @Query("update Estoque e set e.quantidade = e.quantidade + :qtd, e.atualizadoEm = :agora " +
            "where e.unidade.id = :unidadeId and e.produto.id = :produtoId")
    int repor(@Param("unidadeId") Long unidadeId, @Param("produtoId") Long produtoId, @Param("qtd") int qtd, @Param("agora") Instant agora);
}
