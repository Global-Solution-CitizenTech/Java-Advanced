package fiap.com.br.zelafix.chamado.orgao;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrgaoGovernoService {

    private final OrgaoGovernoRepository orgaoGovernoRepository;

    @Cacheable(value = "orgaos", key = "#nome.trim().toLowerCase()")
    public OrgaoGoverno findByNameOrCreate(String nome) {
        return orgaoGovernoRepository.findByNmOrgaoIgnoreCase(nome.trim())
                .orElseGet(() -> orgaoGovernoRepository.save(OrgaoGoverno.builder().nmOrgao(nome.trim()).build()));
    }
}
