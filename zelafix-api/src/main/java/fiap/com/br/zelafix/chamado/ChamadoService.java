package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.categoria.CategoriaService;
import fiap.com.br.zelafix.chamado.dto.ChamadoRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.ChamadoStatusRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoTriagemPreviewResponse;
import fiap.com.br.zelafix.chamado.dto.ChamadoTriagemRequest;
import fiap.com.br.zelafix.chamado.dto.EstatisticasResponse;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.HistChamadoRepository;
import fiap.com.br.zelafix.queue.dto.ChamadoTriagemMessage;
import fiap.com.br.zelafix.queue.dto.TriagemFeedbackMessage;
import fiap.com.br.zelafix.queue.dto.VotoMessage;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.status.StatusChamadoEnum;
import fiap.com.br.zelafix.status.StatusService;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import fiap.com.br.zelafix.voto.VotoChamadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
    private final ApplicationEventPublisher eventPublisher;
    private final WorkerAiService workerAiService;

    // =========================================================================
    // 1. CONSULTAS E LISTAGENS
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> findAll(String status, String categoria, String busca, Boolean apenasMeus, String email, Pageable pageable) {
        Long currentUserId = usuarioService.findByEmail(email).getIdUsuario();
        Long autorId = Boolean.TRUE.equals(apenasMeus) ? currentUserId : null;

        Page<Chamado> chamados = chamadoRepository.findByFiltros(status, categoria, autorId, busca, pageable);
        return enriquecerPagina(chamados, currentUserId);
    }

    @Transactional(readOnly = true)
    public ChamadoResponse findDetailsById(Long id, String email) {
        Chamado chamado = findChamadoById(id);
        Long userId = usuarioService.findByEmail(email).getIdUsuario();
        List<HistChamado> historicos = histChamadoRepository.findByChamadoIdChamadoOrderByDtMudancaDescIdHistDesc(id);
        String status = historicos.isEmpty() ? StatusChamadoEnum.PENDENTE.name() : historicos.getFirst().getStatus().getNmStatus();
        return ChamadoResponse.fromEntity(
                chamado,
                status,
                votoChamadoRepository.countByChamadoIdChamado(id),
                votoChamadoRepository.existsByChamadoIdChamadoAndUsuarioIdUsuario(id, userId),
                historicos
        );
    }

    public byte[] getPhoto(Long id) {
        return findChamadoById(id).getFoto();
    }

    // =========================================================================
    // 2. CICLO DE VIDA DO CHAMADO (ABERTURA, EDIÇÃO, EXCLUSÃO)
    // =========================================================================

    @Transactional
    public ChamadoResponse create(ChamadoRequest request, byte[] fotoBytes, String email) {
        Usuario autor = usuarioService.findByEmail(email);
        Categoria categoria = categoriaService.findById(request.categoriaId());
        Endereco endereco = enderecoService.findOrCreateByCepAndNumero(request.endereco());
        Status statusInicial = statusService.findByNameOrCreate(StatusChamadoEnum.PENDENTE.name());

        Chamado salvo = chamadoRepository.save(request.toEntity(autor, categoria, endereco, fotoBytes));
        votoChamadoRepository.save(new VotoMessage(salvo.getIdChamado(), autor.getIdUsuario(), "VOTAR").toEntity(salvo, autor));
        HistChamado histInicial = new ChamadoStatusRequest(
                StatusChamadoEnum.PENDENTE.name(),
                "Chamado registrado no sistema com status PENDENTE."
        ).toEntity(salvo, autor, statusInicial);
        histChamadoRepository.save(histInicial);
        eventPublisher.publishEvent(ChamadoTriagemMessage.from(salvo));

        return ChamadoResponse.fromEntity(salvo, StatusChamadoEnum.PENDENTE.name(), 1L, true, List.of(histInicial));
    }

    @Transactional(readOnly = true)
    public ChamadoTriagemPreviewResponse preAnalisarIa(ChamadoTriagemRequest request) {
        ChamadoTriagemPreviewResponse ia = workerAiService.preAnalisar(request);
        Categoria categoria = categoriaService.findAll().stream()
                .filter(c -> c.getNmCategoria().equalsIgnoreCase(ia.categoriaNome()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + ia.categoriaNome()));
        return new ChamadoTriagemPreviewResponse(
                categoria.getIdCategoria(),
                categoria.getNmCategoria(),
                ia.prioridade(),
                ia.orgaoResponsavel(),
                ia.parecerIa()
        );
    }

    @Transactional
    public ChamadoResponse update(Long id, ChamadoRequest request, String email) {
        Chamado chamado = findChamadoById(id);
        Usuario usuario = usuarioService.findByEmail(email);
        validarPermissaoEdicao(chamado, usuario);

        aplicarEm(chamado, request);
        chamadoRepository.save(chamado);
        return findDetailsById(id, email);
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
    public ChamadoResponse updateStatus(Long id, ChamadoStatusRequest request, String email) {
        Chamado chamado = findChamadoById(id);
        Usuario operador = usuarioService.findByEmail(email);
        validarPermissaoGestao(operador);

        Status novoStatus = statusService.findByNameOrCreate(request.status());
        histChamadoRepository.save(request.toEntity(chamado, operador, novoStatus));

        return findDetailsById(id, email);
    }

    @Transactional
    public void aplicarTriagemIa(TriagemFeedbackMessage feedback) {
        histChamadoRepository
                .findFirstByChamadoIdChamadoOrderByDtMudancaDescIdHistDesc(feedback.idChamado())
                .ifPresent(base -> histChamadoRepository.save(feedback.toEntity(base)));
    }

    // =========================================================================
    // 4. APOIO POPULAR (VOTOS E MENSAGERIA)
    // =========================================================================

    @Transactional
    public ChamadoResponse apoiar(Long id, String email) {
        Chamado chamado = findChamadoById(id);
        Usuario usuario = usuarioService.findByEmail(email);

        alternarVoto(chamado, usuario);
        return findDetailsById(id, email);
    }

    // =========================================================================
    // 5. PAINEL DE ESTATÍSTICAS
    // =========================================================================

    @Transactional(readOnly = true)
    public EstatisticasResponse getStatistics() {
        return EstatisticasResponse.of(
                chamadoRepository.count(),
                chamadoRepository.countByStatusNmStatusIgnoreCase(StatusChamadoEnum.PENDENTE.name()) + chamadoRepository.countByStatusNmStatusIgnoreCase(StatusChamadoEnum.ABERTO.name()),
                chamadoRepository.countByStatusNmStatusIgnoreCase(StatusChamadoEnum.EM_ANALISE.name()) + chamadoRepository.countByStatusNmStatusIgnoreCase(StatusChamadoEnum.EM_EXECUCAO.name()),
                chamadoRepository.countByStatusNmStatusIgnoreCase(StatusChamadoEnum.CONCLUIDO.name()),
                chamadoRepository.countByStatusNmStatusIgnoreCase(StatusChamadoEnum.REJEITADO.name()),
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
        if (!chamado.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())
                && usuario.getCargo() != CargoUsuario.GESTOR
                && usuario.getCargo() != CargoUsuario.ADMIN) {
            throw new IllegalArgumentException("Você não tem permissão para editar este chamado.");
        }
    }

    private void validarPermissaoGestao(Usuario operador) {
        if (operador.getCargo() != CargoUsuario.GESTOR && operador.getCargo() != CargoUsuario.ADMIN) {
            throw new IllegalArgumentException("Apenas funcionários da prefeitura ou administradores têm permissão para atualizar o status do chamado.");
        }
    }

    private void alternarVoto(Chamado chamado, Usuario usuario) {
        Long chamadoId = chamado.getIdChamado();
        Long usuarioId = usuario.getIdUsuario();

        votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(chamadoId, usuarioId)
                .ifPresentOrElse(
                        votoExistente -> {
                            votoChamadoRepository.delete(votoExistente);
                            eventPublisher.publishEvent(new VotoMessage(chamadoId, usuarioId, "REMOVER"));
                        },
                        () -> {
                            VotoMessage voto = new VotoMessage(chamadoId, usuarioId, "VOTAR");
                            votoChamadoRepository.save(voto.toEntity(chamado, usuario));
                            eventPublisher.publishEvent(voto);
                        }
                );
    }

    private Page<ChamadoResponse> enriquecerPagina(Page<Chamado> chamados, Long currentUserId) {
        if (chamados.isEmpty()) {
            return Page.empty(chamados.getPageable());
        }

        List<Long> ids = chamados.map(Chamado::getIdChamado).toList();
        Map<Long, String> statusMap = histChamadoRepository.findLatestByChamadoIds(ids).stream()
                .collect(Collectors.toMap(
                        h -> h.getChamado().getIdChamado(),
                        h -> h.getStatus().getNmStatus(),
                        (existente, substituto) -> existente
                ));
        Map<Long, Long> votosCountMap = votoChamadoRepository.countVotosByChamadoIds(ids).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
        Set<Long> apoiadosSet = votoChamadoRepository.findChamadoIdsApoiadosPorMim(ids, currentUserId);

        return chamados.map(c -> ChamadoResponse.fromEntity(
                c,
                statusMap.getOrDefault(c.getIdChamado(), StatusChamadoEnum.PENDENTE.name()),
                votosCountMap.getOrDefault(c.getIdChamado(), 0L),
                apoiadosSet.contains(c.getIdChamado()),
                List.of()
        ));
    }

    private void aplicarEm(Chamado chamado, ChamadoRequest request) {
        chamado.setTitulo(request.titulo());
        chamado.setDescricao(request.descricao());
        chamado.setCategoria(categoriaService.findById(request.categoriaId()));
        chamado.setEndereco(enderecoService.findOrCreateByCepAndNumero(request.endereco()));
    }
}
