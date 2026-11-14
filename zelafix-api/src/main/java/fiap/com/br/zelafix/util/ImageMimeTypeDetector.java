package fiap.com.br.zelafix.util;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Optional;

/**
 * Utilitário para detecção dinâmica de formatos de imagem a partir de assinaturas binárias (Magic Bytes).
 * Garante validação na entrada e Content-Type exato da imagem armazenada no banco.
 */
public final class ImageMimeTypeDetector {

    private static final MimeType MIME_WEBP = MimeTypeUtils.parseMimeType("image/webp");

    private ImageMimeTypeDetector() {}

    public static boolean isImagemSuportada(byte[] bytes) {
        return detectarAssinatura(bytes).isPresent();
    }

    public static MimeType detectar(byte[] bytes) {
        return detectarAssinatura(bytes).orElse(MimeTypeUtils.IMAGE_JPEG);
    }

    private static Optional<MimeType> detectarAssinatura(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return Optional.empty();
        }

        // JPEG: FF D8
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return Optional.of(MimeTypeUtils.IMAGE_JPEG);
        }

        // PNG: 89 50 4E 47 (.PNG)
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return Optional.of(MimeTypeUtils.IMAGE_PNG);
        }

        // WEBP: RIFF....WEBP
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return Optional.of(MIME_WEBP);
        }

        // GIF: GIF8
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return Optional.of(MimeTypeUtils.IMAGE_GIF);
        }

        return Optional.empty();
    }
}
