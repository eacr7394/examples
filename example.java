# === 1. OpenApiApp.java ===
"""
import javax.swing.SwingUtilities;

public class OpenApiApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OpenApiPathSelectorUI().setVisible(true));
    }
}
"""

# === 2. DocumentationConfig.java ===
"""
import java.util.List;

public class DocumentationConfig {
    public String version;
    public String developer;
    public String description;
    public List<PathConfig> paths;

    public DocumentationConfig() {}

    public DocumentationConfig(String version, String developer, String description, List<PathConfig> paths) {
        this.version = version;
        this.developer = developer;
        this.description = description;
        this.paths = paths;
    }
}
"""

# === 3. PathConfig.java ===
"""
import java.util.Map;

public class PathConfig {
    public String path;
    public Map<String, String> errors;

    public PathConfig() {}

    public PathConfig(String path, Map<String, String> errors) {
        this.path = path;
        this.errors = errors;
    }
}
"""

# === 4. SimpleDocumentListener.java ===
"""
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

@FunctionalInterface
public interface SimpleDocumentListener extends DocumentListener {
    void update(DocumentEvent e);

    default void insertUpdate(DocumentEvent e) { update(e); }
    default void removeUpdate(DocumentEvent e) { update(e); }
    default void changedUpdate(DocumentEvent e) { update(e); }
}
"""

# === 5. OpenApiFileLoader.java ===
"""
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Map;

public class OpenApiFileLoader {
    public static File chooseFile(Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(parent);
        return result == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
    }

    public static Map<String, Object> loadOpenApi(File file) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(file, Map.class);
    }
}
"""

# === 6. PdfGenerator.java ===
"""
public class PdfGenerator {
    public static void generate(String mdFile, String pdfFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder("pandoc", mdFile, "-o", pdfFile);
            pb.inheritIO();
            Process p = pb.start();
            p.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
"""

# === 7. MarkdownGenerator.java ===
"""
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class MarkdownGenerator {
    public static void generate(List<PathConfig> paths, String version, String developer, String description, String outputFile) {
        try (PrintWriter out = new PrintWriter(new FileWriter(outputFile))) {
            String fechaActual = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            out.println("<div style='page-break-after: always'></div>");
            out.println("# Documentación Técnica de Interfaz");
            out.println("### BIF: Canal Digital  Nombre: [DEFINIR]");
            out.println("### Presidencia de Tecnología\n");

            out.println("### HISTORIAL DE MODIFICACIONES");
            out.println("| Versión | Fecha | Autor | Descripción |");
            out.println("|---------|-------|-------|-------------|");
            out.printf("| %s | %s | %s | %s |\n", version, fechaActual, developer, description);
            out.println();

            for (PathConfig pathConfig : paths) {
                out.println("<div style='page-break-after: always'></div>");
                out.println("## ENDPOINT");
                out.println("**Método:** POST");
                out.printf("**URL:** `%s`\n\n", pathConfig.path);
                out.println("**Tipo de consumo:** Body + Headers\n");

                out.println("### PARÁMETROS DE ENTRADA");
                out.println("| Nombre | Tipo de dato | Ubicación | Requerido | Descripción |");
                out.println("|--------|--------------|-----------|-----------|-------------|");
                out.println("| Authorization | String | Header | Sí | Token de autorización |");
                out.println("| Accept-Language | String | Header | No | Idioma preferido |");
                out.println("| application | String | Header | Sí | Aplicación que invoca |\n");

                out.println("### BODY DE ENTRADA");
                out.println("```json");
                out.println("{\n  \"campo1\": \"valor1\",\n  \"campo2\": 123\n}");
                out.println("```");

                out.println("### RESPUESTA");
                out.println("```json");
                out.println("{\n  \"status\": \"OK\"\n}");
                out.println("```");

                out.println("### ERRORES");
                out.println("| Código | Tipo | Descripción |");
                out.println("|--------|------|-------------|");
                for (Map.Entry<String, String> entry : pathConfig.errors.entrySet()) {
                    out.printf("| %s | Lógico | %s |\n", entry.getKey(), entry.getValue().split("\n")[0]);
                }

                out.println("### CURL");
                for (Map.Entry<String, String> entry : pathConfig.errors.entrySet()) {
                    out.printf("#### Código %s\n", entry.getKey());
                    out.println("```bash");
                    out.println(entry.getValue());
                    out.println("```");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
"""
