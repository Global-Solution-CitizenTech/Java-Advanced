package fiap.com.br.zelafix.historico;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistChamadoRepository extends JpaRepository<HistChamado, Long> {

    @EntityGraph(attributePaths = {"usuario", "status"})
    List<HistChamado> findByChamadoIdChamadoOrderByDtMudancaDesc(Long idChamado);

    Optional<HistChamado> findFirstByChamadoIdChamadoOrderByDtMudancaDesc(Long idChamado);

    @EntityGraph(attributePaths = {"status"})
    @Query("""
        SELECT h FROM HistChamado h
        WHERE h.chamado.idChamado IN :chamadoIds
          AND h.idHist IN (
              SELECT MAX(h2.idHist) FROM HistChamado h2
              WHERE h2.chamado.idChamado IN :chamadoIds
              GROUP BY h2.chamado.idChamado
          )
    """)
    List<HistChamado> findLatestByChamadoIds(@Param("chamadoIds") List<Long> chamadoIds);
}
