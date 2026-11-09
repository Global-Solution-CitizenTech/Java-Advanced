package fiap.com.br.zelafix.endereco.bairro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BairroRepository extends JpaRepository<Bairro, Long> {
    Optional<Bairro> findByNomeIgnoreCaseAndCidadeId(String nome, Long cidadeId);
}
