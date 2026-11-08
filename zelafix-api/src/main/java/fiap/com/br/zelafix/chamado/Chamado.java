package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.comentario.Comentario;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.historico.HistStatus;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.status.StatusChamadoEnum;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.voto.VotoChamado;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chamado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_chamado")
    private Long idChamado;

    @Column(name = "protocolo", length = 30)
    private String protocolo;

    @Column(name = "titulo", length = 100, nullable = false)
    private String titulo;

    @Column(name = "descricao", length = 1000, nullable = false)
    private String descricao;

    @Column(name = "foto_url", length = 255, nullable = false)
    private String fotoUrl;

    @Column(name = "dt_abertura", nullable = false)
    private LocalDateTime dtAbertura;

    @Column(name = "dt_conclusao")
    private LocalDateTime dtConclusao;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", length = 20)
    @Builder.Default
    private UrgenciaChamado prioridade = UrgenciaChamado.MEDIA;

    @Column(name = "parecer_ia", length = 1000)
    private String parecerIa;

    @Column(name = "orgao_responsavel", length = 100)
    private String orgaoResponsavel;

    @Column(name = "feedback_resolucao", length = 500)
    private String feedbackResolucao;

    @Column(name = "apoios_count")
    @Builder.Default
    private Integer apoiosCount = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "endereco_id_endereco", nullable = false)
    private Endereco endereco;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id_status", nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id_categoria", nullable = false)
    private Categoria categoria;

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dtComentario DESC")
    @Builder.Default
    private List<Comentario> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VotoChamado> votos = new ArrayList<>();

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dtMudanca DESC")
    @Builder.Default
    private List<HistStatus> historico = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.dtAbertura == null) {
            this.dtAbertura = LocalDateTime.now();
        }
        if (this.protocolo == null || this.protocolo.isBlank()) {
            this.protocolo = "ZF-" + LocalDateTime.now().getYear() + "-" + String.format("%05d", (int) (Math.random() * 90000) + 10000);
        }
        if (this.apoiosCount == null) {
            this.apoiosCount = 0;
        }
    }

    public void atualizarStatus(Status novoStatus, String feedback) {
        this.status = novoStatus;
        if (feedback != null && !feedback.isBlank()) {
            this.feedbackResolucao = feedback.trim();
        }
        if (StatusChamadoEnum.fromString(novoStatus.getNmStatus()) == StatusChamadoEnum.CONCLUIDO) {
            this.dtConclusao = LocalDateTime.now();
        }
    }

    public void incrementarApoio() {
        this.apoiosCount = (this.apoiosCount != null ? this.apoiosCount : 0) + 1;
    }

    public void decrementarApoio() {
        this.apoiosCount = Math.max(0, (this.apoiosCount != null ? this.apoiosCount : 1) - 1);
    }
}

