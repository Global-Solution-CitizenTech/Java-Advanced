package fiap.com.br.zelafix.chamado.dto;

public record ChamadoFiltroRequest(
        String status,
        String categoria,
        String busca,
        Boolean apenasMeus,
        String cidade,
        String bairro,
        String estado
) {
    public static ChamadoFiltroRequest vazio() {
        return new ChamadoFiltroRequest(null, null, null, null, null, null, null);
    }
}
