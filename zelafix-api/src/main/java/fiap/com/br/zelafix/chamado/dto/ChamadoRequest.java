package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.categoria.CategoriaEnum;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.validation.EnumValidation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(
        @NotBlank(message = "O título do chamado é obrigatório.")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String titulo,

        @NotBlank(message = "A descrição detalhada é obrigatória.")
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String descricao,

        @NotBlank(message = "A categoria é obrigatória.")
        @EnumValidation(enumClass = CategoriaEnum.class, message = "Categoria inválida. Valores aceitos: ILUMINACAO_PUBLICA, VIAS_E_PAVIMENTACAO, DRENAGEM_E_ESGOTO, LIMPEZA_URBANA, ARVORES_E_PARQUES, SINALIZACAO_E_TRANSITO")
        String categoria,

        @NotNull(message = "O endereço da ocorrência é obrigatório.")
        @Valid
        EnderecoRequest endereco
) {
    public Chamado toEntity(Chamado base, Categoria categoria, Endereco endereco) {
        return Chamado.builder()
                .idChamado(base.getIdChamado())
                .titulo(titulo.trim())
                .descricao(descricao.trim())
                .foto(base.getFoto())
                .dtAbertura(base.getDtAbertura())
                .usuario(base.getUsuario())
                .endereco(endereco)
                .categoria(categoria)
                .prioridade(base.getPrioridade())
                .orgaoGoverno(base.getOrgaoGoverno())
                .build();
    }
}
