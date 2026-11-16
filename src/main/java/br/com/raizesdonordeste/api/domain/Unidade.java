package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "unidade")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 80)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    private boolean ativa = true;

    protected Unidade() {
    }

    public Unidade(String nome, String cidade, String uf) {
        this.nome = nome;
        this.cidade = cidade;
        this.uf = uf;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
}
