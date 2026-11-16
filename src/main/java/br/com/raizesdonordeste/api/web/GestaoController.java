package br.com.raizesdonordeste.api.web;

import br.com.raizesdonordeste.api.service.AuditoriaService;
import br.com.raizesdonordeste.api.service.RelatorioService;
import br.com.raizesdonordeste.api.web.dto.AuditoriaResponse;
import br.com.raizesdonordeste.api.web.dto.PaginaResponse;
import br.com.raizesdonordeste.api.web.dto.VendaResumoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Gestão: auditoria e relatórios")
@PreAuthorize("hasAnyRole('GERENTE','MATRIZ')")
public class GestaoController {

    private final AuditoriaService auditoria;
    private final RelatorioService relatorios;

    public GestaoController(AuditoriaService auditoria, RelatorioService relatorios) {
        this.auditoria = auditoria;
        this.relatorios = relatorios;
    }

    @GetMapping("/auditoria")
    @Operation(summary = "Trilha de auditoria (gerente: só a sua unidade)")
    public PaginaResponse<AuditoriaResponse> auditoria(@RequestParam(required = false) Long unidadeId,
                                                       @RequestParam(required = false) String acao,
                                                       @RequestParam(required = false) String entidade,
                                                       @RequestParam(required = false) Long entidadeId,
                                                       @RequestParam(defaultValue = "0") int pagina,
                                                       @RequestParam(defaultValue = "20") int tamanho) {
        return auditoria.pesquisar(unidadeId, acao, entidade, entidadeId, pagina, tamanho);
    }

    @GetMapping("/relatorios/vendas")
    @Operation(summary = "Vendas por unidade e canal no período")
    public List<VendaResumoResponse> vendas(@RequestParam(required = false) Long unidadeId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorios.vendas(unidadeId, inicio, fim);
    }
}
