package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.ClassificacaoChamado;
import fiap.com.br.zelafix.chamado.UrgenciaChamado;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.usuario.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ChamadoCreateRequest(
        @NotBlank(message = "O título do chamado é obrigatório")
        @Size(max = 100, message = "O título pode ter no máximo 100 caracteres")
        String titulo,

        @NotBlank(message = "A descrição detalhada é obrigatória")
        @Size(max = 1000, message = "A descrição pode ter no máximo 1000 caracteres")
        String descricao,

        @NotBlank(message = "A categoria é obrigatória")
        String categoria,

        @NotNull(message = "O endereço da ocorrência é obrigatório")
        @Valid
        EnderecoRequest endereco,

        @NotBlank(message = "A URL da foto da ocorrência é obrigatória")
        @Size(max = 255, message = "A URL da foto pode ter no máximo 255 caracteres")
        String fotoUrl,

        String prioridade
) {
    public Chamado toEntity(Usuario autor, Categoria categoria, Endereco endereco, Status statusPendente) {
        String orgao = ClassificacaoChamado.fromString(categoria.getNmCategoria())
                .getOrgaoCompetente()
                .getDescricao();

        return Chamado.builder()
                .titulo(titulo.trim())
                .descricao(descricao.trim())
                .fotoUrl(fotoUrl)
                .dtAbertura(LocalDateTime.now())
                .prioridade(prioridade != null ? UrgenciaChamado.fromString(prioridade) : UrgenciaChamado.MEDIA)
                .orgaoResponsavel(orgao)
                .usuario(autor)
                .endereco(endereco)
                .status(statusPendente)
                .categoria(categoria)
                .apoiosCount(1)
                .build();
    }
}
