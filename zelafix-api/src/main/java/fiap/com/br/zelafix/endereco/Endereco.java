package fiap.com.br.zelafix.endereco;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import fiap.com.br.zelafix.usuario.Usuario;
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
@Table(name = "endereco")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_endereco")
    private Long id;

    @Column(name = "cep", nullable = false, length = 8)
    private String cep;

    @Column(name = "numero", nullable = false, length = 100)
    private String numero;

    @Column(name = "rua", nullable = false, length = 100)
    private String rua;

    @Column(name = "complemento", length = 30)
    private String complemento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bairro_id_bairro", nullable = false)
    private Bairro bairro;

    @OneToMany(mappedBy = "endereco")
    @JsonIgnore
    @Builder.Default
    private Set<Usuario> usuarios = new HashSet<>();
}
