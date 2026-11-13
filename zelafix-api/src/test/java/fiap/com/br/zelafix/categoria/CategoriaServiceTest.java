package fiap.com.br.zelafix.categoria;

import fiap.com.br.zelafix.categoria.dto.CategoriaRequest;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoriaAsfalto;

    @BeforeEach
    void setUp() {
        categoriaAsfalto = Categoria.builder()
                .idCategoria(1L)
                .nmCategoria("Buraco na via")
                .build();
    }

    @Test
    @DisplayName("Deve listar todas as categorias cadastradas")
    void deveListarTodasAsCategorias() {
        when(categoriaRepository.findAll()).thenReturn(List.of(categoriaAsfalto));

        List<Categoria> resultado = categoriaService.findAll();

        assertEquals(1, resultado.size());
        assertEquals("Buraco na via", resultado.getFirst().getNmCategoria());
        verify(categoriaRepository).findAll();
    }

    @Test
    @DisplayName("Deve buscar categoria por ID com sucesso")
    void deveBuscarCategoriaPorIdComSucesso() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaAsfalto));

        Categoria encontrada = categoriaService.findById(1L);

        assertNotNull(encontrada);
        assertEquals(1L, encontrada.getIdCategoria());
        assertEquals("Buraco na via", encontrada.getNmCategoria());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando categoria não existir por ID")
    void deveLancarExcecaoQuandoCategoriaNaoExistirPorId() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoriaService.findById(99L));
    }

    @Test
    @DisplayName("Deve criar categoria com sucesso")
    void deveCriarCategoriaComSucesso() {
        CategoriaRequest req = new CategoriaRequest("Sinalização");
        when(categoriaRepository.findByNmCategoriaIgnoreCase("Sinalização")).thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(new Categoria(3L, "Sinalização"));

        Categoria criada = categoriaService.create(req);

        assertNotNull(criada);
        assertEquals("Sinalização", criada.getNmCategoria());
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao tentar criar categoria com nome duplicado")
    void deveLancarExcecaoAoCriarCategoriaComNomeDuplicado() {
        CategoriaRequest req = new CategoriaRequest("Buraco na via");
        when(categoriaRepository.findByNmCategoriaIgnoreCase("Buraco na via")).thenReturn(Optional.of(categoriaAsfalto));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> categoriaService.create(req));
        assertEquals("Categoria já cadastrada com o nome: Buraco na via", ex.getMessage());
    }

    @Test
    @DisplayName("Deve atualizar nome de categoria com sucesso")
    void deveAtualizarNomeDeCategoriaComSucesso() {
        CategoriaRequest req = new CategoriaRequest("Buraco na via - Asfalto");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaAsfalto));
        when(categoriaRepository.findByNmCategoriaIgnoreCase("Buraco na via - Asfalto")).thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoriaAsfalto);

        Categoria atualizada = categoriaService.update(1L, req);

        assertEquals("Buraco na via - Asfalto", atualizada.getNmCategoria());
        verify(categoriaRepository).save(categoriaAsfalto);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao atualizar para nome que já pertence a outra categoria")
    void deveLancarExcecaoAoAtualizarParaNomeExistenteDeOutraCategoria() {
        Categoria outra = new Categoria(2L, "Iluminação");
        CategoriaRequest req = new CategoriaRequest("Iluminação");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaAsfalto));
        when(categoriaRepository.findByNmCategoriaIgnoreCase("Iluminação")).thenReturn(Optional.of(outra));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> categoriaService.update(1L, req));
        assertEquals("Já existe uma categoria cadastrada com o nome: Iluminação", ex.getMessage());
    }

    @Test
    @DisplayName("Deve excluir categoria existente com sucesso")
    void deveExcluirCategoriaComSucesso() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaAsfalto));

        categoriaService.delete(1L);

        verify(categoriaRepository).delete(categoriaAsfalto);
    }

    @Test
    @DisplayName("Não deve colidir chaves de cache entre findAll, findDefault, findById e findByNameOrCreate")
    void naoDeveColidirChavesDeCache() {
        when(categoriaRepository.findAll()).thenReturn(List.of(categoriaAsfalto));
        when(categoriaRepository.findFirstByOrderByIdCategoriaAsc()).thenReturn(Optional.of(categoriaAsfalto));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaAsfalto));
        when(categoriaRepository.findByNmCategoriaIgnoreCase("Buraco na via")).thenReturn(Optional.of(categoriaAsfalto));

        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(CategoriaRepository.class, () -> categoriaRepository);
            ctx.register(CacheTestConfig.class, CategoriaService.class);
            ctx.refresh();

            CategoriaService proxied = ctx.getBean(CategoriaService.class);
            List<Categoria> todas = proxied.findAll();
            Categoria padrao = proxied.findDefault();
            Categoria porId = proxied.findById(1L);
            Categoria porNome = proxied.findByNameOrCreate("Buraco na via");

            assertEquals(1, todas.size());
            assertEquals(1L, padrao.getIdCategoria());
            assertEquals(1L, porId.getIdCategoria());
            assertEquals("Buraco na via", porNome.getNmCategoria());
        }
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("categorias");
        }
    }
}
