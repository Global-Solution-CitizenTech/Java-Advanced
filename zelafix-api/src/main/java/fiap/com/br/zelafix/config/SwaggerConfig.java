package fiap.com.br.zelafix.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Comparator;
import java.util.List;

@Configuration
@OpenAPIDefinition(
        servers = {
                @Server(url = "/", description = "Servidor Atual / Base URL")
        },
        info = @Info(
                title = "ZelaFix API — Zeladoria Urbana Colaborativa",
                description = "API RESTful da solucao CitizenTech ZelaFix para zeladoria urbana inteligente. Conecta cidadaos e gestores municipais, com triagem automatica via Spring AI e mensageria assincrona RabbitMQ.",
                version = "1.0.0",
                contact = @Contact(
                        name = "FIAP - Global Solution 2026/2 (Turma 2TDSPG)",
                        url = "https://github.com/Global-Solution-CitizenTech"
                )
        ),
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {
                @Tag(name = "Autenticação", description = "Login, emissão de token JWT e cadastro de usuários"),
                @Tag(name = "Usuarios", description = "Perfil e gestão de usuários munícipes e gestores"),
                @Tag(name = "Chamados", description = "CRUD de ocorrências de zeladoria, paginação e filtros"),
                @Tag(name = "Históricos", description = "CRUD de eventos de auditoria e timeline de chamados"),
                @Tag(name = "Endereco", description = "Consulta e gestão de endereços e CEPs")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Informe o token Bearer JWT obtido no endpoint de login"
)
public class SwaggerConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui.html");
    }

    @Bean
    public OpenApiCustomizer sortTagsCustomizer() {
        return openApi -> {
            if (openApi.getTags() != null) {
                var ordem = List.of("Autenticação", "Usuarios", "Chamados", "Históricos", "Endereco");
                openApi.getTags().sort(Comparator.comparingInt(tag -> {
                    int index = ordem.indexOf(tag.getName());
                    return index == -1 ? Integer.MAX_VALUE : index;
                }));
            }
        };
    }
}

