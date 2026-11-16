package br.com.raizesdonordeste.api.repository;

import br.com.raizesdonordeste.api.domain.EventoPagamentoRecebido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoPagamentoRecebidoRepository extends JpaRepository<EventoPagamentoRecebido, Long> {
    boolean existsByEventoId(String eventoId);
}
