package fiap.com.br.zelafix.chamado.prioridade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prioridade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prioridade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prioridade")
    private Long idPrioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "nm_prioridade", length = 20, nullable = false, unique = true)
    private PrioridadeEnum nmPrioridade;
}
