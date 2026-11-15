package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.categoria.CategoriaEnum;
import fiap.com.br.zelafix.categoria.CategoriaService;
import fiap.com.br.zelafix.chamado.dto.ChamadoFiltroRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.ChamadoStatusRequest;
import fiap.com.br.zelafix.chamado.dto.EstatisticasResponse;
import fiap.com.br.zelafix.chamado.orgao.OrgaoGovernoService;
import fiap.com.br.zelafix.chamado.prioridade.PrioridadeService;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.HistChamadoRepository;
import fiap.com.br.zelafix.historico.status.Status;
import fiap.com.br.zelafix.historico.status.StatusChamadoEnum;
import fiap.com.br.zelafix.historico.status.StatusService;
import fiap.com.br.zelafix.queue.dto.ChamadoTriagemMessage;
import fiap.com.br.zelafix.queue.dto.TriagemFeedbackMessage;
import fiap.com.br.zelafix.queue.dto.VotoMessage;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import fiap.com.br.zelafix.util.ImageMimeTypeDetector;
import fiap.com.br.zelafix.voto.VotoChamadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.DeferredResult;

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
    private final PrioridadeService prioridadeService;
    private final OrgaoGovernoService orgaoGovernoService;
    private final StatusService statusService;
    private final EnderecoService enderecoService;
    private final HistChamadoRepository histChamadoRepository;
    private final VotoChamadoRepository votoChamadoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TriagemPendenteStore triagemPendenteStore;

    // =========================================================================
    // 1. CONSULTAS E LISTAGENS
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> findAll(ChamadoFiltroRequest filtro, String email, Pageable pageable) {
        Long currentUserId = usuarioService.findByEmail(email).getIdUsuario();
        Long autorId = Boolean.TRUE.equals(filtro.apenasMeus()) ? currentUserId : null;

        Page<Chamado> chamados = chamadoRepository.findByFiltros(
                filtro.status(), filtro.categoria(), autorId, filtro.cidade(), filtro.bairro(), filtro.estado(), filtro.busca(), pageable
        );
        return enriquecerPagina(chamados, currentUserId);
    }

    @Transactional(readOnly = true)
    public ChamadoResponse findDetailsById(Long id, String email) {
        Chamado chamado = findChamadoById(id);
        Long userId = usuarioService.findByEmail(email).getIdUsuario();
        List<HistChamado> historicos = histChamadoRepository.findByChamadoIdChamadoOrderByDtMudancaDescIdHistDesc(id);
        String status = historicos.isEmpty() ? StatusChamadoEnum.PENDENTE.name() : historicos.getFirst().getStatus().getNmStatus().name();
        return ChamadoResponse.fromEntity(
                chamado,
                status,
                votoChamadoRepository.countByChamadoIdChamado(id),
                votoChamadoRepository.existsByChamadoIdChamadoAndUsuarioIdUsuario(id, userId),
                historicos
        );
    }

    @Transactional(readOnly = true)
    public byte[] getPhoto(Long id) {
        byte[] foto = findChamadoById(id).getFoto();
        if (foto == null || foto.length == 0) {
            throw new ResourceNotFoundException("Foto do chamado com id " + id + " não encontrada.");
        }
        return foto;
    }

    // =========================================================================
    // 2. CICLO DE VIDA DO CHAMADO (ABERTURA, EDIÇÃO, EXCLUSÃO)
    // =========================================================================

    public DeferredResult<ChamadoResponse> create(EnderecoRequest enderecoRequest, byte[] fotoBytes, String email) {
        if (!ImageMimeTypeDetector.isImagemSuportada(fotoBytes)) {
            throw new IllegalArgumentException("Arquivo enviado não é uma imagem válida. Formatos suportados: JPEG, PNG, WEBP e GIF.");
        }
        Usuario autor = usuarioService.findByEmail(email);
        Endereco endereco = enderecoService.findOrCreateByCepAndNumero(enderecoRequest);

        return triagemPendenteStore.registrar(correlationId ->
                eventPublisher.publishEvent(ChamadoTriagemMessage.from(correlationId, autor, endereco, fotoBytes))
        );
    }

    @Transactional
    public ChamadoResponse update(Long id, ChamadoRequest request, String email) {
        Chamado chamado = findChamadoById(id);
        Usuario usuario = usuarioService.findByEmail(email);
        validarPermissao(chamado, usuario, "Você não tem permissão para editar este chamado.");

        chamadoRepository.save(request.toEntity(
                chamado,
                categoriaService.findByNome(CategoriaEnum.valueOf(request.categoria().trim().toUpperCase())),
                enderecoService.findOrCreateByCepAndNumero(request.endereco())
        ));
        return findDetailsById(id, email);
    }

    @Transactional
    public void delete(Long id, String email) {
        Chamado chamado = findChamadoById(id);
        Usuario usuario = usuarioService.findByEmail(email);
        validarPermissao(chamado, usuario, "Apenas o autor do chamado, gestor ou administrador pode excluí-lo.");
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

        Status novoStatus = statusService.findByNome(StatusChamadoEnum.valueOf(request.status().trim().toUpperCase()));
        histChamadoRepository.save(request.toEntity(chamado, operador, novoStatus));

        return findDetailsById(id, email);
    }

    @Transactional
    public ChamadoResponse aplicarTriagemIa(TriagemFeedbackMessage feedback) {
        Usuario autor = usuarioService.findById(feedback.usuarioId());
        Chamado salvo = chamadoRepository.save(feedback.toEntity(
                autor,
                enderecoService.findById(feedback.enderecoId()),
                categoriaService.findByNome(feedback.categoria()),
                prioridadeService.findByNome(feedback.prioridade()),
                orgaoGovernoService.findByNome(feedback.orgao())
        ));
        votoChamadoRepository.save(new VotoMessage(salvo.getIdChamado(), autor.getIdUsuario(), "VOTAR").toEntity(salvo, autor));
        List<HistChamado> hist = histChamadoRepository.saveAll(feedback.toHistoricos(salvo, statusService::findByNome));
        return ChamadoResponse.fromEntity(salvo, feedback.status().name(), 1L, true, hist.reversed());
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
                chamadoRepository.countByStatusNmStatus(StatusChamadoEnum.PENDENTE),
                chamadoRepository.countByStatusNmStatus(StatusChamadoEnum.EM_ANALISE) +
                chamadoRepository.countByStatusNmStatus(StatusChamadoEnum.EM_EXECUCAO),
                chamadoRepository.countByStatusNmStatus(StatusChamadoEnum.CONCLUIDO),
                chamadoRepository.countByStatusNmStatus(StatusChamadoEnum.REJEITADO),
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

    private void validarPermissao(Chamado chamado, Usuario usuario, String mensagemErro) {
        if (!chamado.getUsuario().getIdUsuario().equals(usuario.getIdUsuario()) && usuario.getCargo() != CargoUsuario.GESTOR && usuario.getCargo() != CargoUsuario.ADMIN) {
            throw new AccessDeniedException(mensagemErro);
        }
    }

    private void validarPermissaoGestao(Usuario operador) {
        if (operador.getCargo() != CargoUsuario.GESTOR && operador.getCargo() != CargoUsuario.ADMIN) {
            throw new AccessDeniedException("Apenas funcionários da prefeitura ou administradores têm permissão para atualizar o status do chamado.");
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
                        h -> h.getStatus().getNmStatus().name(),
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
}
