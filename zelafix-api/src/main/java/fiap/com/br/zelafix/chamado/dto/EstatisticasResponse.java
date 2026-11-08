package fiap.com.br.zelafix.chamado.dto;

public record EstatisticasResponse(
        long total,
        long abertos,
        long emAndamento,
        long resolvidos,
        long rejeitados,
        long totalApoios
) {}

