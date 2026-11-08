package fiap.com.br.zelafix.comentario;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "comentario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comentario")
    private Long idComentario;

    @Column(name = "conteudo", length = 500, nullable = false)
    private String conteudo;

    @Column(name = "dt_comentario", nullable = false)
    private LocalDateTime dtComentario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_id_chamado", nullable = false)
    private Chamado chamado;

    @PrePersist
    public void prePersist() {
        if (this.dtComentario == null) {
            this.dtComentario = LocalDateTime.now();
        }
    }
}

