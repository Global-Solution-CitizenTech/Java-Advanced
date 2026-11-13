package fiap.com.br.zelafix.chamado.orgao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrgaoGovernoRepository extends JpaRepository<OrgaoGoverno, Long> {
    Optional<OrgaoGoverno> findByNmOrgaoIgnoreCase(String nmOrgao);
}
