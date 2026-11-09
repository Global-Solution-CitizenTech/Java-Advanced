package fiap.com.br.zelafix.endereco;

import fiap.com.br.zelafix.endereco.bairro.Bairro;
import fiap.com.br.zelafix.endereco.bairro.BairroRepository;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import fiap.com.br.zelafix.endereco.cidade.CidadeRepository;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.endereco.dto.ViaCepResponse;
import fiap.com.br.zelafix.endereco.estado.Estado;
import fiap.com.br.zelafix.endereco.estado.EstadoRepository;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final EstadoRepository estadoRepository;
    private final CidadeRepository cidadeRepository;
    private final BairroRepository bairroRepository;
    private final ViaCepService viaCepService;

    public Page<Endereco> findAll(Pageable pageable) {
        return enderecoRepository.findAll(pageable);
    }

    public Endereco findById(Long id) {
        return enderecoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço " + id + " não encontrado."));
    }

    @Transactional
    public Endereco findOrCreate(EnderecoRequest req) {
        String cepLimpo = req.cep().replaceAll("\\D", "");
        return enderecoRepository.findFirstByCepAndNumero(cepLimpo, req.numero())
                .orElseGet(() -> {
                    ViaCepResponse dados = viaCepService.getEnderecoPorCep(cepLimpo);
                    Estado estado = estadoRepository.findByNomeIgnoreCase(dados.estado())
                            .orElseGet(() -> estadoRepository.save(new Estado(null, dados.estado(), null)));
                    Cidade cidade = cidadeRepository.findByNomeIgnoreCaseAndEstadoId(dados.localidade(), estado.getId())
                            .orElseGet(() -> cidadeRepository.save(new Cidade(null, dados.localidade(), estado, null)));
                    Bairro bairro = bairroRepository.findByNomeIgnoreCaseAndCidadeId(dados.bairro(), cidade.getId())
                            .orElseGet(() -> bairroRepository.save(new Bairro(null, dados.bairro(), cidade, null)));

                    String rua = (req.rua() != null && !req.rua().isBlank()) ? req.rua() : dados.logradouro();
                    return enderecoRepository.save(Endereco.builder()
                            .cep(cepLimpo)
                            .rua(rua)
                            .numero(req.numero())
                            .referencia(req.referencia())
                            .bairro(bairro)
                            .build());
                });
    }

    @Transactional
    public Endereco findOrCreateByCepAndNumero(EnderecoRequest req) {
        return findOrCreate(req);
    }

    @Transactional
    public void delete(Long id) {
        enderecoRepository.delete(findById(id));
    }
}
