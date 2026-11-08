package fiap.com.br.zelafix.endereco.bairro;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "bairro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bairro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bairro")
    private Long id;

    @Column(name = "nm_bairro", nullable = false, length = 30)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cidade_id_cidade")
    private Cidade cidade;

    @OneToMany(mappedBy = "bairro")
    @JsonIgnore
    @Builder.Default
    private Set<Endereco> enderecos = new HashSet<>();
}
