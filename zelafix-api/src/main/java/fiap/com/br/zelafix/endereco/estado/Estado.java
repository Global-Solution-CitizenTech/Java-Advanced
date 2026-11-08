package fiap.com.br.zelafix.endereco.estado;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "estado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Long id;

    @Column(name = "nm_estado", nullable = false, length = 30)
    private String nome;

    @OneToMany(mappedBy = "estado")
    @JsonIgnore
    @Builder.Default
    private Set<Cidade> cidades = new HashSet<>();
}
