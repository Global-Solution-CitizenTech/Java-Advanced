package fiap.com.br.zelafix.auth.dto;

import fiap.com.br.zelafix.usuario.dto.UsuarioResponse;

public record LoginResponse(
        String token,
        UsuarioResponse user
) {}
