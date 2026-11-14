package fiap.com.br.zelafix.categoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Optional<Categoria> findByNmCategoria(CategoriaEnum nmCategoria);
}
