package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.categoria.CategoriaService;
import fiap.com.br.zelafix.chamado.dto.ChamadoCreateRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.ChamadoUpdateRequest;
import fiap.com.br.zelafix.chamado.dto.EstatisticasResponse;
import fiap.com.br.zelafix.chamado.dto.StatusUpdateRequest;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.EnderecoService;
import fiap.com.br.zelafix.exception.BusinessException;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.historico.HistStatus;
import fiap.com.br.zelafix.historico.HistStatusRepository;
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
    private final HistStatusRepository histStatusRepository;
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
        Usuario usuario = usuarioService.buscarPorEmail(userEmail);
        Long autorId = Boolean.TRUE.equals(apenasMeus) ? usuario.getIdUsuario() : null;

        return chamadoRepository.findByFiltros(status, categoria, autorId, busca, pageable)
                .map(c -> ChamadoResponse.fromEntity(c, usuario.getIdUsuario()));
    }

    public ChamadoResponse buscarPorId(Long id, String userEmail) {
        Chamado chamado = findChamadoById(id);
        Usuario usuario = usuarioService.buscarPorEmail(userEmail);
        return ChamadoResponse.fromEntity(chamado, usuario.getIdUsuario());
    }

    public Chamado buscarEntidadePorId(Long id) {
        return findChamadoById(id);
    }

    @Transactional
    public ChamadoResponse criar(ChamadoCreateRequest request, String userEmail) {
        Usuario autor = usuarioService.buscarPorEmail(userEmail);
        Categoria categoria = categoriaService.buscarPorNomeOuCriar(request.categoria());
        Endereco endereco = enderecoService.findOrCreateByCepAndNumero(request.endereco());
        Status statusPendente = statusService.buscarPorNomeOuCriar("Pendente Triagem");

        Chamado salvo = chamadoRepository.save(request.toEntity(autor, categoria, endereco, statusPendente));

        votoChamadoRepository.save(new VotoChamado(salvo, autor));
        histStatusRepository.save(HistStatus.inicial(salvo, autor, statusPendente));

        chamadoEventProducer.publicarTriagem(new ChamadoTriagemMessage(
                salvo.getIdChamado(),
                salvo.getProtocolo(),
                salvo.getTitulo(),
                salvo.getDescricao(),
                categoria.getNmCategoria(),
                endereco.getBairro().getNome()
        ));

        return ChamadoResponse.fromEntity(salvo, autor.getIdUsuario());
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoUpdateRequest request, String userEmail) {
        Chamado chamado = findChamadoById(id);
        Usuario autor = usuarioService.buscarPorEmail(userEmail);

        Categoria categoria = (request.categoria() != null && !request.categoria().isBlank())
                ? categoriaService.buscarPorNomeOuCriar(request.categoria())
                : null;
        Endereco endereco = (request.endereco() != null)
                ? enderecoService.findOrCreateByCepAndNumero(request.endereco())
                : null;

        request.aplicarEm(chamado, categoria, endereco);

        Chamado atualizado = chamadoRepository.save(chamado);
        return ChamadoResponse.fromEntity(atualizado, autor.getIdUsuario());
    }

    @Transactional
    public ChamadoResponse atualizarStatus(Long id, StatusUpdateRequest request, String userEmail) {
        Chamado chamado = findChamadoById(id);
        Usuario gestor = usuarioService.buscarPorEmail(userEmail);

        if (gestor.getCargo() != CargoUsuario.GESTOR) {
            throw new BusinessException("Apenas servidores de órgãos públicos (GESTOR) têm permissão para dar prosseguimento ou atualizar o status do chamado.");
        }

        String statusAnterior = chamado.getStatus().getNmStatus();
        Status novoStatus = statusService.buscarPorNomeOuCriar(request.novoStatus());
        chamado.atualizarStatus(novoStatus, request.feedbackResolucao());

        histStatusRepository.save(request.toHistorico(chamado, gestor, novoStatus, statusAnterior));
        return ChamadoResponse.fromEntity(chamadoRepository.save(chamado), gestor.getIdUsuario());
    }

    @Transactional
    public ChamadoResponse apoiar(Long id, String userEmail) {
        Chamado chamado = findChamadoById(id);
        Usuario usuario = usuarioService.buscarPorEmail(userEmail);

        Optional<VotoChamado> votoExistente = votoChamadoRepository.findByChamadoIdChamadoAndUsuarioIdUsuario(id, usuario.getIdUsuario());

        if (votoExistente.isPresent()) {
            votoChamadoRepository.delete(votoExistente.get());
            chamado.decrementarApoio();
            votoEventProducer.publicarVoto(new VotoMessage(id, usuario.getIdUsuario(), "REMOVER"));
        } else {
            votoChamadoRepository.save(new VotoChamado(chamado, usuario));
            chamado.incrementarApoio();
            votoEventProducer.publicarVoto(new VotoMessage(id, usuario.getIdUsuario(), "VOTAR"));
        }

        return ChamadoResponse.fromEntity(chamadoRepository.save(chamado), usuario.getIdUsuario());
    }

    @Transactional
    public void deletar(Long id) {
        Chamado chamado = findChamadoById(id);
        chamadoRepository.delete(chamado);
    }

    public EstatisticasResponse obterEstatisticas() {
        long total = chamadoRepository.count();
        long abertos = chamadoRepository.countByStatusNmStatusIgnoreCase("Aberto")
                + chamadoRepository.countByStatusNmStatusIgnoreCase("Pendente Triagem");
        long emAndamento = chamadoRepository.countByStatusNmStatusIgnoreCase("Em Andamento");
        long resolvidos = chamadoRepository.countByStatusNmStatusIgnoreCase("Resolvido")
                + chamadoRepository.countByStatusNmStatusIgnoreCase("Concluído");
        long rejeitados = chamadoRepository.countByStatusNmStatusIgnoreCase("Rejeitado");

        long totalApoios = votoChamadoRepository.count();

        return new EstatisticasResponse(total, abertos, emAndamento, resolvidos, rejeitados, totalApoios);
    }

    private Chamado findChamadoById(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado com id " + id + " não encontrado."));
    }
}
