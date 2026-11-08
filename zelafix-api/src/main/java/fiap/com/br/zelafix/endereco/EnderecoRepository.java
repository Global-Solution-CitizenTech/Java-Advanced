package fiap.com.br.zelafix.endereco;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
    Optional<Endereco> findFirstByCepAndNumeroAndBairroIdOrderByIdAsc(String cep, String numero, Long bairroId);
}
