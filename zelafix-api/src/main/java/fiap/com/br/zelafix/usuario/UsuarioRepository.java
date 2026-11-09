package fiap.com.br.zelafix.usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Override
    @EntityGraph(attributePaths = {"telefones", "endereco.bairro.cidade.estado"})
    Page<Usuario> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"telefones", "endereco.bairro.cidade.estado"})
    Optional<Usuario> findById(Long id);

    @EntityGraph(attributePaths = {"telefones", "endereco.bairro.cidade.estado"})
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByCargo(CargoUsuario cargo);
}

