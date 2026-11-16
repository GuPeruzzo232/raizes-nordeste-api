package br.com.raizesdonordeste.api.repository;

import br.com.raizesdonordeste.api.domain.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    @Query("select r from RegistroAuditoria r where (:unidadeId is null or r.unidadeId = :unidadeId) " +
            "and (:acao is null or r.acao = :acao) " +
            "and (:entidade is null or r.entidade = :entidade) " +
            "and (:entidadeId is null or r.entidadeId = :entidadeId)")
    Page<RegistroAuditoria> pesquisar(@Param("unidadeId") Long unidadeId, @Param("acao") String acao,
                                      @Param("entidade") String entidade, @Param("entidadeId") Long entidadeId,
                                      Pageable pageable);

    List<RegistroAuditoria> findByAcaoAndEntidadeId(String acao, Long entidadeId);
}
