package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "conta_fidelidade")
public class ContaFidelidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private int pontos;

    protected ContaFidelidade() {
    }

    public ContaFidelidade(Usuario usuario) {
        this.usuario = usuario;
    }

    public void creditar(int quantidade) {
        this.pontos += quantidade;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public int getPontos() { return pontos; }
}
