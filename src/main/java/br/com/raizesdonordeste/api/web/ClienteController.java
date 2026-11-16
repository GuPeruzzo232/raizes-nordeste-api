package br.com.raizesdonordeste.api.web;

import br.com.raizesdonordeste.api.service.ClienteService;
import br.com.raizesdonordeste.api.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes, fidelidade e LGPD")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Dados do próprio titular, com histórico de consentimentos")
    public ClienteResponse meusDados() {
        return service.meusDados();
    }

    @GetMapping("/me/fidelidade")
    @PreAuthorize("hasRole('CLIENTE')")
    public FidelidadeResponse fidelidade() {
        return service.minhaFidelidade();
    }

    @PostMapping("/me/consentimentos")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Concede ou revoga consentimento para uma finalidade (gera nova linha de histórico)")
    public List<ConsentimentoResponse> consentimento(@Valid @RequestBody ConsentimentoRequest req) {
        return service.registrarConsentimento(req);
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Anonimiza a conta do titular (direito à eliminação, LGPD art. 18)")
    public void anonimizar() {
        service.anonimizarMinhaConta();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE','MATRIZ')")
    @Operation(summary = "Consulta dados de cliente por funcionário (acesso registrado na auditoria)")
    public ClienteResponse consultar(@PathVariable Long id, @RequestParam(required = false) String finalidade) {
        return service.consultarPorFuncionario(id, finalidade);
    }
}
