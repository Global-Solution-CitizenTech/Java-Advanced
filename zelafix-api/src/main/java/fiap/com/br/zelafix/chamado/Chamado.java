package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.voto.VotoChamado;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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

    @Column(name = "titulo", length = 100, nullable = false)
    private String titulo;

    @Column(name = "descricao", length = 1000, nullable = false)
    private String descricao;

    @Lob
    @Column(name = "foto", nullable = false)
    private byte[] foto;

    @Column(name = "dt_abertura", nullable = false)
    private LocalDateTime dtAbertura;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "endereco_id_endereco", nullable = false)
    private Endereco endereco;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id_categoria", nullable = false)
    private Categoria categoria;

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("idHist DESC")
    @Builder.Default
    private List<HistChamado> historico = new ArrayList<>();

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VotoChamado> votos = new ArrayList<>();

    public Status getStatus() {
        return (historico != null && !historico.isEmpty()) ? historico.get(0).getStatus() : null;
    }

    public int getApoiosCount() {
        return (votos != null) ? votos.size() : 0;
    }

    public String getProtocolo() {
        return String.format("ZFX-%05d", idChamado != null ? idChamado : 0);
    }

    public LocalDateTime getDtConclusao() {
        if (historico != null) {
            for (HistChamado h : historico) {
                if (h.getStatus() != null && "CONCLUIDO".equalsIgnoreCase(h.getStatus().getNmStatus())) {
                    return h.getDtMudanca();
                }
            }
        }
        return null;
    }

    public String getFotoUrl() {
        return idChamado != null ? "/chamados/" + idChamado + "/foto" : "/chamados/foto";
    }

    public UrgenciaChamado getPrioridade() {
        return UrgenciaChamado.MEDIA;
    }

    public String getParecerIa() {
        if (historico != null) {
            for (HistChamado h : historico) {
                if (h.getObservacoes() != null && h.getObservacoes().contains("Spring AI")) {
                    return h.getObservacoes();
                }
            }
        }
        return null;
    }

    public String getOrgaoResponsavel() {
        if (categoria != null) {
            try {
                return ClassificacaoChamado.fromString(categoria.getNmCategoria())
                        .getOrgaoCompetente()
                        .getDescricao();
            } catch (Exception ignored) {}
        }
        return "Secretaria Municipal de Serviços Urbanos";
    }

    public String getFeedbackResolucao() {
        if (historico != null) {
            for (HistChamado h : historico) {
                if (h.getStatus() != null && ("CONCLUIDO".equalsIgnoreCase(h.getStatus().getNmStatus()) || "REJEITADO".equalsIgnoreCase(h.getStatus().getNmStatus()))) {
                    return h.getObservacoes();
                }
            }
        }
        return null;
    }

    @PrePersist
    public void prePersist() {
        if (this.dtAbertura == null) {
            this.dtAbertura = LocalDateTime.now();
        }
    }
}
