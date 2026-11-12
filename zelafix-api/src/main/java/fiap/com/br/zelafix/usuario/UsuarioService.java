package fiap.com.br.zelafix.usuario;

import fiap.com.br.zelafix.chamado.ChamadoRepository;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.historico.HistChamadoRepository;
import fiap.com.br.zelafix.usuario.dto.UsuarioRequest;
import fiap.com.br.zelafix.voto.VotoChamadoRepository;
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
    private final ChamadoRepository chamadoRepository;
    private final HistChamadoRepository histChamadoRepository;
    private final VotoChamadoRepository votoChamadoRepository;
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
        Endereco endereco = enderecoService.findOrCreateByCepAndNumero(req.endereco());
        return usuarioRepository.save(req.toEntity(endereco, passwordEncoder.encode(req.senha())));
    }

    @Transactional
    public Usuario update(Long id, UsuarioRequest req) {
        return aplicarEm(findUsuarioById(id), req);
    }

    @Transactional
    public Usuario update(String email, UsuarioRequest req) {
        return aplicarEm(findByEmail(email), req);
    }

    @Transactional
    public Usuario updateCargo(Long id, String cargoStr) {
        Usuario usuario = findUsuarioById(id);
        CargoUsuario novoCargo = CargoUsuario.valueOf(cargoStr.trim().toUpperCase());
        validarMinimoCargo(usuario, novoCargo, "rebaixar");
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
        validarMinimoCargo(usuario, null, "excluir");
        Long id = usuario.getIdUsuario();
        if (chamadoRepository.existsByUsuarioIdUsuario(id) || histChamadoRepository.existsByUsuarioIdUsuario(id)) {
            throw new IllegalArgumentException("Não é possível excluir usuário que possui chamados ou histórico de auditoria vinculados.");
        }
        votoChamadoRepository.deleteByUsuarioIdUsuario(id);
        usuarioRepository.delete(usuario);
    }

    private void validarMinimoCargo(Usuario usuario, CargoUsuario novoCargo, String acao) {
        CargoUsuario atual = usuario.getCargo();
        if ((atual == CargoUsuario.ADMIN || atual == CargoUsuario.GESTOR)
                && novoCargo != atual
                && usuarioRepository.countByCargo(atual) <= 1) {
            throw new IllegalArgumentException("Não é possível " + acao + " o único " + atual.name() + " do sistema.");
        }
    }

    private Usuario findUsuarioById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com id " + id + " não encontrado."));
    }

    private Usuario aplicarEm(Usuario usuario, UsuarioRequest req) {
        usuario.setNmUsuario(req.nome().trim());
        usuario.setEmail(req.email().trim().toLowerCase());
        usuario.setSenha(passwordEncoder.encode(req.senha()));
        usuario.setEndereco(enderecoService.findOrCreateByCepAndNumero(req.endereco()));

        if (usuario.getTelefone() == null) {
            usuario.setTelefone(req.toTelefone(usuario));
        } else {
            usuario.getTelefone().setNumDdd(req.ddd().trim());
            usuario.getTelefone().setNumTel(req.numeroTelefone().trim());
        }
        return usuarioRepository.save(usuario);
    }
}
