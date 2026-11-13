package fiap.com.br.zelafix.chamado.dto;

public record ChamadoTriagemPreviewResponse(
        Long categoriaId,
        String categoriaNome,
        String prioridade,
        String orgaoResponsavel,
        String parecerIa
) {}
