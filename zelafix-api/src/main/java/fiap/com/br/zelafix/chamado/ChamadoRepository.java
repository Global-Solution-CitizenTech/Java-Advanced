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

    Optional<Chamado> findByProtocolo(String protocolo);

    Page<Chamado> findByUsuarioIdUsuario(Long idUsuario, Pageable pageable);

    Page<Chamado> findByStatusIdStatus(Long idStatus, Pageable pageable);

    Page<Chamado> findByCategoriaIdCategoria(Long idCategoria, Pageable pageable);

    @Query("""
        SELECT c FROM Chamado c
        WHERE (:status IS NULL OR LOWER(c.status.nmStatus) = LOWER(:status))
          AND (:categoria IS NULL OR LOWER(c.categoria.nmCategoria) = LOWER(:categoria))
          AND (:usuarioId IS NULL OR c.usuario.idUsuario = :usuarioId)
          AND (:busca IS NULL OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                             OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                             OR LOWER(c.protocolo) LIKE LOWER(CONCAT('%', :busca, '%'))
                             OR LOWER(c.endereco.bairro.nome) LIKE LOWER(CONCAT('%', :busca, '%')))
    """)
    Page<Chamado> findByFiltros(
            @Param("status") String status,
            @Param("categoria") String categoria,
            @Param("usuarioId") Long usuarioId,
            @Param("busca") String busca,
            Pageable pageable
    );

    long countByStatusNmStatusIgnoreCase(String nmStatus);
}

