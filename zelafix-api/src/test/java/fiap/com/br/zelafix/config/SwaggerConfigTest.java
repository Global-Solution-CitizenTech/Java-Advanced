package fiap.com.br.zelafix.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SwaggerConfigTest {

    @Test
    @DisplayName("Deve registrar redirecionamento da raiz para o Swagger UI")
    void deveRegistrarViewControllers() {
        SwaggerConfig config = new SwaggerConfig();
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        ViewControllerRegistry registry = new ViewControllerRegistry(context);

        assertDoesNotThrow(() -> config.addViewControllers(registry));
    }

    @Test
    @DisplayName("Deve ordenar as tags conforme ordem definida no customizer")
    void deveOrdenarTagsConformeOrdem() {
        SwaggerConfig config = new SwaggerConfig();
        var customizer = config.sortTagsCustomizer();

        OpenAPI openApi = new OpenAPI();
        openApi.setTags(new ArrayList<>(List.of(
                new Tag().name("Endereco"),
                new Tag().name("Chamados"),
                new Tag().name("TagDesconhecida"),
                new Tag().name("Usuarios"),
                new Tag().name("Autenticação")
        )));

        customizer.customise(openApi);

        assertEquals("Autenticação", openApi.getTags().get(0).getName());
        assertEquals("Usuarios", openApi.getTags().get(1).getName());
        assertEquals("Chamados", openApi.getTags().get(2).getName());
        assertEquals("Endereco", openApi.getTags().get(3).getName());
        assertEquals("TagDesconhecida", openApi.getTags().get(4).getName());
    }

    @Test
    @DisplayName("Customizer deve suportar lista de tags nula com segurança")
    void deveTratarListaDeTagsNula() {
        SwaggerConfig config = new SwaggerConfig();
        var customizer = config.sortTagsCustomizer();

        OpenAPI openApi = new OpenAPI();
        openApi.setTags(null);

        assertDoesNotThrow(() -> customizer.customise(openApi));
    }
}
