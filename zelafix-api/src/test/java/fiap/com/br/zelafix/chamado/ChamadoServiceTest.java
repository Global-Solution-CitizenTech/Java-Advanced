package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.categoria.CategoriaService;
import fiap.com.br.zelafix.chamado.dto.ChamadoCreateRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.ChamadoUpdateRequest;
import fiap.com.br.zelafix.chamado.dto.StatusUpdateRequest;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.endereco.estado.Estado;
import fiap.com.br.zelafix.exception.BusinessException;
import fiap.com.br.zelafix.historico.HistStatusRepository;
import fiap.com.br.zelafix.queue.producer.ChamadoEventProducer;
import fiap.com.br.zelafix.queue.producer.VotoEventProducer;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.status.StatusService;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import fiap.com.br.zelafix.voto.VotoChamadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private CategoriaService categoriaService;
    @Mock
    private StatusService statusService;
    @Mock
    private EnderecoService enderecoService;
    @Mock
    private HistStatusRepository histStatusRepository;
    @Mock
    private VotoChamadoRepository votoChamadoRepository;
    @Mock
    private ChamadoEventProducer chamadoEventProducer;
    @Mock
    private VotoEventProducer votoEventProducer;

    @InjectMocks
    private ChamadoService chamadoService;

    private Usuario usuario;
    private Categoria categoria;
    private Status statusPendente;
    private Endereco endereco;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .idUsuario(1L)
                .nmUsuario("Carlos Munícipe")
                .email("cidadao@zelafix.com.br")
                .cargo(CargoUsuario.CIDADAO)
                .build();

        categoria = Categoria.builder()
                .idCategoria(1L)
                .nmCategoria("Buraco na Rua")
                .build();

        statusPendente = Status.builder()
                .idStatus(1L)
                .nmStatus("Pendente Triagem")
                .build();

        Estado estado = Estado.builder().id(1L).nome("SP").build();
        Cidade cidade = Cidade.builder().id(1L).nome("São Paulo").estado(estado).build();
        Bairro bairro = Bairro.builder().id(1L).nome("Pinheiros").cidade(cidade).build();

        endereco = Endereco.builder()
                .id(1L)
                .rua("Rua dos Pinheiros")
                .numero("720")
                .cep("05422001")
                .bairro(bairro)
                .build();
    }

    @Test
    @DisplayName("Deve criar um chamado com status Pendente Triagem e publicar evento no RabbitMQ")
    void deveCriarChamadoComSucesso() {
        EnderecoRequest enderecoRequest = new EnderecoRequest("05422-001", "720", null);
        ChamadoCreateRequest request = new ChamadoCreateRequest(
                "Buraco perigoso na via",
                "Cratera profunda na pista da direita",
                "Buraco na Rua",
                enderecoRequest,
                "https://images.unsplash.com/photo-1581375922343-ac6b40caf5e3",
                null
        );

        when(usuarioService.buscarPorEmail(usuario.getEmail())).thenReturn(usuario);
        when(categoriaService.buscarPorNomeOuCriar("Buraco na Rua")).thenReturn(categoria);
        when(statusService.buscarPorNomeOuCriar("Pendente Triagem")).thenReturn(statusPendente);
        when(enderecoService.findOrCreateByCepAndNumero(any())).thenReturn(endereco);

        Chamado chamadoSalvo = Chamado.builder()
                .idChamado(100L)
                .protocolo("ZF-2026-00100")
                .titulo(request.titulo())
                .descricao(request.descricao())
                .categoria(categoria)
                .status(statusPendente)
                .usuario(usuario)
                .endereco(endereco)
                .prioridade(UrgenciaChamado.ALTA)
                .apoiosCount(1)
                .dtAbertura(LocalDateTime.now())
                .build();

        when(chamadoRepository.save(any())).thenReturn(chamadoSalvo);

        ChamadoResponse response = chamadoService.criar(request, usuario.getEmail());

        assertNotNull(response);
        assertEquals("ZF-2026-00100", response.protocolo());
        assertEquals("Buraco perigoso na via", response.titulo());
        assertEquals(1, response.apoiosCount());

        verify(chamadoEventProducer, times(1)).publicarTriagem(any());
        verify(histStatusRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Deve apoiar um chamado e publicar evento assíncrono no RabbitMQ")
    void deveApoiarChamadoComSucesso() {
        Chamado chamado = Chamado.builder()
                .idChamado(200L)
                .titulo("Poste apagado")
                .descricao("Sem luz na rua")
                .apoiosCount(5)
                .usuario(usuario)
                .endereco(endereco)
                .categoria(categoria)
                .status(statusPendente)
                .dtAbertura(LocalDateTime.now())
                .build();

        when(chamadoRepository.findById(200L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarPorEmail(usuario.getEmail())).thenReturn(usuario);
        when(votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(200L, 1L)).thenReturn(Optional.empty());

        Chamado chamadoSalvo = Chamado.builder()
                .idChamado(200L)
                .titulo("Poste apagado")
                .descricao("Sem luz na rua")
                .apoiosCount(6)
                .usuario(usuario)
                .endereco(endereco)
                .categoria(categoria)
                .status(statusPendente)
                .dtAbertura(LocalDateTime.now())
                .build();
        when(chamadoRepository.save(any())).thenReturn(chamadoSalvo);

        ChamadoResponse response = chamadoService.apoiar(200L, usuario.getEmail());

        assertNotNull(response);
        assertEquals(6, response.apoiosCount());
        verify(votoEventProducer, times(1)).publicarVoto(any());
    }

    @Test
    @DisplayName("Deve atualizar status do chamado quando o usuário for GESTOR (órgão público)")
    void deveAtualizarStatusComAuditoria() {
        Usuario gestor = Usuario.builder()
                .idUsuario(2L)
                .nmUsuario("Mariana Gestora")
                .email("gestor@zelafix.com.br")
                .cargo(CargoUsuario.GESTOR)
                .build();

        Chamado chamado = Chamado.builder()
                .idChamado(300L)
                .titulo("Bueiro entupido")
                .status(statusPendente)
                .usuario(usuario)
                .endereco(endereco)
                .categoria(categoria)
                .dtAbertura(LocalDateTime.now())
                .build();

        Status statusResolvido = Status.builder().idStatus(3L).nmStatus("Resolvido").build();

        when(chamadoRepository.findById(300L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarPorEmail(gestor.getEmail())).thenReturn(gestor);
        when(statusService.buscarPorNomeOuCriar("Resolvido")).thenReturn(statusResolvido);
        when(chamadoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest("Resolvido", "Serviço concluído pela equipe", "Desobstrução realizada com sucesso");
        ChamadoResponse response = chamadoService.atualizarStatus(300L, request, gestor.getEmail());

        assertEquals("Resolvido", response.status());
        verify(histStatusRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Deve impedir que usuário comum (CIDADAO) atualize o status do chamado")
    void deveImpedirCidadaoDeAtualizarStatus() {
        Chamado chamado = Chamado.builder()
                .idChamado(300L)
                .titulo("Bueiro entupido")
                .status(statusPendente)
                .usuario(usuario)
                .build();

        when(chamadoRepository.findById(300L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarPorEmail(usuario.getEmail())).thenReturn(usuario);

        StatusUpdateRequest request = new StatusUpdateRequest("Resolvido", null, null);

        assertThrows(
                BusinessException.class,
                () -> chamadoService.atualizarStatus(300L, request, usuario.getEmail())
        );
    }

    @Test
    @DisplayName("Deve atualizar dados cadastrais do chamado com sucesso")
    void deveAtualizarChamadoComSucesso() {
        Chamado chamado = Chamado.builder()
                .idChamado(400L)
                .titulo("Título Antigo")
                .descricao("Descrição Antiga")
                .usuario(usuario)
                .endereco(endereco)
                .categoria(categoria)
                .status(statusPendente)
                .dtAbertura(LocalDateTime.now())
                .build();

        when(chamadoRepository.findById(400L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarPorEmail(usuario.getEmail())).thenReturn(usuario);
        when(chamadoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChamadoUpdateRequest updateRequest = new ChamadoUpdateRequest(
                "Título Novo",
                "Descrição Nova Atualizada",
                null,
                "ALTA",
                null,
                "https://imagem.com/foto.jpg",
                null,
                null
        );

        ChamadoResponse response = chamadoService.atualizar(400L, updateRequest, usuario.getEmail());

        assertNotNull(response);
        assertEquals("Título Novo", response.titulo());
        assertEquals("Descrição Nova Atualizada", response.descricao());
        assertEquals("Alta", response.prioridade());
        verify(chamadoRepository, times(1)).save(chamado);
    }
}
