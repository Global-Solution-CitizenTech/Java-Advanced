package fiap.com.br.zelafix.historico;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.ChamadoRepository;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.historico.dto.HistoricoRequest;
import fiap.com.br.zelafix.historico.dto.HistoricoResponse;
import fiap.com.br.zelafix.historico.dto.HistoricoUpdateRequest;
import fiap.com.br.zelafix.historico.status.Status;
import fiap.com.br.zelafix.historico.status.StatusChamadoEnum;
import fiap.com.br.zelafix.historico.status.StatusService;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoServiceTest {

    @Mock
    private HistChamadoRepository histChamadoRepository;

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private StatusService statusService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private HistoricoService historicoService;

    private Chamado chamado;
    private Usuario usuario;
    private Status status;
    private HistChamado historico;

    @BeforeEach
    void setUp() {
        chamado = Chamado.builder().idChamado(1L).build();
        usuario = Usuario.builder().idUsuario(1L).nmUsuario("Admin").email("admin@zelafix.com").build();
        status = Status.builder().idStatus(1L).nmStatus(StatusChamadoEnum.PENDENTE).build();
        historico = HistChamado.builder()
                .idHist(10L)
                .chamado(chamado)
                .usuario(usuario)
                .status(status)
                .dtMudanca(LocalDateTime.now())
                .observacoes("Primeiro registro")
                .build();
    }

    @Test
    @DisplayName("Deve listar todos os históricos paginados")
    void deveListarHistoricosPaginados() {
        Pageable pageable = PageRequest.of(0, 10);
        when(histChamadoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(historico)));

        Page<HistoricoResponse> result = historicoService.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().getFirst().id());
    }

    @Test
    @DisplayName("Deve buscar histórico por ID")
    void deveBuscarHistoricoPorId() {
        when(histChamadoRepository.findById(10L)).thenReturn(Optional.of(historico));

        HistoricoResponse response = historicoService.findById(10L);

        assertNotNull(response);
        assertEquals(10L, response.id());
    }

    @Test
    @DisplayName("Deve listar histórico por chamadoId")
    void deveListarHistoricoPorChamadoId() {
        when(histChamadoRepository.findByChamadoIdChamadoOrderByDtMudancaDescIdHistDesc(1L))
                .thenReturn(List.of(historico));

        List<HistoricoResponse> list = historicoService.findByChamadoId(1L);

        assertEquals(1, list.size());
        assertEquals(10L, list.getFirst().id());
    }

    @Test
    @DisplayName("Deve lançar erro ao buscar histórico inexistente")
    void deveLancarErroQuandoHistoricoNaoExiste() {
        when(histChamadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> historicoService.findById(99L));
    }

    @Test
    @DisplayName("Deve criar histórico com sucesso")
    void deveCriarHistoricoComSucesso() {
        HistoricoRequest request = new HistoricoRequest(1L, "EM_ANALISE", "Iniciada análise");
        Status novoStatus = Status.builder().idStatus(2L).nmStatus(StatusChamadoEnum.EM_ANALISE).build();

        when(chamadoRepository.findById(1L)).thenReturn(Optional.of(chamado));
        when(usuarioService.findByEmail("admin@zelafix.com")).thenReturn(usuario);
        when(statusService.findByNome(StatusChamadoEnum.EM_ANALISE)).thenReturn(novoStatus);
        when(histChamadoRepository.save(any(HistChamado.class))).thenReturn(historico);

        HistoricoResponse response = historicoService.create(request, "admin@zelafix.com");

        assertNotNull(response);
        verify(histChamadoRepository).save(any(HistChamado.class));
    }

    @Test
    @DisplayName("Deve atualizar histórico com sucesso")
    void deveAtualizarHistoricoComSucesso() {
        HistoricoUpdateRequest request = new HistoricoUpdateRequest("CONCLUIDO", "Serviço finalizado");
        Status novoStatus = Status.builder().idStatus(4L).nmStatus(StatusChamadoEnum.CONCLUIDO).build();

        when(histChamadoRepository.findById(10L)).thenReturn(Optional.of(historico));
        when(statusService.findByNome(StatusChamadoEnum.CONCLUIDO)).thenReturn(novoStatus);
        when(histChamadoRepository.save(historico)).thenReturn(historico);

        HistoricoResponse response = historicoService.update(10L, request);

        assertNotNull(response);
        verify(histChamadoRepository).save(historico);
    }

    @Test
    @DisplayName("Deve excluir histórico que não seja o inicial")
    void deveExcluirHistoricoNaoInicial() {
        HistChamado hInicial = HistChamado.builder().idHist(1L).chamado(chamado).build();
        HistChamado hSecundario = HistChamado.builder().idHist(2L).chamado(chamado).build();

        when(histChamadoRepository.findById(2L)).thenReturn(Optional.of(hSecundario));
        when(histChamadoRepository.findFirstByChamadoIdChamadoOrderByDtMudancaAscIdHistAsc(1L)).thenReturn(Optional.of(hInicial));

        historicoService.delete(2L);

        verify(histChamadoRepository).delete(hSecundario);
    }

    @Test
    @DisplayName("Deve bloquear exclusão do histórico inicial")
    void deveBloquearExclusaoHistoricoInicial() {
        HistChamado hInicial = HistChamado.builder().idHist(1L).chamado(chamado).build();

        when(histChamadoRepository.findById(1L)).thenReturn(Optional.of(hInicial));
        when(histChamadoRepository.findFirstByChamadoIdChamadoOrderByDtMudancaAscIdHistAsc(1L)).thenReturn(Optional.of(hInicial));

        assertThrows(IllegalStateException.class, () -> historicoService.delete(1L));
    }
}
