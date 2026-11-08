package fiap.com.br.zelafix.voto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VotoChamadoRepository extends JpaRepository<VotoChamado, Long> {
    boolean existsByChamadoIdChamadoAndUsuarioIdUsuario(Long idChamado, Long idUsuario);
    Optional<VotoChamado> findByChamadoIdChamadoAndUsuarioIdUsuario(Long idChamado, Long idUsuario);
    void deleteByChamadoIdChamadoAndUsuarioIdUsuario(Long idChamado, Long idUsuario);
    long countByChamadoIdChamado(Long idChamado);
}

