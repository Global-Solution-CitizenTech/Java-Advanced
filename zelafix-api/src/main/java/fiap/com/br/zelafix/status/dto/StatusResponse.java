package fiap.com.br.zelafix.status.dto;

import fiap.com.br.zelafix.status.Status;

public record StatusResponse(
        Long id,
        String nome
) {
    public static StatusResponse fromEntity(Status s) {
        return new StatusResponse(s.getIdStatus(), s.getNmStatus());
    }
}
