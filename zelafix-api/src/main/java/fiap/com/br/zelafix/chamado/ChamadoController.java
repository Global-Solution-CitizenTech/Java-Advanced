package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.chamado.dto.ChamadoFiltroRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.ChamadoStatusRequest;
import fiap.com.br.zelafix.chamado.dto.EstatisticasResponse;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.util.ImageMimeTypeDetector;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/chamados")
@RequiredArgsConstructor
@Tag(name = "Chamados", description = "Operações completas de CRUD e gestão de chamados de zeladoria urbana")
public class ChamadoController {

    private final ChamadoService chamadoService;

    // =========================================================================
    // 1. CONSULTAS E LISTAGENS
    // =========================================================================

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Listar chamados com paginação (Pageable) e filtros dinâmicos")
    public Page<ChamadoResponse> findAll(@ParameterObject ChamadoFiltroRequest filtro, @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "dtAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        return chamadoService.findAll(filtro, jwt.getSubject(), pageable);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Buscar chamado por ID com dados completos e histórico de auditoria")
    public ChamadoResponse findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return chamadoService.findDetailsById(id, jwt.getSubject());
    }

    @GetMapping("/{id}/foto")
    @Operation(summary = "Obter a imagem fotográfica da ocorrência armazenada em BLOB no Oracle com Content-Type dinâmico")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) {
        byte[] foto = chamadoService.getPhoto(id);
        MimeType mimeType = ImageMimeTypeDetector.detectar(foto);
        return ResponseEntity.ok()
                .contentType(MediaType.asMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(foto);
    }

    // =========================================================================
    // 2. ABERTURA DE CHAMADO (TRIAGEM VIA RABBITMQ)
    // =========================================================================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abrir chamado enviando endereço e foto via multipart/form-data com triagem inteligente por IA via RabbitMQ")
    public DeferredResult<ChamadoResponse> create(@ModelAttribute @Valid EnderecoRequest endereco, @RequestPart("foto") MultipartFile foto, @AuthenticationPrincipal Jwt jwt) throws IOException {
        return chamadoService.create(endereco, foto.getBytes(), jwt.getSubject());
    }

    // =========================================================================
    // 3. CICLO DE VIDA (EDIÇÃO, STATUS, APOIO E EXCLUSÃO)
    // =========================================================================

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Atualizar dados do chamado")
    public ChamadoResponse update(@PathVariable Long id, @RequestBody @Valid ChamadoRequest request, @AuthenticationPrincipal Jwt jwt) {
        return chamadoService.update(id, request, jwt.getSubject());
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    @Operation(summary = "[GESTOR / ADMIN] Atualizar status do chamado (ex: CONCLUIDO, EM_EXECUCAO) com registro de histórico")
    public ChamadoResponse updateStatus(@PathVariable Long id, @RequestBody @Valid ChamadoStatusRequest request, @AuthenticationPrincipal Jwt jwt) {
        return chamadoService.updateStatus(id, request, jwt.getSubject());
    }

    @PostMapping("/{id}/apoiar")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Apoiar um chamado comunitário (ou desfazer apoio) via RabbitMQ assíncrono")
    public ChamadoResponse apoiar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return chamadoService.apoiar(id, jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "[AUTOR / GESTOR / ADMIN] Excluir um chamado")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        chamadoService.delete(id, jwt.getSubject());
    }

    // =========================================================================
    // 4. PAINEL DE ESTATÍSTICAS
    // =========================================================================

    @GetMapping("/estatisticas")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Obter estatísticas consolidadas para os cards do dashboard")
    public EstatisticasResponse getStatistics() {
        return chamadoService.getStatistics();
    }
}
