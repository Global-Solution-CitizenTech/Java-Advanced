package fiap.com.br.zelafix.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
                @Tag(name = "Autenticacao", description = "Login, emissao de token JWT e cadastro de usuarios"),
                @Tag(name = "Usuarios", description = "Perfil e gestao de usuarios munícipes e gestores"),
                @Tag(name = "Chamados", description = "CRUD de ocorrencias de zeladoria, paginacao e filtros"),
                @Tag(name = "Votos & Apoios", description = "Mecanismo de votacao comunitaria assincrona"),
                @Tag(name = "Categorias", description = "Catalogo de categorias de zeladoria urbana"),
                @Tag(name = "Status", description = "Estados do ciclo de vida dos chamados")
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
}

