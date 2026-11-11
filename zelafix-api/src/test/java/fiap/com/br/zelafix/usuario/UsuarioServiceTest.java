package fiap.com.br.zelafix.usuario;

import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.endereco.estado.Estado;
import fiap.com.br.zelafix.exception.BusinessException;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.telefone.Telefone;
import fiap.com.br.zelafix.usuario.dto.UsuarioRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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
        Estado estado = Estado.builder().id(1L).nome("São Paulo").build();
        Cidade cidade = Cidade.builder().id(1L).nome("São Paulo").estado(estado).build();
        Bairro bairro = Bairro.builder().id(1L).nome("Sé").cidade(cidade).build();
        Endereco endereco = Endereco.builder()
                .id(1L)
                .cep("01001000")
                .numero("100")
                .rua("Praça da Sé")
                .referencia("Praça da Sé")
                .bairro(bairro)
                .build();

        Telefone telCidadao = Telefone.builder().idTelefone(1L).numDdd("11").numTel("987654321").build();
        Telefone telAdmin = Telefone.builder().idTelefone(2L).numDdd("11").numTel("999990000").build();

        usuarioCidadao = Usuario.builder()
                .idUsuario(1L)
                .nmUsuario("Carlos Munícipe")
                .email("cidadao@zelafix.com.br")
                .senha("encoded-pass")
                .cargo(CargoUsuario.CIDADAO)
                .endereco(endereco)
                .telefone(telCidadao)
                .build();

        usuarioAdmin = Usuario.builder()
                .idUsuario(2L)
                .nmUsuario("Administrador")
                .email("admin@zelafix.com.br")
                .senha("encoded-pass")
                .cargo(CargoUsuario.ADMIN)
                .endereco(endereco)
                .telefone(telAdmin)
                .build();

        EnderecoRequest endReq = new EnderecoRequest("01001-000", "100", "Praça da Sé", "Praça da Sé");
        request = new UsuarioRequest("Carlos Munícipe", "cidadao@zelafix.com.br", "Senha@123", "CIDADAO", "11", "987654321", endReq);
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
    @DisplayName("Deve listar todos os usuários paginados")
    void deveListarUsuariosPaginados() {
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(usuarioCidadao)));

        Page<Usuario> page = usuarioService.findAll(pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("cidadao@zelafix.com.br", page.getContent().getFirst().getEmail());
    }

    @Test
    @DisplayName("Deve buscar usuário por e-mail com sucesso")
    void deveBuscarUsuarioPorEmailComSucesso() {
        when(usuarioRepository.findByEmail("cidadao@zelafix.com.br")).thenReturn(Optional.of(usuarioCidadao));

        Usuario encontrado = usuarioService.findByEmail("cidadao@zelafix.com.br");

        assertEquals(1L, encontrado.getIdUsuario());
        assertEquals("Carlos Munícipe", encontrado.getNmUsuario());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando e-mail não existir")
    void deveLancarExcecaoQuandoEmailNaoExistir() {
        when(usuarioRepository.findByEmail("desconhecido@zelafix.com.br")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.findByEmail("desconhecido@zelafix.com.br"));
    }

    @Test
    @DisplayName("Deve atualizar dados de telefone existente do usuário")
    void deveAtualizarTelefoneExistente() {
        Telefone tel = Telefone.builder().numDdd("19").numTel("911112222").usuario(usuarioCidadao).build();
        usuarioCidadao.setTelefone(tel);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCidadao));
        when(passwordEncoder.encode(any())).thenReturn("new-encoded-pass");
        when(enderecoService.findOrCreateByCepAndNumero(any())).thenReturn(usuarioCidadao.getEndereco());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario atualizado = usuarioService.update(1L, request);

        assertNotNull(atualizado.getTelefone());
        assertEquals("11", atualizado.getTelefone().getNumDdd());
        assertEquals("987654321", atualizado.getTelefone().getNumTel());
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao atualizar e-mail para um já existente de outro usuário")
    void deveLancarExcecaoAoAtualizarParaEmailJaCadastrado() {
        UsuarioRequest reqNovoEmail = new UsuarioRequest("Carlos Alterado", "outro@zelafix.com.br", "Senha@123",
                "CIDADAO", "11", "987654321", request.endereco());

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCidadao));
        when(usuarioRepository.existsByEmail("outro@zelafix.com.br")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> usuarioService.update(1L, reqNovoEmail));
        assertEquals("E-mail já cadastrado: outro@zelafix.com.br", ex.getMessage());
    }

    @Test
    @DisplayName("Deve atualizar perfil pelo e-mail do usuário autenticado")
    void deveAtualizarUsuarioPorEmail() {
        when(usuarioRepository.findByEmail("cidadao@zelafix.com.br")).thenReturn(Optional.of(usuarioCidadao));
        when(passwordEncoder.encode(any())).thenReturn("new-encoded-pass");
        when(enderecoService.findOrCreateByCepAndNumero(any())).thenReturn(usuarioCidadao.getEndereco());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario atualizado = usuarioService.update("cidadao@zelafix.com.br", request);

        assertEquals("Carlos Munícipe", atualizado.getNmUsuario());
    }

    @Test
    @DisplayName("Deve impedir rebaixamento do único administrador do sistema")
    void deveImpedirRebaixamentoDoUnicoAdmin() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioAdmin));
        when(usuarioRepository.countByCargo(CargoUsuario.ADMIN)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> usuarioService.updateCargo(2L, "GESTOR"));
    }

    @Test
    @DisplayName("Deve permitir rebaixamento se houver mais de um administrador no sistema")
    void devePermitirRebaixamentoSeHouverOutrosAdmins() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioAdmin));
        when(usuarioRepository.countByCargo(CargoUsuario.ADMIN)).thenReturn(2L);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario atualizado = usuarioService.updateCargo(2L, "GESTOR");

        assertEquals(CargoUsuario.GESTOR, atualizado.getCargo());
    }

    @Test
    @DisplayName("Deve impedir exclusão do único administrador do sistema por ID")
    void deveImpedirExclusaoDoUnicoAdminPorId() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioAdmin));
        when(usuarioRepository.countByCargo(CargoUsuario.ADMIN)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> usuarioService.delete(2L));
    }

    @Test
    @DisplayName("Deve impedir exclusão do único administrador do sistema por e-mail")
    void deveImpedirExclusaoDoUnicoAdminPorEmail() {
        when(usuarioRepository.findByEmail("admin@zelafix.com.br")).thenReturn(Optional.of(usuarioAdmin));
        when(usuarioRepository.countByCargo(CargoUsuario.ADMIN)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> usuarioService.delete("admin@zelafix.com.br"));
    }

    @Test
    @DisplayName("Deve excluir usuário comum com sucesso")
    void deveExcluirUsuarioComumComSucesso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioCidadao));

        usuarioService.delete(1L);

        verify(usuarioRepository).delete(usuarioCidadao);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando usuário não for encontrado")
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.findById(99L));
    }
}
