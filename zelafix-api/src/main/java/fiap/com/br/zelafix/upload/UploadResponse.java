package fiap.com.br.zelafix.upload;

public record UploadResponse(
        String url,
        String nomeArquivo,
        long tamanhoBytes,
        String tipoConteudo
) {}

