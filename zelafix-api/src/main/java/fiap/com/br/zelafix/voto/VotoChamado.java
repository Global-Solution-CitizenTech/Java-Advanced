package fiap.com.br.zelafix.voto;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "voto_chamado",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chamado_id_chamado", "usuario_id_usuario"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotoChamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_voto")
    private Long idVoto;

    @Column(name = "dt_voto", nullable = false)
    private LocalDateTime dtVoto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_id_chamado", nullable = false)
    private Chamado chamado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id_usuario", nullable = false)
    private Usuario usuario;

    public VotoChamado(Chamado chamado, Usuario usuario) {
        this.chamado = chamado;
        this.usuario = usuario;
        this.dtVoto = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.dtVoto == null) {
            this.dtVoto = LocalDateTime.now();
        }
    }
}
