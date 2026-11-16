package br.com.raizesdonordeste.api.repository;

import br.com.raizesdonordeste.api.domain.CanalPedido;
import br.com.raizesdonordeste.api.domain.Pedido;
import br.com.raizesdonordeste.api.domain.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByIdempotencyKey(String idempotencyKey);

    @Query("select p from Pedido p where (:unidadeId is null or p.unidade.id = :unidadeId) " +
            "and (:clienteId is null or p.cliente.id = :clienteId) " +
            "and (:status is null or p.status = :status) " +
            "and (:canal is null or p.canal = :canal)")
    Page<Pedido> pesquisar(@Param("unidadeId") Long unidadeId, @Param("clienteId") Long clienteId,
                           @Param("status") StatusPedido status, @Param("canal") CanalPedido canal, Pageable pageable);

    @Query("select p from Pedido p where p.unidade.id = :unidadeId and p.status in :status order by p.criadoEm")
    List<Pedido> filaDaUnidade(@Param("unidadeId") Long unidadeId, @Param("status") Collection<StatusPedido> status);

    @Query("select p.unidade.id, p.unidade.nome, p.canal, count(p), sum(p.total) from Pedido p " +
            "where p.status in :status and p.criadoEm between :inicio and :fim " +
            "and (:unidadeId is null or p.unidade.id = :unidadeId) " +
            "group by p.unidade.id, p.unidade.nome, p.canal order by p.unidade.nome, p.canal")
    List<Object[]> vendasPorUnidadeECanal(@Param("unidadeId") Long unidadeId, @Param("status") Collection<StatusPedido> status,
                                          @Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
