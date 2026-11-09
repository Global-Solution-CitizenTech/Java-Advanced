package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.categoria.CategoriaService;
import fiap.com.br.zelafix.chamado.dto.ChamadoRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.EstatisticasResponse;
import fiap.com.br.zelafix.chamado.dto.StatusRequest;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.exception.BusinessException;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.HistChamadoRepository;
import fiap.com.br.zelafix.historico.dto.HistoricoResponse;
import fiap.com.br.zelafix.queue.dto.ChamadoTriagemMessage;
import fiap.com.br.zelafix.queue.dto.VotoMessage;
import fiap.com.br.zelafix.queue.producer.ChamadoEventProducer;
import fiap.com.br.zelafix.queue.producer.VotoEventProducer;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.status.StatusService;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import fiap.com.br.zelafix.voto.VotoChamado;
import fiap.com.br.zelafix.voto.VotoChamadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final StatusService statusService;
    private final EnderecoService enderecoService;
    private final HistChamadoRepository histChamadoRepository;
    private final VotoChamadoRepository votoChamadoRepository;
    private final ChamadoEventProducer chamadoEventProducer;
    private final VotoEventProducer votoEventProducer;

    public Page<ChamadoResponse> listar(
            String status,
            String categoria,
            String busca,
            Boolean apenasMeus,
            String userEmail,
            Pageable pageable
    ) {
        Long userId = null;
        if (userEmail != null) {
            userId = usuarioService.buscarPorEmail(userEmail).getIdUsuario();
        }
        Long autorId = Boolean.TRUE.equals(apenasMeus) ? userId : null;
        Long currentUserId = userId;

        return chamadoRepository.findByFiltros(status, categoria, autorId, busca, pageable)
                .map(c -> toResponse(c, currentUserId, false));
    }

    public ChamadoResponse buscarPorId(Long id, String userEmail) {
        Chamado chamado = findChamadoById(id);
        Long userId = userEmail != null ? usuarioService.buscarPorEmail(userEmail).getIdUsuario() : null;
        return toResponse(chamado, userId, true);
    }

    public Chamado buscarEntidadePorId(Long id) {
        return findChamadoById(id);
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest request, String userEmail) {
        return criarComFotoBytes(request, null, userEmail);
    }

    @Transactional
    public ChamadoResponse criarComFotoBytes(ChamadoRequest request, byte[] fotoBytes, String userEmail) {
        Usuario autor = usuarioService.buscarPorEmail(userEmail);
        Categoria categoria = categoriaService.buscarPorNomeOuCriar(request.categoria());
        Endereco endereco = enderecoService.findOrCreate(request.endereco());
        Status statusInicial = statusService.buscarPorNomeOuCriar("ABERTO");

        byte[] bytes = fotoBytes;
        if ((bytes == null || bytes.length == 0) && request.fotoBase64() != null && !request.fotoBase64().isBlank()) {
            bytes = Base64.getDecoder().decode(request.fotoBase64());
        }
        if (bytes == null || bytes.length == 0) {
            bytes = new byte[]{0};
        }

        Chamado entity = Chamado.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .foto(bytes)
                .dtAbertura(LocalDateTime.now())
                .usuario(autor)
                .endereco(endereco)
                .categoria(categoria)
                .build();

        Chamado salvo = chamadoRepository.save(entity);

        votoChamadoRepository.save(VotoChamado.builder()
                .chamado(salvo)
                .usuario(autor)
                .dtVoto(LocalDateTime.now())
                .build());

        histChamadoRepository.save(HistChamado.builder()
                .chamado(salvo)
                .usuario(autor)
                .status(statusInicial)
                .dtMudanca(LocalDateTime.now())
                .observacoes("Chamado registrado no sistema.")
                .build());

        chamadoEventProducer.publicarTriagem(new ChamadoTriagemMessage(
                salvo.getIdChamado(),
                "ZFX-" + salvo.getIdChamado(),
                salvo.getTitulo(),
                salvo.getDescricao(),
                categoria.getNmCategoria(),
                endereco.getBairro().getNome()
        ));

        return toResponse(salvo, autor.getIdUsuario(), true);
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest request, String userEmail) {
        Chamado chamado = findChamadoById(id);
        Usuario usuario = usuarioService.buscarPorEmail(userEmail);

        if (request.categoria() != null && !request.categoria().isBlank()) {
            chamado.setCategoria(categoriaService.buscarPorNomeOuCriar(request.categoria()));
        }
        if (request.endereco() != null) {
            chamado.setEndereco(enderecoService.findOrCreate(request.endereco()));
        }
        if (request.titulo() != null && !request.titulo().isBlank()) {
            chamado.setTitulo(request.titulo());
        }
        if (request.descricao() != null && !request.descricao().isBlank()) {
            chamado.setDescricao(request.descricao());
        }
        if (request.fotoBase64() != null && !request.fotoBase64().isBlank()) {
            chamado.setFoto(Base64.getDecoder().decode(request.fotoBase64()));
        }

        Chamado salvo = chamadoRepository.save(chamado);
        return toResponse(salvo, usuario.getIdUsuario(), true);
    }

    @Transactional
    public ChamadoResponse atualizarStatus(Long id, StatusRequest request, String userEmail) {
        Chamado chamado = findChamadoById(id);
        Usuario gestor = usuarioService.buscarPorEmail(userEmail);

        if (gestor.getCargo() != CargoUsuario.GESTOR) {
            throw new BusinessException("Apenas servidores de órgãos públicos (GESTOR) têm permissão para atualizar o status do chamado.");
        }

        Status novoStatus = statusService.buscarPorNomeOuCriar(request.status());
        String obs = (request.observacao() != null && !request.observacao().isBlank())
                ? request.observacao()
                : "Status alterado para " + novoStatus.getNmStatus();

        histChamadoRepository.save(HistChamado.builder()
                .chamado(chamado)
                .usuario(gestor)
                .status(novoStatus)
                .dtMudanca(LocalDateTime.now())
                .observacoes(obs)
                .build());

        return toResponse(chamado, gestor.getIdUsuario(), true);
    }

    @Transactional
    public ChamadoResponse apoiar(Long id, String userEmail) {
        Chamado chamado = findChamadoById(id);
        Usuario usuario = usuarioService.buscarPorEmail(userEmail);

        Optional<VotoChamado> votoExistente = votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(id, usuario.getIdUsuario());

        if (votoExistente.isPresent()) {
            votoChamadoRepository.delete(votoExistente.get());
            votoEventProducer.publicarVoto(new VotoMessage(id, usuario.getIdUsuario(), "REMOVER"));
        } else {
            votoChamadoRepository.save(VotoChamado.builder()
                    .chamado(chamado)
                    .usuario(usuario)
                    .dtVoto(LocalDateTime.now())
                    .build());
            votoEventProducer.publicarVoto(new VotoMessage(id, usuario.getIdUsuario(), "VOTAR"));
        }

        return toResponse(chamado, usuario.getIdUsuario(), true);
    }

    @Transactional
    public void deletar(Long id) {
        Chamado chamado = findChamadoById(id);
        chamadoRepository.delete(chamado);
    }

    public EstatisticasResponse obterEstatisticas() {
        long total = chamadoRepository.count();
        long abertos = chamadoRepository.countByStatusNmStatusIgnoreCase("ABERTO");
        long emAndamento = chamadoRepository.countByStatusNmStatusIgnoreCase("EM_ANALISE")
                + chamadoRepository.countByStatusNmStatusIgnoreCase("EM_EXECUCAO");
        long resolvidos = chamadoRepository.countByStatusNmStatusIgnoreCase("CONCLUIDO");
        long rejeitados = chamadoRepository.countByStatusNmStatusIgnoreCase("REJEITADO");
        long totalApoios = votoChamadoRepository.count();

        return new EstatisticasResponse(total, abertos, emAndamento, resolvidos, rejeitados, totalApoios);
    }

    private Chamado findChamadoById(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado com id " + id + " não encontrado."));
    }

    private ChamadoResponse toResponse(Chamado c, Long userId, boolean incluirHistorico) {
        String status = histChamadoRepository.findFirstByChamadoIdChamadoOrderByDtMudancaDesc(c.getIdChamado())
                .map(h -> h.getStatus().getNmStatus())
                .orElse("ABERTO");
        long apoios = votoChamadoRepository.countByChamadoIdChamado(c.getIdChamado());
        boolean apoiado = userId != null && votoChamadoRepository.existsByChamadoIdChamadoAndUsuarioIdUsuario(c.getIdChamado(), userId);
        List<HistoricoResponse> historico = incluirHistorico
                ? histChamadoRepository.findByChamadoIdChamadoOrderByDtMudancaDesc(c.getIdChamado()).stream().map(HistoricoResponse::new).toList()
                : List.of();

        return new ChamadoResponse(c, status, apoios, apoiado, historico);
    }
}
