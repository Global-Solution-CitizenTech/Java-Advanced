package fiap.com.br.zelafix.usuario;

import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.exception.BusinessException;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.usuario.dto.UsuarioRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EnderecoService enderecoService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioCidadao;
    private Usuario usuarioAdmin;
    private UsuarioRequest request;

    @BeforeEach
    void setUp() {
        Endereco endereco = Endereco.builder().id(1L).cep("01001000").numero("100").build();

        usuarioCidadao = Usuario.builder()
                .idUsuario(1L)
                .nmUsuario("Carlos Munícipe")
                .email("cidadao@zelafix.com.br")
                .senha("encoded-pass")
                .cargo(CargoUsuario.CIDADAO)
                .endereco(endereco)
                .build();

        usuarioAdmin = Usuario.builder()
                .idUsuario(2L)
                .nmUsuario("Administrador")
                .email("admin@zelafix.com.br")
                .senha("encoded-pass")
                .cargo(CargoUsuario.ADMIN)
                .endereco(endereco)
                .build();

        EnderecoRequest endReq = new EnderecoRequest("01001-000", "100", "Praça da Sé", "Praça da Sé");
        request = new UsuarioRequest("Carlos Munícipe", "cidadao@zelafix.com.br", "Senha@123", CargoUsuario.CIDADAO, "11", "987654321", endReq);
    }

    @Test
    @DisplayName("Deve cadastrar novo usuário com sucesso e codificar a senha")
    void deveCadastrarUsuarioComSucesso() {
        when(usuarioRepository.existsByEmail("cidadao@zelafix.com.br")).thenReturn(false);
        when(enderecoService.findOrCreateByCepAndNumero(any())).thenReturn(usuarioCidadao.getEndereco());
        when(passwordEncoder.encode("Senha@123")).thenReturn("encoded-pass");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioCidadao);

        Usuario criado = usuarioService.create(request);

        assertNotNull(criado);
        assertEquals("Carlos Munícipe", criado.getNmUsuario());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao tentar cadastrar usuário com e-mail duplicado")
    void deveLancarExcecaoParaEmailDuplicado() {
        when(usuarioRepository.existsByEmail("cidadao@zelafix.com.br")).thenReturn(true);

        assertThrows(BusinessException.class, () -> usuarioService.create(request));
    }

    @Test
    @DisplayName("Deve impedir rebaixamento do único administrador do sistema")
    void deveImpedirRebaixamentoDoUnicoAdmin() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioAdmin));
        when(usuarioRepository.countByCargo(CargoUsuario.ADMIN)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> usuarioService.updateCargo(2L, "GESTOR"));
    }

    @Test
    @DisplayName("Deve impedir exclusão do único administrador do sistema")
    void deveImpedirExclusaoDoUnicoAdmin() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioAdmin));
        when(usuarioRepository.countByCargo(CargoUsuario.ADMIN)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> usuarioService.delete(2L));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando usuário não for encontrado")
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.findById(99L));
    }
}
