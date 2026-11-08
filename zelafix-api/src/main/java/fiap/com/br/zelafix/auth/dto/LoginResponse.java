package fiap.com.br.zelafix.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fiap.com.br.zelafix.usuario.dto.UsuarioResponse;

public record LoginResponse(
        String token,
        UsuarioResponse usuario,
        @JsonProperty("user")
        UsuarioResponse user
) {
    public LoginResponse(String token, UsuarioResponse usuario) {
        this(token, usuario, usuario);
    }
}

