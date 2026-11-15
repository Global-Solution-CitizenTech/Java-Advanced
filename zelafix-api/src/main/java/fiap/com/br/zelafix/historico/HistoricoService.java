package fiap.com.br.zelafix.historico;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.ChamadoRepository;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.historico.dto.HistoricoRequest;
import fiap.com.br.zelafix.historico.dto.HistoricoResponse;
import fiap.com.br.zelafix.historico.dto.HistoricoUpdateRequest;
import fiap.com.br.zelafix.historico.status.Status;
import fiap.com.br.zelafix.historico.status.StatusChamadoEnum;
import fiap.com.br.zelafix.historico.status.StatusService;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoricoService {

    private final HistChamadoRepository histChamadoRepository;
    private final ChamadoRepository chamadoRepository;
    private final StatusService statusService;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public Page<HistoricoResponse> findAll(Pageable pageable) {
        return histChamadoRepository.findAll(pageable).map(HistoricoResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public HistoricoResponse findById(Long id) {
        return HistoricoResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoResponse> findByChamadoId(Long chamadoId) {
        return histChamadoRepository.findByChamadoIdChamadoOrderByDtMudancaDescIdHistDesc(chamadoId)
                .stream()
                .map(HistoricoResponse::fromEntity)
                .toList();
    }

    @Transactional
    public HistoricoResponse create(HistoricoRequest request, String email) {
        Chamado chamado = chamadoRepository.findById(request.chamadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Chamado com id " + request.chamadoId() + " não encontrado."));
        Usuario operador = usuarioService.findByEmail(email);
        Status status = statusService.findByNome(StatusChamadoEnum.valueOf(request.status().trim().toUpperCase()));

        HistChamado salvo = histChamadoRepository.save(request.toEntity(chamado, operador, status));
        return HistoricoResponse.fromEntity(salvo);
    }

    @Transactional
    public HistoricoResponse update(Long id, HistoricoUpdateRequest request) {
        HistChamado historico = findEntityById(id);
        Status status = statusService.findByNome(StatusChamadoEnum.valueOf(request.status().trim().toUpperCase()));

        historico.setStatus(status);
        historico.setObservacoes(request.observacao());
        return HistoricoResponse.fromEntity(histChamadoRepository.save(historico));
    }

    @Transactional
    public void delete(Long id) {
        HistChamado historico = findEntityById(id);

        histChamadoRepository.findFirstByChamadoIdChamadoOrderByDtMudancaAscIdHistAsc(historico.getChamado().getIdChamado())
                .filter(primeiro -> primeiro.getIdHist().equals(id))
                .ifPresent(p -> {
                    throw new IllegalStateException("Não é permitido excluir o evento inicial de criação do chamado.");
                });

        histChamadoRepository.delete(historico);
    }

    public HistChamado findEntityById(Long id) {
        return histChamadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Histórico com id " + id + " não encontrado."));
    }
}
