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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnderecoServiceTest {

    @Mock
    private EnderecoRepository enderecoRepository;

    @Mock
    private EstadoRepository estadoRepository;

    @Mock
    private CidadeRepository cidadeRepository;

    @Mock
    private BairroRepository bairroRepository;

    @Mock
    private ViaCepService viaCepService;

    @InjectMocks
    private EnderecoService enderecoService;

    private Estado estadoSP;
    private Cidade cidadeSP;
    private Bairro bairroCentro;
    private Endereco endereco;

    @BeforeEach
    void setUp() {
        estadoSP = Estado.builder().id(1L).nome("São Paulo").build();
        cidadeSP = Cidade.builder().id(1L).nome("São Paulo").estado(estadoSP).build();
        bairroCentro = Bairro.builder().id(1L).nome("Sé").cidade(cidadeSP).build();

        endereco = Endereco.builder()
                .id(1L)
                .cep("01001000")
                .numero("100")
                .rua("Praça da Sé")
                .referencia("Lado par")
                .bairro(bairroCentro)
                .build();
    }

    @Test
    @DisplayName("Deve listar todos os endereços paginados")
    void deveListarEnderecosPaginados() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Endereco> page = new PageImpl<>(List.of(endereco));
        when(enderecoRepository.findAll(pageable)).thenReturn(page);

        Page<Endereco> resultado = enderecoService.findAll(pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("01001000", resultado.getContent().getFirst().getCep());
    }

    @Test
    @DisplayName("Deve buscar endereço por ID com sucesso")
    void deveBuscarEnderecoPorIdComSucesso() {
        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(endereco));

        Endereco encontrado = enderecoService.findById(1L);

        assertNotNull(encontrado);
        assertEquals(1L, encontrado.getId());
        assertEquals("Praça da Sé", encontrado.getRua());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando endereço não existir")
    void deveLancarExcecaoQuandoEnderecoNaoExistir() {
        when(enderecoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> enderecoService.findById(99L));
    }

    @Test
    @DisplayName("Deve reutilizar endereço existente com mesmo CEP, número e bairro")
    void deveReutilizarEnderecoExistente() {
        EnderecoRequest req = new EnderecoRequest("01001-000", "100", "Praça da Sé", "Lado par");
        ViaCepResponse viaCep = new ViaCepResponse("Praça da Sé", "Sé", "São Paulo", "São Paulo", false);

        when(viaCepService.getEnderecoPorCep("01001000")).thenReturn(viaCep);
        when(estadoRepository.findByNomeIgnoreCase("São Paulo")).thenReturn(Optional.of(estadoSP));
        when(cidadeRepository.findByNomeIgnoreCaseAndEstadoId("São Paulo", 1L)).thenReturn(Optional.of(cidadeSP));
        when(bairroRepository.findByNomeIgnoreCaseAndCidadeId("Sé", 1L)).thenReturn(Optional.of(bairroCentro));
        when(enderecoRepository.findFirstByCepAndNumeroAndBairroIdOrderByIdAsc("01001000", "100", 1L))
                .thenReturn(Optional.of(endereco));

        Endereco res = enderecoService.findOrCreateByCepAndNumero(req);

        assertEquals(1L, res.getId());
        assertEquals("Praça da Sé", res.getRua());
    }

    @Test
    @DisplayName("Deve criar novo endereço e normalizar dados se ViaCep falhar ou estiver fora do ar")
    void deveCriarEnderecoComFallbackQuandoViaCepFalhar() {
        EnderecoRequest req = new EnderecoRequest("99999-999", "500", "Avenida Desconhecida", "Bloco B");

        when(viaCepService.getEnderecoPorCep("99999999")).thenThrow(new RuntimeException("ViaCEP indisponível"));
        when(estadoRepository.findByNomeIgnoreCase(anyString())).thenReturn(Optional.of(estadoSP));
        when(cidadeRepository.findByNomeIgnoreCaseAndEstadoId(anyString(), anyLong())).thenReturn(Optional.of(cidadeSP));
        when(bairroRepository.findByNomeIgnoreCaseAndCidadeId(anyString(), anyLong())).thenReturn(Optional.of(bairroCentro));
        when(enderecoRepository.findFirstByCepAndNumeroAndBairroIdOrderByIdAsc(anyString(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(enderecoRepository.save(any(Endereco.class))).thenAnswer(inv -> inv.getArgument(0));

        Endereco criado = enderecoService.findOrCreateByCepAndNumero(req);

        assertNotNull(criado);
        assertEquals("99999999", criado.getCep());
        assertEquals("500", criado.getNumero());
        verify(enderecoRepository).save(any(Endereco.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando ViaCEP retornar erro=true para CEP inexistente")
    void deveLancarExcecaoQuandoViaCepRetornarErroTrue() {
        EnderecoRequest req = new EnderecoRequest("07700-000", "302", "Vila Rosina", "Av Otavio");
        when(viaCepService.getEnderecoPorCep("07700000"))
                .thenReturn(new ViaCepResponse(null, null, null, null, true));

        assertThrows(IllegalArgumentException.class, () -> enderecoService.findOrCreateByCepAndNumero(req));
    }

    @Test
    @DisplayName("Deve atualizar endereço com sucesso")
    void deveAtualizarEnderecoComSucesso() {
        EnderecoRequest req = new EnderecoRequest("01001-000", "102", "Novo complemento", "Praça da Sé");
        ViaCepResponse viaCep = new ViaCepResponse("Praça da Sé", "Sé", "São Paulo", "São Paulo", false);

        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(endereco));
        when(viaCepService.getEnderecoPorCep("01001000")).thenReturn(viaCep);
        when(estadoRepository.findByNomeIgnoreCase("São Paulo")).thenReturn(Optional.of(estadoSP));
        when(cidadeRepository.findByNomeIgnoreCaseAndEstadoId("São Paulo", 1L)).thenReturn(Optional.of(cidadeSP));
        when(bairroRepository.findByNomeIgnoreCaseAndCidadeId("Sé", 1L)).thenReturn(Optional.of(bairroCentro));
        when(enderecoRepository.save(any(Endereco.class))).thenReturn(endereco);

        Endereco atualizado = enderecoService.update(1L, req);

        assertNotNull(atualizado);
        assertEquals("102", atualizado.getNumero());
        assertEquals("Novo complemento", atualizado.getReferencia());
    }

    @Test
    @DisplayName("Deve deletar endereço existente")
    void deveDeletarEnderecoExistente() {
        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(endereco));

        enderecoService.delete(1L);

        verify(enderecoRepository).deleteById(1L);
    }
}
