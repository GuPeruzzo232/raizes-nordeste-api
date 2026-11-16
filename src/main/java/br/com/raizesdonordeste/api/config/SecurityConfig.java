package br.com.raizesdonordeste.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import br.com.raizesdonordeste.api.exception.ErroResposta;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * API stateless com JWT (HS256). O perfil vai no token e vira ROLE_<PERFIL>;
 * a autorização fina (por unidade e por dono do pedido) é feita nos serviços.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final SecretKey chave;
    private final ObjectMapper objectMapper;

    public SecurityConfig(@Value("${app.jwt.segredo}") String segredo, ObjectMapper objectMapper) {
        if (segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.segredo precisa ter pelo menos 32 bytes");
        }
        this.chave = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/unidades", "/api/unidades/*/cardapio").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagamentos/webhook").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(conversorDePerfis()))
                        .authenticationEntryPoint((req, res, ex) -> escreverErro(res, 401, "Unauthorized", "Token ausente, inválido ou expirado", req.getRequestURI()))
                        .accessDeniedHandler((req, res, ex) -> escreverErro(res, 403, "Forbidden", "Seu perfil não tem permissão para esta operação", req.getRequestURI())))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> escreverErro(res, 401, "Unauthorized", "Token ausente, inválido ou expirado", req.getRequestURI()))
                        .accessDeniedHandler((req, res, ex) -> escreverErro(res, 403, "Forbidden", "Seu perfil não tem permissão para esta operação", req.getRequestURI())));
        return http.build();
    }

    private JwtAuthenticationConverter conversorDePerfis() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("perfil");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }

    private void escreverErro(HttpServletResponse res, int status, String erro, String mensagem, String caminho) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(res.getOutputStream(), new ErroResposta(Instant.now(), status, erro, mensagem, caminho, List.of()));
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
