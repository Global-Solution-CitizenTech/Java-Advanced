package fiap.com.br.zelafix.chamado;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {

    Page<Chamado> findByUsuarioIdUsuario(Long idUsuario, Pageable pageable);

    Page<Chamado> findByCategoriaIdCategoria(Long idCategoria, Pageable pageable);

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
        SELECT c FROM Chamado c
        WHERE EXISTS (
            SELECT h FROM HistChamado h 
            WHERE h.chamado = c 
              AND h.status.idStatus = :idStatus
              AND h.idHist = (SELECT MAX(h2.idHist) FROM HistChamado h2 WHERE h2.chamado = c)
        )
    """)
    Page<Chamado> findByStatusIdStatus(@Param("idStatus") Long idStatus, Pageable pageable);

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

    default Optional<Chamado> findByProtocolo(String protocolo) {
        if (protocolo == null || protocolo.isBlank()) return Optional.empty();
        String digits = protocolo.replaceAll("\\D", "");
        if (digits.isEmpty()) return Optional.empty();
        try {
            return findById(Long.parseLong(digits));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
