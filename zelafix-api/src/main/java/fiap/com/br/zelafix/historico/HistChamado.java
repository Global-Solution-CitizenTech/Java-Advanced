package fiap.com.br.zelafix.historico;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hist_chamado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistChamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_hist")
    private Long idHist;

    @Column(name = "dt_mudanca", nullable = false)
    private LocalDateTime dtMudanca;

    @Column(name = "observacoes", length = 255, nullable = false)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_id_chamado", nullable = false)
    private Chamado chamado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id_status", nullable = false)
    private Status status;
}
