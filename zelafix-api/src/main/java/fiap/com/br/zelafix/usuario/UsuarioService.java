package fiap.com.br.zelafix.usuario;

import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.exception.BusinessException;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.usuario.dto.UsuarioRequest;
import fiap.com.br.zelafix.usuario.dto.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EnderecoService enderecoService;
    private final PasswordEncoder passwordEncoder;

    public Page<UsuarioResponse> listarTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponse::new);
    }

    public UsuarioResponse buscarPorId(Long id) {
        return new UsuarioResponse(findEntityById(id));
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + email));
    }

    @Transactional
    public UsuarioResponse cadastrar(UsuarioRequest req) {
        if (usuarioRepository.existsByEmail(req.email())) {
            throw new BusinessException("E-mail já cadastrado: " + req.email());
        }

        Endereco endereco = enderecoService.findOrCreate(req.endereco());
        Usuario usuario = Usuario.builder()
                .nmUsuario(req.nome())
                .email(req.email())
                .senha(passwordEncoder.encode(req.senha()))
                .cargo(req.cargo() != null ? req.cargo() : CargoUsuario.CIDADAO)
                .endereco(endereco)
                .build();

        return new UsuarioResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest req) {
        Usuario u = findEntityById(id);
        Endereco endereco = enderecoService.findOrCreate(req.endereco());

        u.setNmUsuario(req.nome());
        u.setEmail(req.email());
        if (req.senha() != null && !req.senha().isBlank()) {
            u.setSenha(passwordEncoder.encode(req.senha()));
        }
        if (req.cargo() != null) {
            u.setCargo(req.cargo());
        }
        u.setEndereco(endereco);

        return new UsuarioResponse(usuarioRepository.save(u));
    }

    @Transactional
    public void deletar(Long id) {
        usuarioRepository.delete(findEntityById(id));
    }

    private Usuario findEntityById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }
}
