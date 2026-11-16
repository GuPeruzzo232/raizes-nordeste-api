package br.com.raizesdonordeste.api.config;

import br.com.raizesdonordeste.api.domain.*;
import br.com.raizesdonordeste.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Carga de demonstração (3 unidades, cardápios diferentes, itens juninos e um usuário
 * por perfil). Só roda com o banco vazio. Senha de todos os usuários: Senha@123
 */
@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true", matchIfMissing = true)
public class DadosIniciais implements ApplicationRunner {

    public static final String SENHA_PADRAO = "Senha@123";
    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final UnidadeRepository unidades;
    private final ProdutoRepository produtos;
    private final ItemCardapioRepository cardapio;
    private final EstoqueRepository estoques;
    private final UsuarioRepository usuarios;
    private final ConsentimentoRepository consentimentos;
    private final ContaFidelidadeRepository contas;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public DadosIniciais(UnidadeRepository unidades, ProdutoRepository produtos, ItemCardapioRepository cardapio,
                         EstoqueRepository estoques, UsuarioRepository usuarios, ConsentimentoRepository consentimentos,
                         ContaFidelidadeRepository contas, PasswordEncoder encoder, Clock clock) {
        this.unidades = unidades;
        this.produtos = produtos;
        this.cardapio = cardapio;
        this.estoques = estoques;
        this.usuarios = usuarios;
        this.consentimentos = consentimentos;
        this.contas = contas;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (unidades.count() > 0) {
            return;
        }
        int ano = LocalDate.now(clock).getYear();
        LocalDate inicioJunino = LocalDate.of(ano, 6, 1);
        LocalDate fimJunino = LocalDate.of(ano, 7, 31);

        Unidade recife = unidades.save(new Unidade("Raízes do Nordeste - Recife Boa Viagem", "Recife", "PE"));
        Unidade salvador = unidades.save(new Unidade("Raízes do Nordeste - Salvador Pelourinho", "Salvador", "BA"));
        Unidade fortaleza = unidades.save(new Unidade("Raízes do Nordeste - Fortaleza Aldeota", "Fortaleza", "CE"));

        Produto baiao = produtos.save(new Produto("Baião de dois", "Arroz, feijão-verde, queijo coalho e nata", "Pratos", new BigDecimal("32.90"), false));
        Produto tapioca = produtos.save(new Produto("Tapioca de queijo coalho", "Tapioca recheada com queijo coalho e manteiga de garrafa", "Lanches", new BigDecimal("14.50"), false));
        Produto cuscuz = produtos.save(new Produto("Cuscuz nordestino", "Cuscuz de milho com ovo e charque", "Lanches", new BigDecimal("12.00"), false));
        Produto carneDeSol = produtos.save(new Produto("Carne de sol com macaxeira", "Carne de sol na brasa com macaxeira cozida", "Pratos", new BigDecimal("39.90"), false));
        Produto sucoCaja = produtos.save(new Produto("Suco de cajá", "Suco natural 400 ml", "Bebidas", new BigDecimal("9.00"), false));
        Produto acaraje = produtos.save(new Produto("Acarajé", "Acarajé com vatapá e camarão", "Lanches", new BigDecimal("18.00"), false));
        Produto canjica = produtos.save(new Produto("Canjica junina", "Canjica de milho com canela (São João)", "Sobremesas", new BigDecimal("11.00"), true));
        Produto pamonha = produtos.save(new Produto("Pamonha", "Pamonha doce de milho verde (São João)", "Sobremesas", new BigDecimal("10.00"), true));

        item(recife, baiao, "32.90", 100, null, null);
        item(recife, tapioca, "14.50", 100, null, null);
        item(recife, cuscuz, "12.00", 100, null, null);
        item(recife, carneDeSol, "39.90", 5, null, null);
        item(recife, sucoCaja, "9.00", 200, null, null);
        item(recife, canjica, "11.00", 80, inicioJunino, fimJunino);
        item(recife, pamonha, "10.00", 80, inicioJunino, fimJunino);

        item(salvador, baiao, "34.90", 100, null, null);
        item(salvador, acaraje, "18.00", 100, null, null);
        item(salvador, tapioca, "15.00", 100, null, null);
        item(salvador, sucoCaja, "9.50", 200, null, null);

        item(fortaleza, baiao, "31.90", 100, null, null);
        item(fortaleza, tapioca, "13.50", 100, null, null);
        item(fortaleza, cuscuz, "11.50", 100, null, null);
        item(fortaleza, canjica, "10.50", 60, inicioJunino, fimJunino);

        String senha = encoder.encode(SENHA_PADRAO);
        usuarios.save(new Usuario("Ana Matriz", "matriz@raizes.com.br", senha, Perfil.MATRIZ, null));
        usuarios.save(new Usuario("Gilberto Gerente", "gerente.recife@raizes.com.br", senha, Perfil.GERENTE, recife));
        usuarios.save(new Usuario("Alice Atendente", "atendente.recife@raizes.com.br", senha, Perfil.ATENDENTE, recife));
        usuarios.save(new Usuario("Caio Cozinha", "cozinha.recife@raizes.com.br", senha, Perfil.COZINHA, recife));
        usuarios.save(new Usuario("Sara Gerente", "gerente.salvador@raizes.com.br", senha, Perfil.GERENTE, salvador));
        usuarios.save(new Usuario("Bruno Atendente", "atendente.salvador@raizes.com.br", senha, Perfil.ATENDENTE, salvador));
        usuarios.save(new Usuario("Fábio Gerente", "gerente.fortaleza@raizes.com.br", senha, Perfil.GERENTE, fortaleza));

        Usuario maria = new Usuario("Maria do Carmo", "maria@email.com", senha, Perfil.CLIENTE, null);
        maria.setCpf("12345678901");
        maria.setTelefone("81999990000");
        usuarios.save(maria);
        for (FinalidadeConsentimento f : FinalidadeConsentimento.values()) {
            consentimentos.save(new Consentimento(maria, f, f == FinalidadeConsentimento.PROGRAMA_FIDELIDADE, "2026.1", "APP"));
        }
        contas.save(new ContaFidelidade(maria));

        Usuario joao = new Usuario("João Batista", "joao@email.com", senha, Perfil.CLIENTE, null);
        usuarios.save(joao);
        for (FinalidadeConsentimento f : FinalidadeConsentimento.values()) {
            consentimentos.save(new Consentimento(joao, f, false, "2026.1", "TOTEM"));
        }
        log.info("Dados de demonstração carregados (senha padrão: {})", SENHA_PADRAO);
    }

    private void item(Unidade u, Produto p, String preco, int estoque, LocalDate inicio, LocalDate fim) {
        cardapio.save(new ItemCardapio(u, p, new BigDecimal(preco), inicio, fim));
        estoques.save(new Estoque(u, p, estoque));
    }
}
