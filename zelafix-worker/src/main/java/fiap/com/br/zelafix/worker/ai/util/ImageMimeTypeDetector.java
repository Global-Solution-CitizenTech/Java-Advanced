package fiap.com.br.zelafix.worker.ai.util;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utilitário para detecção dinâmica de formatos de imagem a partir de assinaturas binárias (Magic Bytes).
 * Garante compatibilidade universal com fotos capturadas em dispositivos móveis (Android e iOS / Expo).
 */
public final class ImageMimeTypeDetector {

    private static final MimeType MIME_WEBP = MimeTypeUtils.parseMimeType("image/webp");
    private static final int MAX_IA_IMAGE_DIMENSION = 768;
    private static final int COMPACT_THRESHOLD_BYTES = 150 * 1024;

    private ImageMimeTypeDetector() {}

    /**
     * Redimensiona uma cópia da imagem para no máximo 768px de lado maior em JPEG,
     * reduzindo drasticamente o consumo de tokens visuais (TPM) no modelo multimodal.
     */
    public static byte[] redimensionarParaIa(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
            if (original == null) {
                return bytes;
            }
            int largura = original.getWidth();
            int altura = original.getHeight();
            int maiorLado = Math.max(largura, altura);
            if (maiorLado <= MAX_IA_IMAGE_DIMENSION && bytes.length <= COMPACT_THRESHOLD_BYTES) {
                return bytes;
            }
            double escala = maiorLado > MAX_IA_IMAGE_DIMENSION ? (double) MAX_IA_IMAGE_DIMENSION / maiorLado : 1.0;
            int novaLargura = Math.max(1, (int) Math.round(largura * escala));
            int novaAltura = Math.max(1, (int) Math.round(altura * escala));

            BufferedImage convertida = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = convertida.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, novaLargura, novaAltura, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            return ImageIO.write(convertida, "jpg", out) ? out.toByteArray() : bytes;
        } catch (IOException e) {
            return bytes;
        }
    }

    public static MimeType detectar(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return MimeTypeUtils.IMAGE_JPEG;
        }

        // JPEG: FF D8
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return MimeTypeUtils.IMAGE_JPEG;
        }

        // PNG: 89 50 4E 47 (.PNG)
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return MimeTypeUtils.IMAGE_PNG;
        }

        // WEBP: RIFF....WEBP
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return MIME_WEBP;
        }

        // GIF: GIF8
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return MimeTypeUtils.IMAGE_GIF;
        }

        return MimeTypeUtils.IMAGE_JPEG;
    }
}
