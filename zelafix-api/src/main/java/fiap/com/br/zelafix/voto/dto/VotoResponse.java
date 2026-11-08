package fiap.com.br.zelafix.voto.dto;

public record VotoResponse(
        Long chamadoId,
        Integer apoiosCount,
        boolean apoiadoPeloUsuario,
        String mensagem
) {}

