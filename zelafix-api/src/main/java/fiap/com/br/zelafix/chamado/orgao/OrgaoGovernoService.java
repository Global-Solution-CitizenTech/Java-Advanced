package fiap.com.br.zelafix.chamado.orgao;

import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrgaoGovernoService {

    private final OrgaoGovernoRepository orgaoGovernoRepository;

    @Cacheable(value = "orgaos", key = "#orgao.name()")
    public OrgaoGoverno findByNome(OrgaoGovernoEnum orgao) {
        return orgaoGovernoRepository.findByNmOrgao(orgao)
                .orElseThrow(() -> new ResourceNotFoundException("Órgão governamental não encontrado: " + orgao));
    }
}
