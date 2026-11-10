package fiap.com.br.zelafix.worker.ai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageMimeTypeDetectorTest {

    @Test
    @DisplayName("Deve detectar JPEG via Magic Bytes (FF D8)")
    void deveDetectarJpeg() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
        MimeType mimeType = ImageMimeTypeDetector.detectar(jpegBytes);
        assertEquals(MimeTypeUtils.IMAGE_JPEG, mimeType);
    }

    @Test
    @DisplayName("Deve detectar PNG via Magic Bytes (89 50 4E 47)")
    void deveDetectarPng() {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MimeType mimeType = ImageMimeTypeDetector.detectar(pngBytes);
        assertEquals(MimeTypeUtils.IMAGE_PNG, mimeType);
    }

    @Test
    @DisplayName("Deve detectar WEBP via Magic Bytes (RIFF...WEBP)")
    void deveDetectarWebp() {
        byte[] webpBytes = new byte[]{
                'R', 'I', 'F', 'F',
                0, 0, 0, 0,
                'W', 'E', 'B', 'P',
                'V', 'P', '8', ' '
        };
        MimeType mimeType = ImageMimeTypeDetector.detectar(webpBytes);
        assertEquals("image/webp", mimeType.toString());
    }

    @Test
    @DisplayName("Deve detectar GIF via Magic Bytes (GIF8)")
    void deveDetectarGif() {
        byte[] gifBytes = new byte[]{'G', 'I', 'F', '8', '9', 'a'};
        MimeType mimeType = ImageMimeTypeDetector.detectar(gifBytes);
        assertEquals(MimeTypeUtils.IMAGE_GIF, mimeType);
    }

    @Test
    @DisplayName("Deve retornar JPEG como fallback quando bytes forem nulos ou desconhecidos")
    void deveRetornarFallbackParaJpeg() {
        assertEquals(MimeTypeUtils.IMAGE_JPEG, ImageMimeTypeDetector.detectar(null));
        assertEquals(MimeTypeUtils.IMAGE_JPEG, ImageMimeTypeDetector.detectar(new byte[]{0, 1}));
    }
}
