package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Registro histórico de consentimento (LGPD art. 8º). Nunca é sobrescrito:
 * cada concessão ou revogação gera uma nova linha, o que serve de prova.
 */
@Entity
@Table(name = "consentimento")
public class Consentimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FinalidadeConsentimento finalidade;

    @Column(nullable = false)
    private boolean concedido;

    @Column(name = "versao_termo", nullable = false, length = 20)
    private String versaoTermo;

    @Column(name = "canal_origem", length = 20)
    private String canalOrigem;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm = Instant.now();

    protected Consentimento() {
    }

    public Consentimento(Usuario usuario, FinalidadeConsentimento finalidade, boolean concedido, String versaoTermo, String canalOrigem) {
        this.usuario = usuario;
        this.finalidade = finalidade;
        this.concedido = concedido;
        this.versaoTermo = versaoTermo;
        this.canalOrigem = canalOrigem;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public FinalidadeConsentimento getFinalidade() { return finalidade; }
    public boolean isConcedido() { return concedido; }
    public String getVersaoTermo() { return versaoTermo; }
    public String getCanalOrigem() { return canalOrigem; }
    public Instant getRegistradoEm() { return registradoEm; }
}
