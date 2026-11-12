package fiap.com.br.zelafix.endereco;

import fiap.com.br.zelafix.endereco.dto.ViaCepResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepServiceTest {

    private MockRestServiceServer mockServer;
    private ViaCepService viaCepService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://viacep.com.br/ws");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClientAdapter adapter = RestClientAdapter.create(builder.build());
        viaCepService = HttpServiceProxyFactory.builderFor(adapter).build().createClient(ViaCepService.class);
    }

    @Test
    @DisplayName("Deve consultar endereço no ViaCEP com sucesso para CEP válido")
    void deveConsultarCepValidoComSucesso() {
        String json = """
                {
                  "cep": "01311-000",
                  "logradouro": "Avenida Paulista",
                  "bairro": "Bela Vista",
                  "localidade": "São Paulo",
                  "estado": "São Paulo",
                  "erro": false
                }
                """;
        mockServer.expect(requestTo("https://viacep.com.br/ws/01311000/json"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        ViaCepResponse resposta = viaCepService.getEnderecoPorCep("01311000");

        assertNotNull(resposta);
        assertEquals("Avenida Paulista", resposta.logradouro());
        assertEquals("Bela Vista", resposta.bairro());
        assertEquals("São Paulo", resposta.localidade());
        assertEquals("São Paulo", resposta.estado());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve retornar erro=true quando CEP não existir no ViaCEP")
    void deveRetornarErroQuandoCepNaoExistir() {
        mockServer.expect(requestTo("https://viacep.com.br/ws/99999999/json"))
                .andRespond(withSuccess("{\"erro\": true}", MediaType.APPLICATION_JSON));

        ViaCepResponse resposta = viaCepService.getEnderecoPorCep("99999999");

        assertNotNull(resposta);
        assertTrue(Boolean.TRUE.equals(resposta.erro()));
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve propagar erro HTTP quando o serviço ViaCEP estiver indisponível")
    void devePropagarErroHttpQuandoServicoIndisponivel() {
        mockServer.expect(requestTo("https://viacep.com.br/ws/01001000/json"))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class, () -> viaCepService.getEnderecoPorCep("01001000"));
        mockServer.verify();
    }
}
