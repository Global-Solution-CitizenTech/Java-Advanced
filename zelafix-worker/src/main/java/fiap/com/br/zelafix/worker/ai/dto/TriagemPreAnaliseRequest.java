package fiap.com.br.zelafix.worker.ai.dto;

public record TriagemPreAnaliseRequest(
        String titulo,
        String descricao,
        String bairro,
        String fotoBase64
) {}
