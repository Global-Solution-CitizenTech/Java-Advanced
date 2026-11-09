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
        return findEnderecoById(id);
    }

    @Transactional
    public Endereco create(EnderecoRequest enderecoRequest) {
        return findOrCreateByCepAndNumero(enderecoRequest);
    }

    @Transactional
    public Endereco update(Long id, EnderecoRequest enderecoRequest) {
        Endereco endereco = findEnderecoById(id);
        aplicarEndereco(endereco, enderecoRequest);
        return enderecoRepository.save(endereco);
    }

    @Transactional
    public void delete(Long id) {
        findEnderecoById(id);
        enderecoRepository.deleteById(id);
    }

    private Endereco findEnderecoById(Long id) {
        return enderecoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço com id " + id + " não encontrado."));
    }

    @Transactional
    public Endereco findOrCreateByCepAndNumero(EnderecoRequest enderecoRequest) {
        Endereco novo = buildEndereco(enderecoRequest);
        return enderecoRepository
                .findFirstByCepAndNumeroAndBairroIdOrderByIdAsc(novo.getCep(), novo.getNumero(), novo.getBairro().getId())
                .orElseGet(() -> enderecoRepository.save(novo));
    }

    private Endereco buildEndereco(EnderecoRequest request) {
        ViaCepResponse dados = obterDadosCep(request.cep());
        Bairro bairro = obterOuCriarBairro(dados);
        return request.toEntity(normalizarTexto(dados.logradouro()), bairro);
    }

    private Endereco aplicarEndereco(Endereco endereco, EnderecoRequest request) {
        ViaCepResponse dados = obterDadosCep(request.cep());
        Bairro bairro = obterOuCriarBairro(dados);
        return aplicarEm(endereco, request, normalizarTexto(dados.logradouro()), bairro);
    }

    private ViaCepResponse obterDadosCep(String cep) {
        ViaCepResponse dados = viaCepService.getEnderecoPorCep(cep.replaceAll("\\D", ""));
        if (Boolean.TRUE.equals(dados.erro())) {
            throw new IllegalArgumentException("CEP " + cep + " não encontrado.");
        }
        return dados;
    }

    private Bairro obterOuCriarBairro(ViaCepResponse dados) {
        String nomeEstado = normalizarTexto(dados.estado());
        Estado estado = estadoRepository.findByNomeIgnoreCase(nomeEstado)
                .orElseGet(() -> estadoRepository.save(Estado.builder().nome(nomeEstado).build()));

        String nomeCidade = normalizarTexto(dados.localidade());
        Cidade cidade = cidadeRepository.findByNomeIgnoreCaseAndEstadoId(nomeCidade, estado.getId())
                .orElseGet(() -> cidadeRepository.save(Cidade.builder().nome(nomeCidade).estado(estado).build()));

        String nomeBairro = normalizarTexto(dados.bairro());
        return bairroRepository.findByNomeIgnoreCaseAndCidadeId(nomeBairro, cidade.getId())
                .orElseGet(() -> bairroRepository.save(Bairro.builder().nome(nomeBairro).cidade(cidade).build()));
    }

    private String normalizarTexto(String valor) {
        return (valor == null || valor.isBlank()) ? "Não Informado" : valor.trim();
    }

    private Endereco aplicarEm(Endereco endereco, EnderecoRequest request, String rua, Bairro bairro) {
        endereco.setCep(request.cep().replaceAll("\\D", ""));
        endereco.setNumero(request.numero());
        endereco.setRua(rua);
        endereco.setBairro(bairro);
        String ref = (request.referencia() != null && !request.referencia().isBlank())
                ? request.referencia()
                : (request.complemento() != null && !request.complemento().isBlank() ? request.complemento() : "Sem referência");
        if (ref.length() > 50) {
            ref = ref.substring(0, 50);
        }
        endereco.setReferencia(ref);
        return endereco;
    }
}
