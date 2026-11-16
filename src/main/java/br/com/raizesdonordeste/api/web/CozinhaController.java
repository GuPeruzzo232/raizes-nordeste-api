package br.com.raizesdonordeste.api.web;

import br.com.raizesdonordeste.api.service.PedidoService;
import br.com.raizesdonordeste.api.web.dto.FilaCozinhaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cozinha")
@Tag(name = "Cozinha")
public class CozinhaController {

    private final PedidoService pedidos;

    public CozinhaController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping("/fila")
    @PreAuthorize("hasAnyRole('COZINHA','GERENTE','MATRIZ')")
    @Operation(summary = "Fila de preparo da unidade (sem dados pessoais nem valores)")
    public List<FilaCozinhaResponse> fila(@RequestParam(required = false) Long unidadeId) {
        return pedidos.filaCozinha(unidadeId);
    }
}
