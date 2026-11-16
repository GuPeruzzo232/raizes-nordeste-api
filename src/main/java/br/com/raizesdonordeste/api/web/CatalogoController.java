package br.com.raizesdonordeste.api.web;

import br.com.raizesdonordeste.api.service.CatalogoService;
import br.com.raizesdonordeste.api.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Unidades, cardápio e estoque")
public class CatalogoController {

    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping("/unidades")
    @Operation(summary = "Lista as unidades ativas (público)")
    public List<UnidadeResponse> unidades() {
        return service.listarUnidades();
    }

    @GetMapping("/unidades/{unidadeId}/cardapio")
    @Operation(summary = "Cardápio vigente da unidade (público). Itens sazonais só aparecem na vigência.")
    public List<ItemCardapioResponse> cardapio(@PathVariable Long unidadeId,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.cardapio(unidadeId, data);
    }

    @PutMapping("/unidades/{unidadeId}/cardapio")
    @PreAuthorize("hasAnyRole('GERENTE','MATRIZ')")
    @Operation(summary = "Inclui/atualiza item do cardápio da unidade (preço local, vigência)")
    public ItemCardapioResponse salvarItem(@PathVariable Long unidadeId, @Valid @RequestBody ItemCardapioRequest req) {
        return service.salvarItemCardapio(unidadeId, req);
    }

    @GetMapping("/unidades/{unidadeId}/estoque")
    @PreAuthorize("hasAnyRole('ATENDENTE','COZINHA','GERENTE','MATRIZ')")
    @Operation(summary = "Saldo de estoque da unidade")
    public List<EstoqueResponse> estoque(@PathVariable Long unidadeId) {
        return service.estoque(unidadeId);
    }

    @PutMapping("/unidades/{unidadeId}/estoque/{produtoId}")
    @PreAuthorize("hasAnyRole('GERENTE','MATRIZ')")
    @Operation(summary = "Ajuste manual de estoque (auditado)")
    public EstoqueResponse ajustarEstoque(@PathVariable Long unidadeId, @PathVariable Long produtoId,
                                          @Valid @RequestBody AjusteEstoqueRequest req) {
        return service.ajustarEstoque(unidadeId, produtoId, req);
    }

    @GetMapping("/produtos")
    @PreAuthorize("hasAnyRole('GERENTE','MATRIZ')")
    @Operation(summary = "Catálogo corporativo de produtos")
    public List<ProdutoResponse> produtos() {
        return service.listarProdutos();
    }

    @PostMapping("/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MATRIZ')")
    @Operation(summary = "Cadastra produto no catálogo corporativo (somente matriz)")
    public ProdutoResponse criarProduto(@Valid @RequestBody ProdutoRequest req) {
        return service.criarProduto(req);
    }
}
