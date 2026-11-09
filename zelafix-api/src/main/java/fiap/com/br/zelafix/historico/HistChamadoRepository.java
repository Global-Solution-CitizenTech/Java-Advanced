package fiap.com.br.zelafix.historico;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistChamadoRepository extends JpaRepository<HistChamado, Long> {
    List<HistChamado> findByChamadoIdChamadoOrderByDtMudancaDesc(Long idChamado);
}
