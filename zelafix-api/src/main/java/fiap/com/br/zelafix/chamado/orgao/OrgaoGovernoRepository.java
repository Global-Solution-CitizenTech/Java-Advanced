package fiap.com.br.zelafix.chamado.orgao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrgaoGovernoRepository extends JpaRepository<OrgaoGoverno, Long> {
    Optional<OrgaoGoverno> findByNmOrgao(OrgaoGovernoEnum nmOrgao);
}
