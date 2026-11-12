package fiap.com.br.zelafix.usuario.telefone;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fiap.com.br.zelafix.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "telefone")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Telefone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_telefone")
    private Long idTelefone;

    @Column(name = "num_ddd", nullable = false, length = 2)
    private String numDdd;

    @Column(name = "num_tel", nullable = false, length = 9)
    private String numTel;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id_usuario", nullable = false, unique = true)
    @JsonIgnore
    private Usuario usuario;
}
