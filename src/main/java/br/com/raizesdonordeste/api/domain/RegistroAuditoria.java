package br.com.raizesdonordeste.api.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Trilha de auditoria: quem fez, quando, o quê e em qual registro. Somente inserção. */
@Entity
@Table(name = "registro_auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_email", length = 160)
    private String usuarioEmail;

    @Column(length = 20)
    private String perfil;

    @Column(name = "unidade_id")
    private Long unidadeId;

    @Column(nullable = false, length = 50)
    private String acao;

    @Column(nullable = false, length = 50)
    private String entidade;

    @Column(name = "entidade_id")
    private Long entidadeId;

    @Column(length = 2000)
    private String detalhes;

    @Column(length = 60)
    private String ip;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora = Instant.now();

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(Long usuarioId, String usuarioEmail, String perfil, Long unidadeId, String acao,
                             String entidade, Long entidadeId, String detalhes, String ip) {
        this.usuarioId = usuarioId;
        this.usuarioEmail = usuarioEmail;
        this.perfil = perfil;
        this.unidadeId = unidadeId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.detalhes = detalhes;
        this.ip = ip;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getUsuarioEmail() { return usuarioEmail; }
    public String getPerfil() { return perfil; }
    public Long getUnidadeId() { return unidadeId; }
    public String getAcao() { return acao; }
    public String getEntidade() { return entidade; }
    public Long getEntidadeId() { return entidadeId; }
    public String getDetalhes() { return detalhes; }
    public String getIp() { return ip; }
    public Instant getDataHora() { return dataHora; }
}
