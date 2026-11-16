package br.com.raizesdonordeste.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class TratadorGlobalDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> regra(RegraNegocioException e, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResposta> conflito(ConflitoException e, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), req, List.of());
    }

    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    public ResponseEntity<ErroResposta> acessoNegado(RuntimeException e, HttpServletRequest req) {
        String msg = e instanceof AcessoNegadoException ? e.getMessage() : "Seu perfil não tem permissão para esta operação";
        return resposta(HttpStatus.FORBIDDEN, msg, req, List.of());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResposta> credenciais(CredenciaisInvalidasException e, HttpServletRequest req) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados de entrada inválidos", req, detalhes);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class})
    public ResponseEntity<ErroResposta> requisicaoMalFormada(Exception e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição mal formada ou com valor inválido", req, List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErroResposta> concorrencia(OptimisticLockingFailureException e, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, "O registro foi alterado por outro usuário. Recarregue e tente novamente.", req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException e, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, "Operação viola uma restrição de integridade (registro duplicado ou referência inválida)", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> inesperado(Exception e, HttpServletRequest req) {
        if (e instanceof ErrorResponse er) { // 404 de rota, 405 de método etc. já trazem o status correto
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            return resposta(status, status.getReasonPhrase(), req, List.of());
        }
        log.error("Erro inesperado em {}", req.getRequestURI(), e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde.", req, List.of());
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem, HttpServletRequest req, List<String> detalhes) {
        return ResponseEntity.status(status)
                .body(new ErroResposta(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, req.getRequestURI(), detalhes));
    }
}
