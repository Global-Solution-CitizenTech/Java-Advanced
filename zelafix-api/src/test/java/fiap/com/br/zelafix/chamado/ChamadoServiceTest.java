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
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.HistChamadoRepository;
import fiap.com.br.zelafix.queue.producer.ChamadoEventProducer;
import fiap.com.br.zelafix.queue.producer.VotoEventProducer;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.status.StatusService;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import fiap.com.br.zelafix.voto.VotoChamado;
import fiap.com.br.zelafix.voto.VotoChamadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private HistChamadoRepository histChamadoRepository;
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
    private Status statusAberto;
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

        statusAberto = Status.builder()
                .idStatus(1L)
                .nmStatus("ABERTO")
                .build();

        Estado estado = Estado.builder().id(1L).nome("São Paulo").build();
        Cidade cidade = Cidade.builder().id(1L).nome("São Paulo").estado(estado).build();
        Bairro bairro = Bairro.builder().id(1L).nome("Pinheiros").cidade(cidade).build();

        endereco = Endereco.builder()
                .id(1L)
                .rua("Rua dos Pinheiros")
                .numero("720")
                .referencia("Esquina com Fradique")
                .cep("05422001")
                .bairro(bairro)
                .build();
    }

    @Test
    @DisplayName("Deve criar um chamado com status ABERTO e publicar evento no RabbitMQ")
    void deveCriarChamadoComSucesso() {
        EnderecoRequest enderecoRequest = new EnderecoRequest("05422-001", "720", "Esquina com Fradique");
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
        when(statusService.buscarPorNomeOuCriar("ABERTO")).thenReturn(statusAberto);
        when(enderecoService.findOrCreateByCepAndNumero(any())).thenReturn(endereco);

        Chamado chamadoSalvo = Chamado.builder()
                .idChamado(100L)
                .titulo(request.titulo())
                .descricao(request.descricao())
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .categoria(categoria)
                .usuario(usuario)
                .endereco(endereco)
                .dtAbertura(LocalDateTime.now())
                .historico(new ArrayList<>())
                .votos(new ArrayList<>())
                .build();

        when(chamadoRepository.save(any())).thenReturn(chamadoSalvo);

        ChamadoResponse response = chamadoService.criar(request, usuario.getEmail());

        assertNotNull(response);
        assertEquals("ZFX-00100", response.protocolo());
        assertEquals("Buraco perigoso na via", response.titulo());

        verify(chamadoEventProducer, times(1)).publicarTriagem(any());
        verify(histChamadoRepository, times(1)).save(any());
        verify(votoChamadoRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Deve apoiar um chamado e publicar evento assíncrono no RabbitMQ")
    void deveApoiarChamadoComSucesso() {
        Chamado chamado = Chamado.builder()
                .idChamado(200L)
                .titulo("Poste apagado")
                .descricao("Sem luz na rua")
                .usuario(usuario)
                .endereco(endereco)
                .categoria(categoria)
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .historico(new ArrayList<>())
                .votos(new ArrayList<>())
                .build();

        when(chamadoRepository.findById(200L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarPorEmail(usuario.getEmail())).thenReturn(usuario);
        when(votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(200L, 1L))
                .thenReturn(Optional.empty());

        ChamadoResponse response = chamadoService.apoiar(200L, usuario.getEmail());

        assertNotNull(response);
        verify(votoChamadoRepository, times(1)).save(any());
        verify(votoEventProducer, times(1)).publicarVoto(any());
    }

    @Test
    @DisplayName("Deve desapoiar quando cidadão já havia apoiado")
    void deveDesapoiarQuandoJaVotou() {
        Chamado chamado = Chamado.builder()
                .idChamado(300L)
                .titulo("Semáforo quebrado")
                .descricao("Cruzamento perigoso")
                .usuario(usuario)
                .endereco(endereco)
                .categoria(categoria)
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .historico(new ArrayList<>())
                .votos(new ArrayList<>())
                .build();

        VotoChamado voto = new VotoChamado(chamado, usuario);

        when(chamadoRepository.findById(300L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarPorEmail(usuario.getEmail())).thenReturn(usuario);
        when(votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(300L, 1L))
                .thenReturn(Optional.of(voto));

        ChamadoResponse response = chamadoService.apoiar(300L, usuario.getEmail());

        assertNotNull(response);
        verify(votoChamadoRepository, times(1)).delete(voto);
        verify(votoEventProducer, times(1)).publicarVoto(any());
    }

    @Test
    @DisplayName("Apenas GESTOR pode atualizar o status de um chamado")
    void deveBloquearCidadaoAoTentarAtualizarStatus() {
        Chamado chamado = Chamado.builder()
                .idChamado(400L)
                .titulo("Guia rebaixada danificada")
                .usuario(usuario)
                .historico(new ArrayList<>())
                .build();

        when(chamadoRepository.findById(400L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarPorEmail(usuario.getEmail())).thenReturn(usuario);

        StatusUpdateRequest request = new StatusUpdateRequest("CONCLUIDO", "Resolvido", null);

        assertThrows(BusinessException.class, () ->
                chamadoService.atualizarStatus(400L, request, usuario.getEmail())
        );
    }

    @Test
    @DisplayName("GESTOR atualiza status com sucesso e grava histórico")
    void devePermitirGestorAtualizarStatus() {
        Usuario gestor = Usuario.builder()
                .idUsuario(2L)
                .nmUsuario("Gestora Maria")
                .email("gestor@zelafix.com.br")
                .cargo(CargoUsuario.GESTOR)
                .build();

        Status statusNovo = Status.builder().idStatus(4L).nmStatus("CONCLUIDO").build();

        Chamado chamado = Chamado.builder()
                .idChamado(500L)
                .titulo("Asfalto afundando")
                .descricao("Perigo")
                .categoria(categoria)
                .endereco(endereco)
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .historico(new ArrayList<>())
                .votos(new ArrayList<>())
                .build();

        when(chamadoRepository.findById(500L)).thenReturn(Optional.of(chamado));
        when(usuarioService.buscarPorEmail(gestor.getEmail())).thenReturn(gestor);
        when(statusService.buscarPorNomeOuCriar("CONCLUIDO")).thenReturn(statusNovo);

        StatusUpdateRequest request = new StatusUpdateRequest("CONCLUIDO", "Serviço finalizado com sucesso.", "Recapeamento concluído.");

        ChamadoResponse response = chamadoService.atualizarStatus(500L, request, gestor.getEmail());

        assertNotNull(response);
        verify(histChamadoRepository, times(1)).save(any(HistChamado.class));
    }
}
