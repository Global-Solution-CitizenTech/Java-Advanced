package fiap.com.br.zelafix.usuario;

import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.exception.BusinessException;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.telefone.Telefone;
import fiap.com.br.zelafix.telefone.TelefoneRepository;
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
    private final TelefoneRepository telefoneRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UsuarioResponse> listarTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(UsuarioResponse::fromEntity);
    }

    public Usuario buscarPorId(Long id) {
        return findUsuarioById(id);
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com e-mail " + email + " não encontrado."));
    }

    @Transactional
    public UsuarioResponse cadastrar(UsuarioRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new BusinessException("Já existe um usuário cadastrado com o e-mail: " + request.email());
        }

        Endereco endereco = enderecoService.findOrCreateByCepAndNumero(request.endereco());
        Usuario salvo = usuarioRepository.save(request.toEntity(endereco, passwordEncoder.encode(request.senha())));
        vincularTelefoneSeInformado(salvo, request);

        return UsuarioResponse.fromEntity(salvo);
    }

    @Transactional
    public UsuarioResponse atualizarPerfil(Long id, UsuarioRequest request) {
        Usuario usuario = findUsuarioById(id);
        Endereco endereco = enderecoService.findOrCreateByCepAndNumero(request.endereco());
        request.aplicarEm(usuario, endereco, passwordEncoder);
        vincularTelefoneSeInformado(usuario, request);
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    @Transactional
    public void deletar(Long id) {
        Usuario usuario = findUsuarioById(id);
        usuarioRepository.delete(usuario);
    }

    private Usuario findUsuarioById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com id " + id + " não encontrado."));
    }

    private void vincularTelefoneSeInformado(Usuario usuario, UsuarioRequest request) {
        Telefone telefone = request.toTelefone(usuario);
        if (telefone != null) {
            telefoneRepository.save(telefone);
            usuario.getTelefones().clear();
            usuario.getTelefones().add(telefone);
        }
    }
}
