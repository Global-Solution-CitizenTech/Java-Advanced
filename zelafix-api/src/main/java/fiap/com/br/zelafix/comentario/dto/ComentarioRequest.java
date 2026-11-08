package fiap.com.br.zelafix.comentario.dto;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.comentario.Comentario;
import fiap.com.br.zelafix.usuario.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ComentarioRequest(
        @NotBlank(message = "O comentário não pode ser vazio")
        @Size(max = 500, message = "O comentário pode ter no máximo 500 caracteres")
        String conteudo
) {
    public Comentario toEntity(Chamado chamado, Usuario autor) {
        return Comentario.builder()
                .conteudo(conteudo.trim())
                .dtComentario(LocalDateTime.now())
                .chamado(chamado)
                .usuario(autor)
                .build();
    }
}
