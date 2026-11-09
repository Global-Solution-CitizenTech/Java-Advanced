package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.categoria.CategoriaService;
import fiap.com.br.zelafix.chamado.dto.ChamadoRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.StatusRequest;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
                .nmStatus("PENDENTE")
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
    @DisplayName("Deve criar um chamado com status PENDENTE e publicar evento no RabbitMQ")
    void deveCriarChamadoComSucesso() {
        EnderecoRequest enderecoRequest = new EnderecoRequest("05422001", "720", "Esquina com Fradique", "Rua dos Pinheiros");
        ChamadoRequest request = new ChamadoRequest(
                "Buraco perigoso na via",
                "Cratera profunda na pista da direita",
                1L,
                enderecoRequest,
                null
        );

        when(usuarioService.findByEmail(usuario.getEmail())).thenReturn(usuario);
        when(categoriaService.findById(1L)).thenReturn(categoria);
        when(statusService.findByNameOrCreate("PENDENTE")).thenReturn(statusPendente);
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
                .build();

        when(chamadoRepository.save(any())).thenReturn(chamadoSalvo);

        ChamadoResponse response = chamadoService.create(request, usuario.getEmail());

        assertNotNull(response);
        assertEquals(100L, response.idChamado());
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
                .dtAbertura(LocalDateTime.now())
                .build();

        when(chamadoRepository.findById(200L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(usuario.getEmail())).thenReturn(usuario);
        when(votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(200L, 1L))
                .thenReturn(Optional.empty());

        ChamadoResponse response = chamadoService.vote(200L, usuario.getEmail());

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
                .dtAbertura(LocalDateTime.now())
                .build();

        VotoChamado voto = VotoChamado.builder().chamado(chamado).usuario(usuario).dtVoto(LocalDateTime.now()).build();

        when(chamadoRepository.findById(300L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(usuario.getEmail())).thenReturn(usuario);
        when(votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(300L, 1L))
                .thenReturn(Optional.of(voto));

        ChamadoResponse response = chamadoService.vote(300L, usuario.getEmail());

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
                .build();

        when(chamadoRepository.findById(400L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(usuario.getEmail())).thenReturn(usuario);

        StatusRequest request = new StatusRequest("CONCLUIDO", "Resolvido");

        assertThrows(BusinessException.class, () ->
                chamadoService.updateStatus(400L, request, usuario.getEmail())
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
                .usuario(usuario)
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .dtAbertura(LocalDateTime.now())
                .build();

        when(chamadoRepository.findById(500L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(gestor.getEmail())).thenReturn(gestor);
        when(statusService.findByNameOrCreate("CONCLUIDO")).thenReturn(statusNovo);

        StatusRequest request = new StatusRequest("CONCLUIDO", "Serviço finalizado com sucesso.");

        ChamadoResponse response = chamadoService.updateStatus(500L, request, gestor.getEmail());

        assertNotNull(response);
        verify(histChamadoRepository, times(1)).save(any(HistChamado.class));
    }

    @Test
    @DisplayName("ADMIN atualiza status com sucesso e grava histórico")
    void devePermitirAdminAtualizarStatus() {
        Usuario admin = Usuario.builder()
                .idUsuario(99L)
                .nmUsuario("Admin do Sistema")
                .email("admin@zelafix.com.br")
                .cargo(CargoUsuario.ADMIN)
                .build();

        Status statusNovo = Status.builder().idStatus(4L).nmStatus("CONCLUIDO").build();

        Chamado chamado = Chamado.builder()
                .idChamado(600L)
                .titulo("Vazamento de água")
                .descricao("Urgente")
                .categoria(categoria)
                .endereco(endereco)
                .usuario(usuario)
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .dtAbertura(LocalDateTime.now())
                .build();

        when(chamadoRepository.findById(600L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(admin.getEmail())).thenReturn(admin);
        when(statusService.findByNameOrCreate("CONCLUIDO")).thenReturn(statusNovo);

        StatusRequest request = new StatusRequest("CONCLUIDO", "Resolvido por intervenção direta de admin.");

        ChamadoResponse response = chamadoService.updateStatus(600L, request, admin.getEmail());

        assertNotNull(response);
        verify(histChamadoRepository, times(1)).save(any(HistChamado.class));
    }

    @Test
    @DisplayName("ADMIN pode deletar chamado por ID")
    void devePermitirAdminDeletarChamado() {
        Chamado chamado = Chamado.builder()
                .idChamado(700L)
                .titulo("Chamado para exclusão")
                .build();

        when(chamadoRepository.findById(700L)).thenReturn(Optional.of(chamado));

        chamadoService.delete(700L);

        verify(chamadoRepository, times(1)).delete(chamado);
    }

    @Test
    @DisplayName("Deve buscar chamados paginados com carregamento em lote de status, votos e apoio (sem N+1)")
    void deveBuscarChamadosComCarregamentoEmLote() {
        Chamado c1 = Chamado.builder().idChamado(10L).titulo("Buraco 1").categoria(categoria).usuario(usuario).endereco(endereco).dtAbertura(LocalDateTime.now()).build();
        Chamado c2 = Chamado.builder().idChamado(20L).titulo("Buraco 2").categoria(categoria).usuario(usuario).endereco(endereco).dtAbertura(LocalDateTime.now()).build();
        Page<Chamado> page = new PageImpl<>(List.of(c1, c2));

        when(usuarioService.findByEmail(usuario.getEmail())).thenReturn(usuario);
        when(chamadoRepository.findByFiltros(null, null, null, null, Pageable.unpaged())).thenReturn(page);

        HistChamado h1 = HistChamado.builder().chamado(c1).status(statusPendente).build();
        when(histChamadoRepository.findLatestByChamadoIds(List.of(10L, 20L))).thenReturn(List.of(h1));
        when(votoChamadoRepository.countVotosByChamadoIds(List.of(10L, 20L))).thenReturn(List.<Object[]>of(new Object[]{10L, 5L}));
        when(votoChamadoRepository.findChamadoIdsApoiadosPorMim(List.of(10L, 20L), 1L)).thenReturn(Set.of(10L));

        Page<ChamadoResponse> resultado = chamadoService.findAll(null, null, null, null, usuario.getEmail(), Pageable.unpaged());

        assertEquals(2, resultado.getTotalElements());
        assertEquals("PENDENTE", resultado.getContent().get(0).status());
        assertEquals(5L, resultado.getContent().get(0).totalVotos());
        assertEquals(true, resultado.getContent().get(0).apoiadoPorMim());
        assertEquals("PENDENTE", resultado.getContent().get(1).status());
        assertEquals(0L, resultado.getContent().get(1).totalVotos());
        assertEquals(false, resultado.getContent().get(1).apoiadoPorMim());
    }
}
