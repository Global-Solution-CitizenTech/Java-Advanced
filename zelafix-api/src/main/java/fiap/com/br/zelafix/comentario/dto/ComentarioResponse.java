package fiap.com.br.zelafix.comentario.dto;

import fiap.com.br.zelafix.comentario.Comentario;

public record ComentarioResponse(
        String id,
        String conteudo,
        String data,
        String autorId,
        String autorNome
) {
    public static ComentarioResponse fromEntity(Comentario c) {
        return new ComentarioResponse(
                String.valueOf(c.getIdComentario()),
                c.getConteudo(),
                c.getDtComentario().toString(),
                String.valueOf(c.getUsuario().getIdUsuario()),
                c.getUsuario().getNmUsuario()
        );
    }
}
