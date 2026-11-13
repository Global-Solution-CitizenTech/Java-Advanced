package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.chamado.orgao.OrgaoGoverno;
import fiap.com.br.zelafix.chamado.prioridade.Prioridade;
import fiap.com.br.zelafix.usuario.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ChamadoCreateRequest(
        @NotNull(message = "O endereço da ocorrência é obrigatório.")
        @Valid
        EnderecoRequest endereco,

        @NotBlank(message = "A foto da ocorrência é obrigatória.")
        String fotoBase64
) {
    public static Chamado toEntity(Usuario autor, Categoria categoria, Prioridade prioridade, OrgaoGoverno orgao, Endereco endereco, byte[] foto) {
        return Chamado.builder()
                .titulo("Ocorrência em análise pela IA")
                .descricao("Aguardando análise técnica automática da imagem enviada...")
                .foto(foto)
                .dtAbertura(LocalDateTime.now())
                .usuario(autor)
                .endereco(endereco)
                .categoria(categoria)
                .prioridade(prioridade)
                .orgaoGoverno(orgao)
                .build();
    }
}
