package br.com.raizesdonordeste.api.repository;

import br.com.raizesdonordeste.api.domain.Pagamento;
import br.com.raizesdonordeste.api.domain.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    Optional<Pagamento> findByIdempotencyKey(String idempotencyKey);
    Optional<Pagamento> findByTransacaoExterna(String transacaoExterna);
    List<Pagamento> findByPedidoIdOrderByIdDesc(Long pedidoId);
    boolean existsByPedidoIdAndStatus(Long pedidoId, StatusPagamento status);
    List<Pagamento> findTop50ByStatusAndTentativasLessThanOrderByAtualizadoEm(StatusPagamento status, int maxTentativas);
}
