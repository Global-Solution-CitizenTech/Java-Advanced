package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.usuario.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Base64;

public record ChamadoRequest(
        @NotBlank(message = "O título do chamado é obrigatório.")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String titulo,

        @NotBlank(message = "A descrição detalhada é obrigatória.")
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String descricao,

        @NotNull(message = "O ID da categoria é obrigatório.")
        Long categoriaId,

        @NotNull(message = "O endereço da ocorrência é obrigatório.")
        @Valid
        EnderecoRequest endereco,

        String fotoBase64
) {
    public Chamado toEntity(Usuario autor, Categoria categoria, Endereco endereco, byte[] fotoBytes) {
        return Chamado.builder()
                .titulo(titulo)
                .descricao(descricao)
                .foto(resolverFoto(fotoBytes))
                .dtAbertura(LocalDateTime.now())
                .usuario(autor)
                .endereco(endereco)
                .categoria(categoria)
                .build();
    }

    private byte[] resolverFoto(byte[] fotoBytes) {
        if (fotoBytes != null && fotoBytes.length > 0) return fotoBytes;
        if (fotoBase64 != null && !fotoBase64.isBlank()) return Base64.getDecoder().decode(fotoBase64.trim());
        return new byte[]{0};
    }
}
