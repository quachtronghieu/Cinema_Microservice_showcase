package vn.edu.fpt.cinema.catalog.movie.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.cinema.catalog.exception.CatalogException;
import java.io.IOException;
import java.util.*;

public final class MovieImages {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final String UPLOAD_PATH = "/assets/img/movies/uploads/";

    public record Content(String mediaType, String extension, byte[] data) {}
    private static final Map<String,String> EXTENSIONS =
        Map.of("image/jpeg","jpg", "image/png","png", "image/webp","webp");

    // The client's filename and Content-Type are untrusted; only the file's own signature decides its type.
    public static Content read(MultipartFile file) {
        if (file.getSize() > MAX_BYTES)
            throw new CatalogException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE", "Image must not exceed 5 MB");
        byte[] data;
        try { data = file.getBytes(); }
        catch (IOException e) { throw invalid("Image could not be read"); }
        String type = detect(data);
        if (type == null) throw invalid("Image must be a JPEG, PNG or WEBP file");
        return new Content(type, EXTENSIONS.get(type), data);
    }
    public static String publicPath(String imageId, String extension) { return UPLOAD_PATH + imageId + "." + extension; }
    public static String mediaTypeFor(String extension) {
        return EXTENSIONS.entrySet().stream().filter(entry -> entry.getValue().equals(extension))
            .map(Map.Entry::getKey).findFirst().orElse(null);
    }
    public static CatalogException invalid(String message) {
        return new CatalogException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid movie data",
            Map.of("image", List.of(message)));
    }
    private static String detect(byte[] d) {
        if (d.length >= 3 && (d[0]&0xFF)==0xFF && (d[1]&0xFF)==0xD8 && (d[2]&0xFF)==0xFF) return "image/jpeg";
        if (startsWith(d, 0, new byte[]{(byte)0x89,'P','N','G',0x0D,0x0A,0x1A,0x0A})) return "image/png";
        if (startsWith(d, 0, new byte[]{'R','I','F','F'}) && startsWith(d, 8, new byte[]{'W','E','B','P'})) return "image/webp";
        return null;
    }
    private static boolean startsWith(byte[] data, int offset, byte[] signature) {
        return data.length >= offset + signature.length
            && Arrays.equals(data, offset, offset + signature.length, signature, 0, signature.length);
    }
    private MovieImages() {}
}
