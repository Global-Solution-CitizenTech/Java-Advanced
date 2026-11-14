package fiap.com.br.zelafix.chamado.orgao;

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
@Table(name = "orgao_governo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgaoGoverno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orgao")
    private Long idOrgao;

    @Enumerated(EnumType.STRING)
    @Column(name = "orgao_governo", length = 100, nullable = false, unique = true)
    private OrgaoGovernoEnum nmOrgao;
}
