package fiap.com.br.zelafix.worker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade de Chamado utilizada pelo Worker para atualizações assíncronas dos pareceres de IA
 * e agregação em lote de votos/apoios.
 * Mapeada diretamente para a tabela 'chamado' do citizenTech.ddl.
 */
@Entity
@Table(name = "chamado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChamadoWorkerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_chamado")
    private Long idChamado;

    @Column(name = "protocolo", length = 30)
    private String protocolo;

    @Column(name = "titulo", length = 100)
    private String titulo;

    @Column(name = "descricao", length = 1000)
    private String descricao;

    @Column(name = "prioridade", length = 20)
    private String prioridade;

    @Column(name = "parecer_ia", length = 1000)
    private String parecerIa;

    @Column(name = "orgao_responsavel", length = 100)
    private String orgaoResponsavel;

    @Column(name = "apoios_count")
    private Integer apoiosCount;
}
