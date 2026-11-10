package fiap.com.br.zelafix.voto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface VotoChamadoRepository extends JpaRepository<VotoChamado, Long> {
    boolean existsByChamadoIdChamadoAndUsuarioIdUsuario(Long idChamado, Long idUsuario);
    Optional<VotoChamado> findByChamadoIdChamadoAndUsuarioIdUsuario(Long idChamado, Long idUsuario);
    long countByChamadoIdChamado(Long idChamado);
    void deleteByChamadoIdChamado(Long idChamado);

    @Query("""
        SELECT v.chamado.idChamado, COUNT(v)
        FROM VotoChamado v
        WHERE v.chamado.idChamado IN :chamadoIds
        GROUP BY v.chamado.idChamado
    """)
    List<Object[]> countVotosByChamadoIds(@Param("chamadoIds") List<Long> chamadoIds);

    @Query("""
        SELECT v.chamado.idChamado
        FROM VotoChamado v
        WHERE v.chamado.idChamado IN :chamadoIds
          AND v.usuario.idUsuario = :usuarioId
    """)
    Set<Long> findChamadoIdsApoiadosPorMim(@Param("chamadoIds") List<Long> chamadoIds, @Param("usuarioId") Long usuarioId);
}
