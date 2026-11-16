package fiap.com.br.zelafix.util;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * Utilitário para detecção dinâmica de formatos de imagem a partir de assinaturas binárias (Magic Bytes).
 * Garante validação na entrada e Content-Type exato da imagem armazenada no banco.
 */
public final class ImageMimeTypeDetector {

    private static final MimeType MIME_WEBP = MimeTypeUtils.parseMimeType("image/webp");
    private static final int MAX_DIMENSAO_ARMAZENAMENTO = 1080;
    private static final int LIMIAR_COMPACTACAO_BYTES = 200_000;

    private ImageMimeTypeDetector() {}

    public static byte[] otimizarParaArmazenamento(byte[] bytes) {
        if (bytes == null || bytes.length <= LIMIAR_COMPACTACAO_BYTES) {
            return bytes;
        }
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
            if (original == null) {
                return bytes;
            }
            int maiorLado = Math.max(original.getWidth(), original.getHeight());
            double escala = maiorLado > MAX_DIMENSAO_ARMAZENAMENTO ? (double) MAX_DIMENSAO_ARMAZENAMENTO / maiorLado : 1.0;
            int largura = Math.max(1, (int) Math.round(original.getWidth() * escala));
            int altura = Math.max(1, (int) Math.round(original.getHeight() * escala));

            BufferedImage jpeg = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = jpeg.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, largura, altura, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            return ImageIO.write(jpeg, "jpg", out) && out.size() < bytes.length ? out.toByteArray() : bytes;
        } catch (IOException e) {
            return bytes;
        }
    }

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
