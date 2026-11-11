package fiap.com.br.zelafix.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.voto.VotoChamado;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VotoMessage(
        Long idChamado,
        Long idUsuario,
        String operacao
) {
    public VotoChamado toEntity(Chamado chamado, Usuario usuario) {
        return VotoChamado.builder()
                .chamado(chamado)
                .usuario(usuario)
                .dtVoto(LocalDateTime.now())
                .build();
    }
}
