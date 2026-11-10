package fiap.com.br.zelafix.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:oracle:thin:@localhost:1521/xe",
    "spring.datasource.username=test",
    "spring.datasource.password=test",
    "spring.jpa.hibernate.ddl-auto=none"
})
class ZelaFixWorkerApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }
}

