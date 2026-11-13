package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.categoria.CategoriaService;
import fiap.com.br.zelafix.chamado.dto.ChamadoRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.ChamadoStatusRequest;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.endereco.estado.Estado;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.HistChamadoRepository;
import fiap.com.br.zelafix.chamado.orgao.OrgaoGoverno;
import fiap.com.br.zelafix.chamado.orgao.OrgaoGovernoService;
import fiap.com.br.zelafix.chamado.prioridade.Prioridade;
import fiap.com.br.zelafix.chamado.prioridade.PrioridadeService;
import fiap.com.br.zelafix.queue.dto.ChamadoTriagemMessage;
import fiap.com.br.zelafix.queue.dto.TriagemFeedbackMessage;
import fiap.com.br.zelafix.queue.dto.VotoMessage;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    private PrioridadeService prioridadeService;
    @Mock
    private OrgaoGovernoService orgaoGovernoService;
    @Mock
    private StatusService statusService;
    @Mock
    private EnderecoService enderecoService;
    @Mock
    private HistChamadoRepository histChamadoRepository;
    @Mock
    private VotoChamadoRepository votoChamadoRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChamadoService chamadoService;

    private Usuario usuario;
    private Categoria categoria;
    private Prioridade prioridadePendente;
    private OrgaoGoverno orgaoInicial;
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

        prioridadePendente = Prioridade.builder()
                .idPrioridade(5L)
                .nmPrioridade("PENDENTE")
                .build();

        orgaoInicial = OrgaoGoverno.builder()
                .idOrgao(7L)
                .nmOrgao("Em análise pela IA")
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
        byte[] fotoBytes = "bytes".getBytes(StandardCharsets.UTF_8);

        when(usuarioService.findByEmail(usuario.getEmail())).thenReturn(usuario);
        when(categoriaService.findDefault()).thenReturn(categoria);
        when(prioridadeService.findByNameOrCreate("PENDENTE")).thenReturn(prioridadePendente);
        when(orgaoGovernoService.findByNameOrCreate("Em análise pela IA")).thenReturn(orgaoInicial);
        when(statusService.findByNameOrCreate("PENDENTE")).thenReturn(statusPendente);
        when(enderecoService.findOrCreateByCepAndNumero(any())).thenReturn(endereco);

        Chamado chamadoSalvo = Chamado.builder()
                .idChamado(100L)
                .titulo("Ocorrência em análise pela IA")
                .descricao("Aguardando análise técnica automática da imagem enviada...")
                .foto(fotoBytes)
                .categoria(categoria)
                .prioridade(prioridadePendente)
                .orgaoGoverno(orgaoInicial)
                .usuario(usuario)
                .endereco(endereco)
                .dtAbertura(LocalDateTime.now())
                .build();

        when(chamadoRepository.save(any())).thenReturn(chamadoSalvo);

        ChamadoResponse response = chamadoService.create(enderecoRequest, fotoBytes, usuario.getEmail());

        assertNotNull(response);
        assertEquals(100L, response.idChamado());
        assertEquals("Ocorrência em análise pela IA", response.titulo());
        assertEquals("PENDENTE", response.prioridadeNome());
        assertEquals("Em análise pela IA", response.orgaoNome());
        assertEquals("PENDENTE", response.status());

        ArgumentCaptor<ChamadoTriagemMessage> triagemCaptor = ArgumentCaptor.forClass(ChamadoTriagemMessage.class);
        verify(eventPublisher, times(1)).publishEvent(triagemCaptor.capture());
        assertArrayEquals(chamadoSalvo.getFoto(), triagemCaptor.getValue().foto());
        assertEquals("Pinheiros", triagemCaptor.getValue().bairro());
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
                .prioridade(prioridadePendente)
                .orgaoGoverno(orgaoInicial)
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .dtAbertura(LocalDateTime.now())
                .build();

        when(chamadoRepository.findById(200L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(usuario.getEmail())).thenReturn(usuario);
        when(votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(200L, 1L))
                .thenReturn(Optional.empty());

        ChamadoResponse response = chamadoService.apoiar(200L, usuario.getEmail());

        assertNotNull(response);
        verify(votoChamadoRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publishEvent(any(VotoMessage.class));
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
                .prioridade(prioridadePendente)
                .orgaoGoverno(orgaoInicial)
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .dtAbertura(LocalDateTime.now())
                .build();

        VotoChamado voto = VotoChamado.builder().chamado(chamado).usuario(usuario).dtVoto(LocalDateTime.now()).build();

        when(chamadoRepository.findById(300L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(usuario.getEmail())).thenReturn(usuario);
        when(votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(300L, 1L))
                .thenReturn(Optional.of(voto));

        ChamadoResponse response = chamadoService.apoiar(300L, usuario.getEmail());

        assertNotNull(response);
        verify(votoChamadoRepository, times(1)).delete(voto);
        verify(eventPublisher, times(1)).publishEvent(any(VotoMessage.class));
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

        ChamadoStatusRequest request = new ChamadoStatusRequest("CONCLUIDO", "Resolvido");

        assertThrows(IllegalArgumentException.class, () ->
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
                .prioridade(prioridadePendente)
                .orgaoGoverno(orgaoInicial)
                .endereco(endereco)
                .usuario(usuario)
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .dtAbertura(LocalDateTime.now())
                .build();

        when(chamadoRepository.findById(500L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(gestor.getEmail())).thenReturn(gestor);
        when(statusService.findByNameOrCreate("CONCLUIDO")).thenReturn(statusNovo);

        ChamadoStatusRequest request = new ChamadoStatusRequest("CONCLUIDO", "Serviço finalizado com sucesso.");

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
                .prioridade(prioridadePendente)
                .orgaoGoverno(orgaoInicial)
                .endereco(endereco)
                .usuario(usuario)
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .dtAbertura(LocalDateTime.now())
                .build();

        when(chamadoRepository.findById(600L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(admin.getEmail())).thenReturn(admin);
        when(statusService.findByNameOrCreate("CONCLUIDO")).thenReturn(statusNovo);

        ChamadoStatusRequest request = new ChamadoStatusRequest("CONCLUIDO", "Resolvido por intervenção direta de admin.");

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

        verify(votoChamadoRepository).deleteByChamadoIdChamado(700L);
        verify(histChamadoRepository).deleteByChamadoIdChamado(700L);
        verify(chamadoRepository, times(1)).delete(chamado);
    }

    @Test
    @DisplayName("Deve buscar chamados paginados com carregamento em lote de status, votos e apoio (sem N+1)")
    void deveBuscarChamadosComCarregamentoEmLote() {
        Chamado c1 = Chamado.builder().idChamado(10L).titulo("Buraco 1").categoria(categoria).prioridade(prioridadePendente).orgaoGoverno(orgaoInicial).usuario(usuario).endereco(endereco).dtAbertura(LocalDateTime.now()).build();
        Chamado c2 = Chamado.builder().idChamado(20L).titulo("Buraco 2").categoria(categoria).prioridade(prioridadePendente).orgaoGoverno(orgaoInicial).usuario(usuario).endereco(endereco).dtAbertura(LocalDateTime.now()).build();
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

    @Test
    @DisplayName("Deve aplicar triagem da IA diretamente no chamado com categoria, prioridade e órgão")
    void deveAplicarTriagemIa() {
        Chamado chamado = Chamado.builder().idChamado(100L).build();
        Categoria catIa = Categoria.builder().idCategoria(2L).nmCategoria("Vias e Pavimentação").build();
        Prioridade prioAlta = Prioridade.builder().idPrioridade(3L).nmPrioridade("ALTA").build();
        OrgaoGoverno orgao = OrgaoGoverno.builder().idOrgao(1L).nmOrgao("Secretaria Municipal de Infraestrutura Urbana e Obras").build();

        when(chamadoRepository.findById(100L)).thenReturn(Optional.of(chamado));
        when(categoriaService.findByNameOrCreate("Vias e Pavimentação")).thenReturn(catIa);
        when(prioridadeService.findByNameOrCreate("ALTA")).thenReturn(prioAlta);
        when(orgaoGovernoService.findByNameOrCreate("Secretaria Municipal de Infraestrutura Urbana e Obras")).thenReturn(orgao);

        var feedback = new TriagemFeedbackMessage(
                100L,
                "Cratera aberta no asfalto",
                "Reparo emergencial recomendado devido ao risco viário.",
                "Vias e Pavimentação",
                "ALTA",
                "Secretaria Municipal de Infraestrutura Urbana e Obras"
        );
        chamadoService.aplicarTriagemIa(feedback);

        ArgumentCaptor<Chamado> chamadoCaptor = ArgumentCaptor.forClass(Chamado.class);
        verify(chamadoRepository).save(chamadoCaptor.capture());
        Chamado atualizado = chamadoCaptor.getValue();
        assertEquals(100L, atualizado.getIdChamado());
        assertEquals("Cratera aberta no asfalto", atualizado.getTitulo());
        assertEquals("Reparo emergencial recomendado devido ao risco viário.", atualizado.getDescricao());
        assertEquals(catIa, atualizado.getCategoria());
        assertEquals(prioAlta, atualizado.getPrioridade());
        assertEquals(orgao, atualizado.getOrgaoGoverno());
    }

    @Test
    @DisplayName("GESTOR tem permissão para editar dados do chamado")
    void devePermitirGestorEditarChamado() {
        Usuario gestor = Usuario.builder()
                .idUsuario(99L)
                .email("gestor.obras@subprefeitura.sp.gov.br")
                .cargo(CargoUsuario.GESTOR)
                .build();

        Chamado chamado = Chamado.builder()
                .idChamado(150L)
                .titulo("Título Original")
                .descricao("Descrição Original")
                .usuario(usuario) // criado pelo cidadão (id 1)
                .categoria(categoria)
                .prioridade(prioridadePendente)
                .orgaoGoverno(orgaoInicial)
                .endereco(endereco)
                .foto("bytes".getBytes(StandardCharsets.UTF_8))
                .dtAbertura(LocalDateTime.now())
                .build();

        ChamadoRequest requestEdicao = new ChamadoRequest(
                "Título Atualizado pelo Gestor",
                "Descrição atualizada para obras",
                1L,
                new EnderecoRequest("01311000", "50", "Em frente ao parque", "Avenida Paulista")
        );

        when(chamadoRepository.findById(150L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(gestor.getEmail())).thenReturn(gestor);
        when(categoriaService.findById(1L)).thenReturn(categoria);
        when(enderecoService.findOrCreateByCepAndNumero(any())).thenReturn(endereco);
        when(chamadoRepository.save(any())).thenReturn(chamado);
        HistChamado histMock = HistChamado.builder()
                .idHist(1L)
                .dtMudanca(LocalDateTime.now())
                .usuario(usuario)
                .status(statusPendente)
                .observacoes("Histórico inicial")
                .build();
        when(histChamadoRepository.findByChamadoIdChamadoOrderByDtMudancaDescIdHistDesc(150L))
                .thenReturn(List.of(histMock));

        ChamadoResponse response = chamadoService.update(150L, requestEdicao, gestor.getEmail());

        assertNotNull(response);
        ArgumentCaptor<Chamado> chamadoCaptor = ArgumentCaptor.forClass(Chamado.class);
        verify(chamadoRepository).save(chamadoCaptor.capture());
        Chamado atualizado = chamadoCaptor.getValue();
        assertEquals(150L, atualizado.getIdChamado());
        assertEquals("Título Atualizado pelo Gestor", atualizado.getTitulo());
        assertEquals("Descrição atualizada para obras", atualizado.getDescricao());
        assertEquals(prioridadePendente, atualizado.getPrioridade());
        assertEquals(orgaoInicial, atualizado.getOrgaoGoverno());
    }

    @Test
    @DisplayName("Outro cidadão deve ser bloqueado ao tentar editar chamado alheio")
    void deveBloquearOutroCidadaoAoEditarChamado() {
        Usuario outroCidadao = Usuario.builder()
                .idUsuario(88L)
                .email("outro.cidadao@email.com")
                .cargo(CargoUsuario.CIDADAO)
                .build();

        Chamado chamado = Chamado.builder()
                .idChamado(151L)
                .titulo("Título Original")
                .usuario(usuario) // criado por usuario (id 1)
                .build();

        ChamadoRequest requestEdicao = new ChamadoRequest(
                "Invasão de chamado", "Desc", 1L,
                new EnderecoRequest("01311000", "50", "Referência", "Avenida Paulista")
        );

        when(chamadoRepository.findById(151L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail(outroCidadao.getEmail())).thenReturn(outroCidadao);

        assertThrows(IllegalArgumentException.class, () ->
                chamadoService.update(151L, requestEdicao, outroCidadao.getEmail())
        );
    }
}
