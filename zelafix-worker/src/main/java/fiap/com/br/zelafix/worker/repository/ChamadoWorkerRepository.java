package fiap.com.br.zelafix.worker.repository;

import fiap.com.br.zelafix.worker.entity.ChamadoWorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChamadoWorkerRepository extends JpaRepository<ChamadoWorkerEntity, Long> {
}

