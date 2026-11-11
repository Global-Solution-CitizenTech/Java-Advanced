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
import fiap.com.br.zelafix.queue.dto.TriagemFeedbackMessage;
import fiap.com.br.zelafix.queue.dto.VotoMessage;
import fiap.com.br.zelafix.queue.producer.ChamadoEventProducer;
import fiap.com.br.zelafix.queue.producer.VotoEventProducer;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.status.StatusChamadoEnum;
import fiap.com.br.zelafix.status.StatusService;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import fiap.com.br.zelafix.voto.VotoChamado;
import fiap.com.br.zelafix.voto.VotoChamadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    // =========================================================================
    // 1. CONSULTAS E LISTAGENS
    // =========================================================================

    public Page<ChamadoResponse> findAll(String status, String categoria, String busca, Boolean apenasMeus, String email, Pageable pageable) {
        Long currentUserId = usuarioService.findByEmail(email).getIdUsuario();
        Long autorId = Boolean.TRUE.equals(apenasMeus) ? currentUserId : null;

        Page<Chamado> chamados = chamadoRepository.findByFiltros(status, categoria, autorId, busca, pageable);
        return enriquecerPagina(chamados, currentUserId);
    }

    public Chamado findById(Long id) {
        return findChamadoById(id);
    }

    public ChamadoResponse findDetailsById(Long id, String email) {
        Long userId = usuarioService.findByEmail(email).getIdUsuario();
        return toResponse(findChamadoById(id), userId, true);
    }

    public byte[] getPhoto(Long id) {
        return findChamadoById(id).getFoto();
    }

    // =========================================================================
    // 2. CICLO DE VIDA DO CHAMADO (ABERTURA, EDIÇÃO, EXCLUSÃO)
    // =========================================================================

    @Transactional
    public ChamadoResponse create(ChamadoRequest request, String email) {
        return create(request, null, email);
    }

    @Transactional
    public ChamadoResponse create(ChamadoRequest request, byte[] fotoBytes, String email) {
        Usuario autor = usuarioService.findByEmail(email);
        Categoria categoria = categoriaService.findById(request.categoriaId());
        Endereco endereco = enderecoService.findOrCreateByCepAndNumero(request.endereco());
        Status statusInicial = statusService.findByNameOrCreate(StatusChamadoEnum.PENDENTE.name());

        Chamado salvo = chamadoRepository.save(request.toEntity(autor, categoria, endereco, fotoBytes));
        votoChamadoRepository.save(criarVoto(salvo, autor));
        histChamadoRepository.save(criarHistoricoInicial(salvo, autor, statusInicial));
        chamadoEventProducer.publicarTriagem(ChamadoTriagemMessage.from(salvo));

        return toResponse(salvo, autor.getIdUsuario(), true);
    }

    @Transactional
    public ChamadoResponse update(Long id, ChamadoRequest request, String email) {
        Chamado chamado = findChamadoById(id);
        Usuario usuario = usuarioService.findByEmail(email);
        validarPermissaoEdicao(chamado, usuario);

        aplicarEm(chamado, request);
        return toResponse(chamadoRepository.save(chamado), usuario.getIdUsuario(), true);
    }

    @Transactional
    public void delete(Long id) {
        Chamado chamado = findChamadoById(id);
        votoChamadoRepository.deleteByChamadoIdChamado(id);
        histChamadoRepository.deleteByChamadoIdChamado(id);
        chamadoRepository.delete(chamado);
    }

    // =========================================================================
    // 3. GESTÃO DE STATUS E AUDITORIA
    // =========================================================================

    @Transactional
    public ChamadoResponse updateStatus(Long id, StatusRequest request, String email) {
        Chamado chamado = findChamadoById(id);
        Usuario operador = usuarioService.findByEmail(email);
        validarPermissaoGestao(operador);

        Status novoStatus = statusService.findByNameOrCreate(request.status());
        histChamadoRepository.save(request.toEntity(chamado, operador, novoStatus));

        return toResponse(chamado, operador.getIdUsuario(), true);
    }

    @Transactional
    public void aplicarTriagemIa(TriagemFeedbackMessage feedback) {
        histChamadoRepository
                .findFirstByChamadoIdChamadoOrderByDtMudancaDescIdHistDesc(feedback.idChamado())
                .ifPresent(base -> histChamadoRepository.save(feedback.toEntity(base)));
    }

    @Transactional
    public void aplicarTriagemIa(Long idChamado, String prioridade, String orgao, String parecer) {
        aplicarTriagemIa(new TriagemFeedbackMessage(idChamado, prioridade, orgao, parecer));
    }

    // =========================================================================
    // 4. APOIO POPULAR (VOTOS E MENSERAGIA)
    // =========================================================================

    @Transactional
    public ChamadoResponse apoiar(Long id, String email) {
        Chamado chamado = findChamadoById(id);
        Usuario usuario = usuarioService.findByEmail(email);

        alternarVoto(chamado, usuario);
        return toResponse(chamado, usuario.getIdUsuario(), true);
    }

    // =========================================================================
    // 5. PAINEL DE ESTATÍSTICAS
    // =========================================================================

    public EstatisticasResponse getStatistics() {
        return new EstatisticasResponse(
                chamadoRepository.count(),
                chamadoRepository.countByStatusNmStatusIgnoreCase("PENDENTE") + chamadoRepository.countByStatusNmStatusIgnoreCase("ABERTO"),
                chamadoRepository.countByStatusNmStatusIgnoreCase("EM_ANALISE") + chamadoRepository.countByStatusNmStatusIgnoreCase("EM_EXECUCAO"),
                chamadoRepository.countByStatusNmStatusIgnoreCase("CONCLUIDO"),
                chamadoRepository.countByStatusNmStatusIgnoreCase("REJEITADO"),
                votoChamadoRepository.count()
        );
    }

    // =========================================================================
    // 6. HELPERS PRIVADOS (BUSCAS, ENRIQUECIMENTO E VALIDAÇÕES)
    // =========================================================================

    private Chamado findChamadoById(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado com id " + id + " não encontrado."));
    }

    private void validarPermissaoEdicao(Chamado chamado, Usuario usuario) {
        if (!chamado.getUsuario().getIdUsuario().equals(usuario.getIdUsuario()) && usuario.getCargo() != CargoUsuario.ADMIN) {
            throw new BusinessException("Você não tem permissão para editar este chamado.");
        }
    }

    private void validarPermissaoGestao(Usuario operador) {
        if (operador.getCargo() != CargoUsuario.GESTOR && operador.getCargo() != CargoUsuario.ADMIN) {
            throw new BusinessException("Apenas funcionários da prefeitura ou administradores têm permissão para atualizar o status do chamado.");
        }
    }

    private void alternarVoto(Chamado chamado, Usuario usuario) {
        Long chamadoId = chamado.getIdChamado();
        Long usuarioId = usuario.getIdUsuario();
        Optional<VotoChamado> votoExistente = votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(chamadoId, usuarioId);

        if (votoExistente.isPresent()) {
            votoChamadoRepository.delete(votoExistente.get());
            votoEventProducer.publicarVoto(new VotoMessage(chamadoId, usuarioId, "REMOVER"));
        } else {
            votoChamadoRepository.save(criarVoto(chamado, usuario));
            votoEventProducer.publicarVoto(new VotoMessage(chamadoId, usuarioId, "VOTAR"));
        }
    }

    private Page<ChamadoResponse> enriquecerPagina(Page<Chamado> chamados, Long currentUserId) {
        if (chamados.isEmpty()) {
            return chamados.map(c -> toResponse(c, currentUserId, false));
        }

        List<Long> ids = chamados.map(Chamado::getIdChamado).toList();
        Map<Long, String> statusMap = mapearStatusPorChamado(ids);
        Map<Long, Long> votosCountMap = mapearVotosPorChamado(ids);
        Set<Long> apoiadosSet = votoChamadoRepository.findChamadoIdsApoiadosPorMim(ids, currentUserId);

        return chamados.map(c -> ChamadoResponse.fromEntity(
                c,
                statusMap.getOrDefault(c.getIdChamado(), StatusChamadoEnum.PENDENTE.name()),
                votosCountMap.getOrDefault(c.getIdChamado(), 0L),
                apoiadosSet.contains(c.getIdChamado()),
                List.of()
        ));
    }

    private Map<Long, String> mapearStatusPorChamado(List<Long> ids) {
        return histChamadoRepository.findLatestByChamadoIds(ids).stream()
                .collect(Collectors.toMap(
                        h -> h.getChamado().getIdChamado(),
                        h -> h.getStatus().getNmStatus(),
                        (existente, substituto) -> existente
                ));
    }

    private Map<Long, Long> mapearVotosPorChamado(List<Long> ids) {
        return votoChamadoRepository.countVotosByChamadoIds(ids).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private void aplicarEm(Chamado chamado, ChamadoRequest request) {
        chamado.setTitulo(request.titulo());
        chamado.setDescricao(request.descricao());
        chamado.setCategoria(categoriaService.findById(request.categoriaId()));
        chamado.setEndereco(enderecoService.findOrCreateByCepAndNumero(request.endereco()));
    }

    private VotoChamado criarVoto(Chamado chamado, Usuario usuario) {
        return VotoChamado.builder()
                .chamado(chamado)
                .usuario(usuario)
                .dtVoto(LocalDateTime.now())
                .build();
    }

    private HistChamado criarHistoricoInicial(Chamado chamado, Usuario usuario, Status status) {
        return HistChamado.builder()
                .chamado(chamado)
                .usuario(usuario)
                .status(status)
                .dtMudanca(LocalDateTime.now())
                .observacoes("Chamado registrado no sistema com status PENDENTE.")
                .build();
    }

    private ChamadoResponse toResponse(Chamado c, Long userId, boolean incluirHistorico) {
        String status = histChamadoRepository.findFirstByChamadoIdChamadoOrderByDtMudancaDescIdHistDesc(c.getIdChamado())
                .map(h -> h.getStatus().getNmStatus())
                .orElse(StatusChamadoEnum.PENDENTE.name());
        long apoios = votoChamadoRepository.countByChamadoIdChamado(c.getIdChamado());
        boolean apoiado = votoChamadoRepository.existsByChamadoIdChamadoAndUsuarioIdUsuario(c.getIdChamado(), userId);

        List<HistoricoResponse> historico = incluirHistorico
                ? histChamadoRepository.findByChamadoIdChamadoOrderByDtMudancaDescIdHistDesc(c.getIdChamado()).stream().map(HistoricoResponse::new).toList()
                : List.of();

        return ChamadoResponse.fromEntity(c, status, apoios, apoiado, historico);
    }
}
