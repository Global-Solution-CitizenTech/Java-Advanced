package fiap.com.br.zelafix.comentario;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.ChamadoService;
import fiap.com.br.zelafix.comentario.dto.ComentarioRequest;
import fiap.com.br.zelafix.comentario.dto.ComentarioResponse;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final ChamadoService chamadoService;
    private final UsuarioService usuarioService;

    public List<ComentarioResponse> listarPorChamado(Long chamadoId) {
        chamadoService.buscarEntidadePorId(chamadoId);
        return comentarioRepository.findByChamadoIdChamadoOrderByDtComentarioDesc(chamadoId).stream()
                .map(ComentarioResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ComentarioResponse adicionar(Long chamadoId, ComentarioRequest request, String userEmail) {
        Chamado chamado = chamadoService.buscarEntidadePorId(chamadoId);
        Usuario autor = usuarioService.buscarPorEmail(userEmail);
        Comentario salvo = comentarioRepository.save(request.toEntity(chamado, autor));
        return ComentarioResponse.fromEntity(salvo);
    }
}
