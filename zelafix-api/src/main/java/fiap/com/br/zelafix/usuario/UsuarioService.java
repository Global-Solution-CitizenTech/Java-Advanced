package fiap.com.br.zelafix.usuario;

import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.exception.BusinessException;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.telefone.Telefone;
import fiap.com.br.zelafix.usuario.dto.UsuarioRequest;
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

    public Page<Usuario> findAll(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    public Usuario findById(Long id) {
        return findUsuarioById(id);
    }

    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com e-mail " + email + " não encontrado."));
    }

    @Transactional
    public Usuario create(UsuarioRequest req) {
        if (usuarioRepository.existsByEmail(req.email().trim().toLowerCase())) {
            throw new BusinessException("E-mail já cadastrado: " + req.email());
        }
        Endereco endereco = enderecoService.findOrCreateByCepAndNumero(req.endereco());
        Usuario usuario = req.toEntity(endereco, passwordEncoder.encode(req.senha()));
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario update(Long id, UsuarioRequest req) {
        Usuario usuario = findUsuarioById(id);
        aplicarEm(usuario, req);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario update(String email, UsuarioRequest req) {
        Usuario usuario = findByEmail(email);
        aplicarEm(usuario, req);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario updateCargo(Long id, String cargoStr) {
        Usuario usuario = findUsuarioById(id);
        CargoUsuario novoCargo = CargoUsuario.valueOf(cargoStr.trim().toUpperCase());

        if (usuario.getCargo() == CargoUsuario.ADMIN && novoCargo != CargoUsuario.ADMIN && usuarioRepository.countByCargo(CargoUsuario.ADMIN) <= 1) {
            throw new BusinessException("Não é possível rebaixar o único administrador do sistema.");
        }

        usuario.setCargo(novoCargo);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void delete(Long id) {
        deleteUsuario(findUsuarioById(id));
    }

    @Transactional
    public void delete(String email) {
        deleteUsuario(findByEmail(email));
    }

    private void deleteUsuario(Usuario usuario) {
        if (usuario.getCargo() == CargoUsuario.ADMIN && usuarioRepository.countByCargo(CargoUsuario.ADMIN) <= 1) {
            throw new BusinessException("Não é possível excluir o único administrador do sistema.");
        }
        usuarioRepository.delete(usuario);
    }

    private Usuario findUsuarioById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com id " + id + " não encontrado."));
    }

    private void aplicarEm(Usuario usuario, UsuarioRequest req) {
        usuario.setNmUsuario(req.nome().trim());
        usuario.setEmail(req.email().trim().toLowerCase());
        usuario.setSenha(passwordEncoder.encode(req.senha()));
        if (usuario.getTelefones() != null && !usuario.getTelefones().isEmpty()) {
            usuario.getTelefones().getFirst().setNumDdd(req.ddd().trim());
            usuario.getTelefones().getFirst().setNumTel(req.numeroTelefone().trim());
        } else {
            Telefone tel = Telefone.builder()
                    .numDdd(req.ddd().trim())
                    .numTel(req.numeroTelefone().trim())
                    .usuario(usuario)
                    .build();
            usuario.getTelefones().add(tel);
        }
        usuario.setEndereco(enderecoService.findOrCreateByCepAndNumero(req.endereco()));
    }
}
