package fiap.com.br.zelafix.seeder;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.categoria.CategoriaRepository;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.ChamadoRepository;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoRepository;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import fiap.com.br.zelafix.endereco.bairro.BairroRepository;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import fiap.com.br.zelafix.endereco.cidade.CidadeRepository;
import fiap.com.br.zelafix.endereco.estado.Estado;
import fiap.com.br.zelafix.endereco.estado.EstadoRepository;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.HistChamadoRepository;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.status.StatusRepository;
import fiap.com.br.zelafix.telefone.Telefone;
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

import java.nio.charset.StandardCharsets;
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
    private final HistChamadoRepository histChamadoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoriaRepository.count() > 0) {
            log.info("Banco de dados já populado. Pulando seed inicial.");
            return;
        }

        log.info("Populando dados iniciais do ecossistema ZelaFix (CitizenTech)...");

        // 1. Estados, Cidades e Bairros
        Estado sp = estadoRepository.save(Estado.builder().nome("São Paulo").build());
        Cidade saoPaulo = cidadeRepository.save(Cidade.builder().nome("São Paulo").estado(sp).build());

        Bairro belaVista = bairroRepository.save(Bairro.builder().nome("Bela Vista").cidade(saoPaulo).build());
        Bairro pinheiros = bairroRepository.save(Bairro.builder().nome("Pinheiros").cidade(saoPaulo).build());
        Bairro mooca = bairroRepository.save(Bairro.builder().nome("Mooca").cidade(saoPaulo).build());

        // 2. Categorias
        Categoria catBuraco = categoriaRepository.save(Categoria.builder()
                .nmCategoria("Vias e Pavimentação")
                .build());

        Categoria catLuz = categoriaRepository.save(Categoria.builder()
                .nmCategoria("Iluminação Pública")
                .build());

        categoriaRepository.save(Categoria.builder()
                .nmCategoria("Drenagem e Esgoto")
                .build());

        categoriaRepository.save(Categoria.builder()
                .nmCategoria("Limpeza Urbana")
                .build());

        categoriaRepository.save(Categoria.builder()
                .nmCategoria("Árvores e Parques")
                .build());

        categoriaRepository.save(Categoria.builder()
                .nmCategoria("Sinalização e Trânsito")
                .build());

        // 3. Status FSM 100% conforme DDL Oracle
        Status statusAberto = statusRepository.save(Status.builder().nmStatus("ABERTO").build());
        Status statusEmAnalise = statusRepository.save(Status.builder().nmStatus("EM_ANALISE").build());
        Status statusEmExecucao = statusRepository.save(Status.builder().nmStatus("EM_EXECUCAO").build());
        statusRepository.save(Status.builder().nmStatus("CONCLUIDO").build());
        statusRepository.save(Status.builder().nmStatus("REJEITADO").build());

        // 4. Usuários Padrão (Cidadão e Gestor)
        Endereco endCidadao = enderecoRepository.save(Endereco.builder()
                .rua("Rua Treze de Maio")
                .numero("450")
                .referencia("Próximo ao Teatro Sérgio Cardoso")
                .cep("01327000")
                .bairro(belaVista)
                .build());

        Usuario cidadao = Usuario.builder()
                .nmUsuario("Carlos Munícipe")
                .email("cidadao@zelafix.com.br")
                .senha(passwordEncoder.encode("Senha@123"))
                .cargo(CargoUsuario.CIDADAO)
                .endereco(endCidadao)
                .telefones(new ArrayList<>())
                .build();
        cidadao.getTelefones().add(Telefone.builder().numDdd("11").numTel("987654321").usuario(cidadao).build());
        usuarioRepository.save(cidadao);

        Endereco endGestor = enderecoRepository.save(Endereco.builder()
                .rua("Viaduto do Chá")
                .numero("15")
                .referencia("Prédio da Prefeitura")
                .cep("01002020")
                .bairro(belaVista)
                .build());

        Usuario gestor = Usuario.builder()
                .nmUsuario("Mariana Gestora Pública")
                .email("gestor@zelafix.com.br")
                .senha(passwordEncoder.encode("Senha@123"))
                .cargo(CargoUsuario.GESTOR)
                .endereco(endGestor)
                .telefones(new ArrayList<>())
                .build();
        gestor.getTelefones().add(Telefone.builder().numDdd("11").numTel("912345678").usuario(gestor).build());
        usuarioRepository.save(gestor);

        // 5. Chamados Iniciais de Demonstração (com imagem em byte[])
        byte[] fotoDemo = "ZelaFix-Imagem-Demonstracao".getBytes(StandardCharsets.UTF_8);

        Endereco endChamado1 = enderecoRepository.save(Endereco.builder()
                .rua("Rua dos Pinheiros")
                .numero("720")
                .referencia("Cruzamento com a Fradique Coutinho")
                .cep("05422001")
                .bairro(pinheiros)
                .build());

        Chamado c1 = Chamado.builder()
                .titulo("Cratera profunda na faixa de ônibus")
                .descricao("Buraco extenso formado após as chuvas recentes na faixa exclusiva de ônibus, forçando desvio perigoso de ciclistas e pedestres.")
                .foto(fotoDemo)
                .dtAbertura(LocalDateTime.now().minusDays(2))
                .usuario(cidadao)
                .endereco(endChamado1)
                .categoria(catBuraco)
                .build();
        Chamado c1Salvo = chamadoRepository.save(c1);

        votoChamadoRepository.save(VotoChamado.builder().chamado(c1Salvo).usuario(cidadao).dtVoto(LocalDateTime.now().minusDays(2)).build());
        votoChamadoRepository.save(VotoChamado.builder().chamado(c1Salvo).usuario(gestor).dtVoto(LocalDateTime.now().minusDays(1)).build());

        histChamadoRepository.save(HistChamado.builder()
                .chamado(c1Salvo)
                .usuario(cidadao)
                .status(statusAberto)
                .observacoes("[Triagem Spring AI - Órgão: Secretaria de Obras | Urgência: ALTA] Ocorrência registrada com sucesso.")
                .dtMudanca(LocalDateTime.now().minusDays(2))
                .build());

        Endereco endChamado2 = enderecoRepository.save(Endereco.builder()
                .rua("Rua da Mooca")
                .numero("1420")
                .referencia("Em frente ao ponto de ônibus da estação Mooca")
                .cep("03104001")
                .bairro(mooca)
                .build());

        Chamado c2 = Chamado.builder()
                .titulo("Poste com luminária apagada há 4 noites")
                .descricao("Trecho da calçada totalmente às escuras, gerando insegurança constante para os moradores.")
                .foto(fotoDemo)
                .dtAbertura(LocalDateTime.now().minusDays(4))
                .usuario(cidadao)
                .endereco(endChamado2)
                .categoria(catLuz)
                .build();
        Chamado c2Salvo = chamadoRepository.save(c2);

        histChamadoRepository.save(HistChamado.builder()
                .chamado(c2Salvo)
                .usuario(gestor)
                .status(statusEmExecucao)
                .observacoes("Status alterado para EM_EXECUCAO: Ordem de serviço #49281 emitida para equipe de campo do Ilume.")
                .dtMudanca(LocalDateTime.now().minusDays(1))
                .build());

        log.info("Base de dados do ZelaFix populada com sucesso! Usuários de teste prontos: cidadao@zelafix.com.br e gestor@zelafix.com.br (Senha@123)");
    }
}
