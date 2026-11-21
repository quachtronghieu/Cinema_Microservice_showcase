package vn.edu.fpt.cinema.catalog.concession.validation;

import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ConcessionImages {
    private static final String PREFIX = "/assets/img/concessions/";
    private static final Set<String> FILENAMES = load();
    private ConcessionImages() {}
    private static Set<String> load() {
        try (var in = new ClassPathResource("concession-images.txt").getInputStream();
             var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            var filenames = new HashSet<String>();
            for (String line; (line = reader.readLine()) != null;) {
                if (line.isBlank()) continue;
                if (line.contains("/") || line.contains("\\") || !new ClassPathResource("static" + PREFIX + line).exists()) {
                    throw new IllegalStateException("Invalid packaged concession image allowlist");
                }
                filenames.add(line);
            }
            return Set.copyOf(filenames);
        } catch (IOException e) { throw new IllegalStateException("Cannot load concession image allowlist", e); }
    }
    public static boolean contains(String filename) { return FILENAMES.contains(filename); }
    public static String publicPath(String filename) { return PREFIX + filename; }
}
