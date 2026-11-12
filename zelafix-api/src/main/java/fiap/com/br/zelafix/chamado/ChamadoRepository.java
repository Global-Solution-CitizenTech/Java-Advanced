package fiap.com.br.zelafix.chamado;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {

    @Override
    @EntityGraph(attributePaths = {"categoria", "usuario", "endereco.bairro.cidade.estado"})
    Optional<Chamado> findById(Long id);

    boolean existsByUsuarioIdUsuario(Long idUsuario);

    @EntityGraph(attributePaths = {"categoria", "usuario", "endereco.bairro.cidade.estado"})
    @Query("""
        SELECT c FROM Chamado c
        WHERE (:status IS NULL OR EXISTS (
            SELECT h FROM HistChamado h 
            WHERE h.chamado = c 
              AND LOWER(h.status.nmStatus) = LOWER(:status)
              AND h.idHist = (SELECT MAX(h2.idHist) FROM HistChamado h2 WHERE h2.chamado = c)
        ))
          AND (:categoria IS NULL OR LOWER(c.categoria.nmCategoria) = LOWER(:categoria))
          AND (:usuarioId IS NULL OR c.usuario.idUsuario = :usuarioId)
          AND (:busca IS NULL OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                             OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                             OR LOWER(c.endereco.bairro.nome) LIKE LOWER(CONCAT('%', :busca, '%')))
    """)
    Page<Chamado> findByFiltros(
            @Param("status") String status,
            @Param("categoria") String categoria,
            @Param("usuarioId") Long usuarioId,
            @Param("busca") String busca,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(c) FROM Chamado c
        WHERE EXISTS (
            SELECT h FROM HistChamado h 
            WHERE h.chamado = c 
              AND LOWER(h.status.nmStatus) = LOWER(:nmStatus)
              AND h.idHist = (SELECT MAX(h2.idHist) FROM HistChamado h2 WHERE h2.chamado = c)
        )
    """)
    long countByStatusNmStatusIgnoreCase(@Param("nmStatus") String nmStatus);
}
