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

        String fotoUrl,
        String fotoBase64,
        String prioridade
) {
    public ChamadoCreateRequest(String titulo, String descricao, String categoria, EnderecoRequest endereco, String fotoUrl, String prioridade) {
        this(titulo, descricao, categoria, endereco, fotoUrl, null, prioridade);
    }

    public Chamado toEntity(Usuario autor, Categoria categoria, Endereco endereco) {
        return toEntity(autor, categoria, endereco, null);
    }

    public Chamado toEntity(Usuario autor, Categoria categoria, Endereco endereco, byte[] fotoBytes) {
        byte[] bytes = (fotoBytes != null && fotoBytes.length > 0) ? fotoBytes : extrairFotoBytes();

        return Chamado.builder()
                .titulo(titulo.trim())
                .descricao(descricao.trim())
                .foto(bytes)
                .dtAbertura(LocalDateTime.now())
                .usuario(autor)
                .endereco(endereco)
                .categoria(categoria)
                .build();
    }

    private byte[] extrairFotoBytes() {
        if (fotoBase64 != null && !fotoBase64.isBlank()) {
            try {
                return java.util.Base64.getDecoder().decode(fotoBase64.replaceAll("^data:image/\\w+;base64,", ""));
            } catch (Exception ignored) {}
        }
        if (fotoUrl != null && fotoUrl.startsWith("data:image")) {
            try {
                return java.util.Base64.getDecoder().decode(fotoUrl.replaceAll("^data:image/\\w+;base64,", ""));
            } catch (Exception ignored) {}
        }
        if (fotoUrl != null && !fotoUrl.isBlank()) {
            return fotoUrl.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        // Placeholder binário de 1x1 GIF padrão para garantir NOT NULL
        return new byte[]{
                0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00,
                (byte) 0x80, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                0x00, 0x00, 0x00, 0x21, (byte) 0xf9, 0x04, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
                0x00, 0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3b
        };
    }
}
