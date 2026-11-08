package fiap.com.br.zelafix.endereco;

import fiap.com.br.zelafix.endereco.dto.ViaCepResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(
        url = "https://viacep.com.br/ws",
        accept = "application/json"
)
public interface ViaCepService {

    @Cacheable(value = "viacep", key = "#cep")
    @GetExchange("/{cep}/json")
    ViaCepResponse getEnderecoPorCep(@PathVariable("cep") String cep);
}
