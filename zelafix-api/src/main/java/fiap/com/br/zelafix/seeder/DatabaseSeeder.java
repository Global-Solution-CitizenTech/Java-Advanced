package fiap.com.br.zelafix.seeder;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.categoria.CategoriaRepository;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.ChamadoRepository;
import fiap.com.br.zelafix.chamado.UrgenciaChamado;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoRepository;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import fiap.com.br.zelafix.endereco.bairro.BairroRepository;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import fiap.com.br.zelafix.endereco.cidade.CidadeRepository;
import fiap.com.br.zelafix.endereco.estado.Estado;
import fiap.com.br.zelafix.endereco.estado.EstadoRepository;
import fiap.com.br.zelafix.telefone.Telefone;
import fiap.com.br.zelafix.historico.HistStatus;
import fiap.com.br.zelafix.historico.HistStatusRepository;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.status.StatusRepository;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioRepository;
import fiap.com.br.zelafix.voto.VotoChamado;
import fiap.com.br.zelafix.voto.VotoChamadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final StatusRepository statusRepository;
    private final EstadoRepository estadoRepository;
    private final CidadeRepository cidadeRepository;
    private final BairroRepository bairroRepository;
    private final EnderecoRepository enderecoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;
    private final VotoChamadoRepository votoChamadoRepository;
    private final HistStatusRepository histStatusRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoriaRepository.count() > 0) {
            log.info("Banco de dados já populado. Pulando seed inicial.");
            return;
        }

        log.info("Populando dados iniciais do ecossistema ZelaFix (CitizenTech)...");

        // 1. Estados e Cidades
        Estado sp = estadoRepository.save(Estado.builder().nome("SP").build());
        Cidade saoPaulo = cidadeRepository.save(Cidade.builder().nome("São Paulo").estado(sp).build());

        Bairro belaVista = bairroRepository.save(Bairro.builder().nome("Bela Vista").cidade(saoPaulo).build());
        Bairro pinheiros = bairroRepository.save(Bairro.builder().nome("Pinheiros").cidade(saoPaulo).build());
        Bairro mooca = bairroRepository.save(Bairro.builder().nome("Mooca").cidade(saoPaulo).build());

        // 2. Categorias
        Categoria catBuraco = categoriaRepository.save(Categoria.builder()
                .nmCategoria("Buraco na Rua")
                .build());

        Categoria catLuz = categoriaRepository.save(Categoria.builder()
                .nmCategoria("Iluminação Pública")
                .build());

        categoriaRepository.save(Categoria.builder()
                .nmCategoria("Sinalização e Trânsito")
                .build());

        categoriaRepository.save(Categoria.builder()
                .nmCategoria("Vazamento de Água")
                .build());

        categoriaRepository.save(Categoria.builder()
                .nmCategoria("Outros")
                .build());

        // 3. Status
        Status statusAberto = statusRepository.save(Status.builder().nmStatus("Aberto").build());
        Status statusEmAndamento = statusRepository.save(Status.builder().nmStatus("Em Andamento").build());
        statusRepository.save(Status.builder().nmStatus("Rejeitado").build());

        // 4. Usuários Padrão (Cidadão e Gestor)
        Endereco endCidadao = enderecoRepository.save(Endereco.builder()
                .rua("Rua Treze de Maio")
                .numero("450")
                .cep("01327000")
                .bairro(belaVista)
                .build());

        Usuario cidadao = Usuario.builder()
                .nmUsuario("Carlos Munícipe")
                .email("cidadao@zelafix.com.br")
                .senha(passwordEncoder.encode("Senha@123"))
                .cargo(CargoUsuario.CIDADAO)
                .cpf("123.456.789-00")
                .avatarUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=400&q=80")
                .endereco(endCidadao)
                .telefones(new ArrayList<>())
                .dtCadastro(LocalDateTime.now().minusDays(30))
                .build();
        cidadao.getTelefones().add(Telefone.builder().numDdd("11").numTel("987654321").usuario(cidadao).build());
        usuarioRepository.save(cidadao);

        Endereco endGestor = enderecoRepository.save(Endereco.builder()
                .rua("Viaduto do Chá")
                .numero("15")
                .cep("01002020")
                .bairro(belaVista)
                .build());

        Usuario gestor = Usuario.builder()
                .nmUsuario("Mariana Gestora Pública")
                .email("gestor@zelafix.com.br")
                .senha(passwordEncoder.encode("Senha@123"))
                .cargo(CargoUsuario.GESTOR)
                .cpf("987.654.321-99")
                .avatarUrl("https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=400&q=80")
                .endereco(endGestor)
                .telefones(new ArrayList<>())
                .dtCadastro(LocalDateTime.now().minusDays(60))
                .build();
        gestor.getTelefones().add(Telefone.builder().numDdd("11").numTel("912345678").usuario(gestor).build());
        usuarioRepository.save(gestor);

        // 5. Chamados Iniciais de Demonstração
        Endereco endChamado1 = enderecoRepository.save(Endereco.builder()
                .rua("Rua dos Pinheiros")
                .numero("720")
                .cep("05422001")
                .bairro(pinheiros)
                .build());

        Chamado c1 = Chamado.builder()
                .protocolo("ZF-2026-00101")
                .titulo("Cratera profunda na faixa de ônibus")
                .descricao("Buraco extenso formado após as chuvas recentes na faixa exclusiva de ônibus, forçando desvio perigoso de ciclistas e pedestres.")
                .fotoUrl("https://images.unsplash.com/photo-1581375922343-ac6b40caf5e3?auto=format&fit=crop&w=800&q=80")
                .dtAbertura(LocalDateTime.now().minusDays(2))
                .prioridade(UrgenciaChamado.CRITICA)
                .parecerIa("Triagem Inteligente (Spring AI): Risco crítico de colisão e dano a coletivos na faixa de alta velocidade. Recomendada fresagem e recapeamento asfáltico emergencial.")
                .orgaoResponsavel("Secretaria Municipal de Infraestrutura Urbana e Obras")
                .usuario(cidadao)
                .endereco(endChamado1)
                .status(statusAberto)
                .categoria(catBuraco)
                .apoiosCount(28)
                .build();
        Chamado c1Salvo = chamadoRepository.save(c1);

        votoChamadoRepository.save(VotoChamado.builder().chamado(c1Salvo).usuario(cidadao).dtVoto(LocalDateTime.now().minusDays(2)).build());
        votoChamadoRepository.save(VotoChamado.builder().chamado(c1Salvo).usuario(gestor).dtVoto(LocalDateTime.now().minusDays(1)).build());

        histStatusRepository.save(HistStatus.builder()
                .chamado(c1Salvo)
                .usuario(cidadao)
                .status(statusAberto)
                .observacoes("Status alterado para Aberto: Chamado triado automaticamente pelo Spring AI.")
                .dtMudanca(LocalDateTime.now().minusDays(2))
                .build());

        Endereco endChamado2 = enderecoRepository.save(Endereco.builder()
                .rua("Rua da Mooca")
                .numero("1420")
                .cep("03104001")
                .bairro(mooca)
                .build());

        Chamado c2 = Chamado.builder()
                .protocolo("ZF-2026-00102")
                .titulo("Poste com luminária apagada há 4 noites")
                .descricao("Trecho da calçada em frente ao ponto de ônibus totalmente às escuras, gerando insegurança constante para os moradores.")
                .fotoUrl("https://images.unsplash.com/photo-1780415493361-07c5a78dbaef?auto=format&fit=crop&w=800&q=80")
                .dtAbertura(LocalDateTime.now().minusDays(4))
                .prioridade(UrgenciaChamado.ALTA)
                .parecerIa("Triagem Inteligente (Spring AI): Deficit luminoso em área de circulação noturna. Priorizado para substituição de relé fotocelular e lâmpada LED.")
                .orgaoResponsavel("Departamento de Iluminação Pública (Ilume)")
                .usuario(cidadao)
                .endereco(endChamado2)
                .status(statusEmAndamento)
                .categoria(catLuz)
                .apoiosCount(15)
                .build();
        Chamado c2Salvo = chamadoRepository.save(c2);

        histStatusRepository.save(HistStatus.builder()
                .chamado(c2Salvo)
                .usuario(gestor)
                .status(statusEmAndamento)
                .observacoes("Status alterado para Em Andamento: Ordem de serviço #49281 emitida para a equipe de campo do Ilume.")
                .dtMudanca(LocalDateTime.now().minusDays(1))
                .build());

        log.info("Base de dados do ZelaFix populada com sucesso! Usuários de teste prontos: cidadao@zelafix.com.br e gestor@zelafix.com.br (Senha@123)");
    }
}

