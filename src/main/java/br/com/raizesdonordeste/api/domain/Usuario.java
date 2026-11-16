package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    /** Unidade de lotação (atendente, cozinha, gerente). Nulo para cliente e matriz. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @Column(length = 14)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false)
    private boolean anonimizado;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, Perfil perfil, Unidade unidade) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.unidade = unidade;
    }

    /** LGPD: substitui dados pessoais por valores sem relação com o titular. Pedidos são mantidos (obrigação fiscal). */
    public void anonimizar(String senhaAleatoriaHash) {
        this.nome = "Cliente anonimizado #" + id;
        this.email = "anonimizado-" + id + "@anonimizado.local";
        this.cpf = null;
        this.telefone = null;
        this.senhaHash = senhaAleatoriaHash;
        this.anonimizado = true;
        this.ativo = false;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public Unidade getUnidade() { return unidade; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public boolean isAnonimizado() { return anonimizado; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
}
