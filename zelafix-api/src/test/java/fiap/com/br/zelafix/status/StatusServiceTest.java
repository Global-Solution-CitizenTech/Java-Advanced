package fiap.com.br.zelafix.status;

import fiap.com.br.zelafix.exception.BusinessException;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.status.dto.StatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private StatusRepository statusRepository;

    @InjectMocks
    private StatusService statusService;

    private Status statusAberto;

    @BeforeEach
    void setUp() {
        statusAberto = Status.builder()
                .idStatus(1L)
                .nmStatus("ABERTO")
                .build();
    }

    @Test
    @DisplayName("Deve listar todos os status cadastrados")
    void deveListarTodosOsStatus() {
        when(statusRepository.findAll()).thenReturn(List.of(statusAberto));

        List<Status> resultado = statusService.findAll();

        assertEquals(1, resultado.size());
        assertEquals("ABERTO", resultado.getFirst().getNmStatus());
        verify(statusRepository).findAll();
    }

    @Test
    @DisplayName("Deve buscar status por ID com sucesso")
    void deveBuscarStatusPorIdComSucesso() {
        when(statusRepository.findById(1L)).thenReturn(Optional.of(statusAberto));

        Status encontrado = statusService.findById(1L);

        assertNotNull(encontrado);
        assertEquals(1L, encontrado.getIdStatus());
        assertEquals("ABERTO", encontrado.getNmStatus());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando status não existir")
    void deveLancarExcecaoQuandoStatusNaoExistir() {
        when(statusRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> statusService.findById(99L));
    }

    @Test
    @DisplayName("Deve retornar status existente em findByNameOrCreate")
    void deveRetornarStatusExistenteEmFindByNameOrCreate() {
        when(statusRepository.findByNmStatusIgnoreCase("ABERTO")).thenReturn(Optional.of(statusAberto));

        Status resultado = statusService.findByNameOrCreate("ABERTO");

        assertEquals(1L, resultado.getIdStatus());
        assertEquals("ABERTO", resultado.getNmStatus());
    }

    @Test
    @DisplayName("Deve criar status em findByNameOrCreate quando não existir")
    void deveCriarStatusEmFindByNameOrCreate() {
        when(statusRepository.findByNmStatusIgnoreCase("EM_ANDAMENTO")).thenReturn(Optional.empty());
        when(statusRepository.save(any(Status.class))).thenReturn(new Status(2L, "EM_ANDAMENTO"));

        Status resultado = statusService.findByNameOrCreate("EM_ANDAMENTO");

        assertNotNull(resultado);
        assertEquals("EM_ANDAMENTO", resultado.getNmStatus());
    }

    @Test
    @DisplayName("Deve criar status com sucesso e normalizar para maiúsculas")
    void deveCriarStatusComSucesso() {
        StatusRequest req = new StatusRequest("concluido");
        when(statusRepository.findByNmStatusIgnoreCase("CONCLUIDO")).thenReturn(Optional.empty());
        when(statusRepository.save(any(Status.class))).thenReturn(new Status(3L, "CONCLUIDO"));

        Status criado = statusService.create(req);

        assertNotNull(criado);
        assertEquals("CONCLUIDO", criado.getNmStatus());
        verify(statusRepository).save(any(Status.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao tentar criar status com nome duplicado")
    void deveLancarExcecaoAoCriarStatusComNomeDuplicado() {
        StatusRequest req = new StatusRequest("ABERTO");
        when(statusRepository.findByNmStatusIgnoreCase("ABERTO")).thenReturn(Optional.of(statusAberto));

        BusinessException ex = assertThrows(BusinessException.class, () -> statusService.create(req));
        assertEquals("Status já cadastrado com o nome: ABERTO", ex.getMessage());
    }

    @Test
    @DisplayName("Deve atualizar status com sucesso")
    void deveAtualizarStatusComSucesso() {
        StatusRequest req = new StatusRequest("FINALIZADO");
        when(statusRepository.findById(1L)).thenReturn(Optional.of(statusAberto));
        when(statusRepository.findByNmStatusIgnoreCase("FINALIZADO")).thenReturn(Optional.empty());
        when(statusRepository.save(any(Status.class))).thenReturn(statusAberto);

        Status atualizado = statusService.update(1L, req);

        assertEquals("FINALIZADO", atualizado.getNmStatus());
        verify(statusRepository).save(statusAberto);
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao atualizar status para nome de outro status existente")
    void deveLancarExcecaoAoAtualizarParaNomeExistenteDeOutroStatus() {
        Status outro = new Status(2L, "CANCELADO");
        StatusRequest req = new StatusRequest("CANCELADO");

        when(statusRepository.findById(1L)).thenReturn(Optional.of(statusAberto));
        when(statusRepository.findByNmStatusIgnoreCase("CANCELADO")).thenReturn(Optional.of(outro));

        BusinessException ex = assertThrows(BusinessException.class, () -> statusService.update(1L, req));
        assertEquals("Já existe um status cadastrado com o nome: CANCELADO", ex.getMessage());
    }

    @Test
    @DisplayName("Deve excluir status existente com sucesso")
    void deveExcluirStatusComSucesso() {
        when(statusRepository.findById(1L)).thenReturn(Optional.of(statusAberto));

        statusService.delete(1L);

        verify(statusRepository).delete(statusAberto);
    }
}
