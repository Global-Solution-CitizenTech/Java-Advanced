package fiap.com.br.zelafix;

import fiap.com.br.zelafix.endereco.ViaCepService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan
@ImportHttpServices(ViaCepService.class)
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ZelaFixApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZelaFixApiApplication.class, args);
    }

}
