package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.chamado.dto.ChamadoRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.EstatisticasResponse;
import fiap.com.br.zelafix.chamado.dto.StatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/chamados")
@RequiredArgsConstructor
@Tag(name = "Chamados", description = "Operações completas de CRUD e gestão de chamados de zeladoria urbana")
public class ChamadoController {

    private final ChamadoService chamadoService;

    @GetMapping
    @Operation(summary = "Listar chamados com paginação (Pageable) e filtros dinâmicos")
    public Page<ChamadoResponse> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Boolean apenasMeus,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "dtAbertura", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return chamadoService.findAll(status, categoria, busca, apenasMeus, jwt.getSubject(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar chamado por ID com dados completos e histórico de auditoria")
    public ChamadoResponse findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return chamadoService.findDetailsById(id, jwt.getSubject());
    }

    @GetMapping(value = "/{id}/foto", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    @Operation(summary = "Obter a imagem fotográfica da ocorrência armazenada em BLOB no Oracle")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"chamado-" + id + ".jpg\"")
                .body(chamadoService.getPhoto(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abrir novo chamado com payload JSON (foto em base64)")
    public ChamadoResponse create(
            @RequestBody @Valid ChamadoRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return chamadoService.create(request, jwt.getSubject());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abrir novo chamado enviando foto via MultipartFile (armazenada em BLOB)")
    public ChamadoResponse createWithMultipart(
            @RequestPart("dados") @Valid ChamadoRequest request,
            @RequestPart(value = "foto", required = false) MultipartFile foto,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        byte[] bytes = foto != null ? foto.getBytes() : null;
        return chamadoService.create(request, bytes, jwt.getSubject());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do chamado")
    public ChamadoResponse update(
            @PathVariable Long id,
            @RequestBody @Valid ChamadoRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return chamadoService.update(id, request, jwt.getSubject());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    @Operation(summary = "[GESTOR / ADMIN] Atualizar status do chamado (ex: CONCLUIDO, EM_EXECUCAO) com registro de histórico")
    public ChamadoResponse updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid StatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return chamadoService.updateStatus(id, request, jwt.getSubject());
    }

    @PostMapping("/{id}/apoiar")
    @Operation(summary = "Apoiar um chamado comunitário (ou desfazer apoio) via RabbitMQ assíncrono")
    public ChamadoResponse apoiar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return chamadoService.apoiar(id, jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Excluir um chamado")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        chamadoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Obter estatísticas consolidadas para os cards do dashboard")
    public EstatisticasResponse getStatistics() {
        return chamadoService.getStatistics();
    }
}
