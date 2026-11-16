package br.com.raizesdonordeste.api.web;

import br.com.raizesdonordeste.api.service.AuthService;
import br.com.raizesdonordeste.api.web.dto.LoginRequest;
import br.com.raizesdonordeste.api.web.dto.RegistroClienteRequest;
import br.com.raizesdonordeste.api.web.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve um token JWT")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastro de cliente com consentimentos LGPD explícitos")
    public TokenResponse registrar(@Valid @RequestBody RegistroClienteRequest req) {
        return service.registrarCliente(req);
    }
}
