package br.com.raizesdonordeste.api.service;

import br.com.raizesdonordeste.api.domain.CanalPedido;
import br.com.raizesdonordeste.api.domain.Perfil;
import br.com.raizesdonordeste.api.domain.StatusPedido;
import br.com.raizesdonordeste.api.exception.AcessoNegadoException;
import br.com.raizesdonordeste.api.repository.PedidoRepository;
import br.com.raizesdonordeste.api.security.UsuarioAtual;
import br.com.raizesdonordeste.api.security.UsuarioAutenticado;
import br.com.raizesdonordeste.api.web.dto.VendaResumoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

/** Consolidado de vendas por unidade e canal. Matriz vê a rede; gerente só a própria loja. */
@Service
public class RelatorioService {

    private final PedidoRepository pedidos;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public RelatorioService(PedidoRepository pedidos, UsuarioAtual usuarioAtual, Clock clock) {
        this.pedidos = pedidos;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<VendaResumoResponse> vendas(Long unidadeId, LocalDate inicio, LocalDate fim) {
        UsuarioAutenticado u = usuarioAtual.obter();
        Long filtro = unidadeId;
        if (!u.is(Perfil.MATRIZ)) {
            if (unidadeId != null && !unidadeId.equals(u.unidadeId())) {
                throw new AcessoNegadoException("Gerente só consulta vendas da própria unidade");
            }
            filtro = u.unidadeId();
        }
        LocalDate de = inicio != null ? inicio : LocalDate.now(clock).withDayOfMonth(1);
        LocalDate ate = fim != null ? fim : LocalDate.now(clock);
        var status = EnumSet.of(StatusPedido.PAGO, StatusPedido.EM_PREPARO, StatusPedido.PRONTO, StatusPedido.ENTREGUE);
        return pedidos.vendasPorUnidadeECanal(filtro, status, de.atStartOfDay(clock.getZone()).toInstant(),
                        ate.plusDays(1).atStartOfDay(clock.getZone()).toInstant()).stream()
                .map(r -> new VendaResumoResponse((Long) r[0], (String) r[1], (CanalPedido) r[2], ((Number) r[3]).longValue(), new BigDecimal(String.valueOf(r[4]))))
                .toList();
    }
}
