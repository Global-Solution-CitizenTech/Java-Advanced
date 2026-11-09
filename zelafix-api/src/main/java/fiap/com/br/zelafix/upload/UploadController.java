package fiap.com.br.zelafix.upload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/uploads")
@Tag(name = "Uploads", description = "Serviço de upload e visualização de fotos de ocorrências capturadas no mobile ou web")
public class UploadController {

    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    public UploadController() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o diretório de armazenamento de uploads.", e);
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[PROTEGIDO] Enviar foto tirada no mobile/web e obter URL para cadastrar o chamado")
    public UploadResponse upload(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de imagem enviado está vazio.");
        }

        String nomeOriginal = arquivo.getOriginalFilename();
        String extensao = (nomeOriginal != null && nomeOriginal.contains("."))
                ? nomeOriginal.substring(nomeOriginal.lastIndexOf("."))
                : ".jpg";

        String nomeArquivo = UUID.randomUUID() + extensao;
        Path destino = uploadDir.resolve(nomeArquivo);
        Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(nomeArquivo)
                .toUriString();

        return new UploadResponse(url, nomeArquivo, arquivo.getSize(), arquivo.getContentType());
    }

    @GetMapping("/{nomeArquivo:.+}")
    @Operation(summary = "[PÚBLICO] Visualizar foto da ocorrência diretamente no navegador/app móvel")
    public ResponseEntity<Resource> visualizarFoto(@PathVariable String nomeArquivo) {
        try {
            Path arquivo = uploadDir.resolve(nomeArquivo).normalize();
            if (!Files.exists(arquivo) || !arquivo.startsWith(uploadDir)) {
                Path alt1 = Paths.get("zelafix-api", "uploads").resolve(nomeArquivo).normalize();
                Path alt2 = Paths.get("..", "uploads").resolve(nomeArquivo).normalize();
                if (Files.exists(alt1)) {
                    arquivo = alt1;
                } else if (Files.exists(alt2)) {
                    arquivo = alt2;
                } else {
                    return ResponseEntity.notFound().build();
                }
            }

            Resource resource = new UrlResource(arquivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(arquivo);
            if (contentType == null) {
                String lower = nomeArquivo.toLowerCase();
                if (lower.endsWith(".png")) contentType = "image/png";
                else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
                else if (lower.endsWith(".webp")) contentType = "image/webp";
                else if (lower.endsWith(".gif")) contentType = "image/gif";
                else contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
