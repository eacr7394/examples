# Proyecto Java completo: OpenAPI GUI con selección de paths y ejemplos curl por error
# Las siguientes clases conforman la aplicación estructurada:

# === Archivo: OpenApiApp.java ===
"""
import javax.swing.SwingUtilities;

public class OpenApiApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OpenApiPathSelectorUI().setVisible(true));
    }
}
"""

# === Archivo: OpenApiPathSelectorUI.java ===
"""
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class OpenApiPathSelectorUI extends JFrame {
    private final DefaultListModel<String> allPathsModel = new DefaultListModel<>();
    private final DefaultListModel<String> selectedPathsModel = new DefaultListModel<>();
    private final JList<String> allPathsList = new JList<>(allPathsModel);
    private final JList<String> selectedPathsList = new JList<>(selectedPathsModel);
    private final JComboBox<String> errorCodeDropdown = new JComboBox<>();
    private final JTextArea curlTextArea = new JTextArea(5, 40);
    private final JPanel curlPanel = new JPanel(new BorderLayout());
    private final Map<String, Map<String, String>> pathErrorsMap = new HashMap<>();
    private Map<String, Object> openApi;
    private File currentFile;

    public OpenApiPathSelectorUI() {
        setTitle("OpenAPI Path Selector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLayout(new BorderLayout());

        JButton loadButton = new JButton("Cargar archivo OpenAPI");
        loadButton.addActionListener(this::loadOpenApiFile);

        JButton addButton = new JButton("Agregar >>");
        addButton.addActionListener(e -> {
            moveSelectedPath(allPathsList, allPathsModel, selectedPathsModel);
            saveConfig();
        });

        JButton removeButton = new JButton("<< Quitar");
        removeButton.addActionListener(e -> {
            moveSelectedPath(selectedPathsList, selectedPathsModel, allPathsModel);
            saveConfig();
        });

        errorCodeDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateCurlArea();
            }
        });

        curlTextArea.setVisible(false);
        curlTextArea.setLineWrap(true);
        curlTextArea.setWrapStyleWord(true);
        curlTextArea.setBorder(BorderFactory.createTitledBorder("Ejemplo curl"));

        curlPanel.setBorder(BorderFactory.createTitledBorder("Ejemplo curl por código de error"));
        curlPanel.add(errorCodeDropdown, BorderLayout.NORTH);
        curlPanel.add(new JScrollPane(curlTextArea), BorderLayout.CENTER);

        curlTextArea.getDocument().addDocumentListener((SimpleDocumentListener) e -> {
            String path = selectedPathsList.getSelectedValue();
            String code = (String) errorCodeDropdown.getSelectedItem();
            if (path != null && code != null) {
                pathErrorsMap.computeIfAbsent(path, k -> new HashMap<>()).put(code, curlTextArea.getText());
                saveConfig();
            }
        });

        selectedPathsList.addListSelectionListener(e -> {
            updateErrorDropdown();
            updateCurlArea();
        });

        JPanel topPanel = new JPanel();
        topPanel.add(loadButton);

        JPanel centerPanel = new JPanel(new GridLayout(1, 3));
        centerPanel.add(new JScrollPane(allPathsList));

        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1));
        buttonsPanel.add(addButton);
        buttonsPanel.add(removeButton);
        centerPanel.add(buttonsPanel);
        centerPanel.add(new JScrollPane(selectedPathsList));

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(curlPanel, BorderLayout.SOUTH);

        // Drag and Drop
        setTransferHandler(new TransferHandler() {
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            public boolean importData(TransferSupport support) {
                try {
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        loadOpenApiFromFile(files.get(0));
                        return true;
                    }
                } catch (Exception ex) {
                    showError("Error al cargar archivo: " + ex.getMessage());
                }
                return false;
            }
        });
    }

    private void loadOpenApiFile(ActionEvent e) {
        File file = OpenApiFileLoader.chooseFile(this);
        if (file != null) {
            loadOpenApiFromFile(file);
        }
    }

    private void loadOpenApiFromFile(File file) {
        try {
            openApi = OpenApiFileLoader.loadOpenApi(file);
            currentFile = file;
            Map<String, Object> paths = (Map<String, Object>) openApi.get("paths");
            allPathsModel.clear();
            selectedPathsModel.clear();
            pathErrorsMap.clear();
            for (String path : paths.keySet()) {
                allPathsModel.addElement(path);
            }
            showInfo("Archivo cargado: " + file.getName());
        } catch (Exception ex) {
            showError("Error cargando el archivo: " + ex.getMessage());
        }
    }

    private void moveSelectedPath(JList<String> sourceList, DefaultListModel<String> sourceModel, DefaultListModel<String> targetModel) {
        List<String> selected = sourceList.getSelectedValuesList();
        for (String item : selected) {
            sourceModel.removeElement(item);
            if (!targetModel.contains(item)) {
                targetModel.addElement(item);
            }
        }
    }

    private void updateErrorDropdown() {
        errorCodeDropdown.removeAllItems();
        curlTextArea.setVisible(false);
        if (selectedPathsList.getSelectedValue() == null) return;

        Map<String, Object> pathObj = (Map<String, Object>) ((Map<String, Object>) openApi.get("paths")).get(selectedPathsList.getSelectedValue());
        if (pathObj == null) return;

        pathObj.values().forEach(method -> {
            if (method instanceof Map) {
                Map<String, Object> methodMap = (Map<String, Object>) method;
                Map<String, Object> responses = (Map<String, Object>) methodMap.get("responses");
                if (responses != null) {
                    for (String code : responses.keySet()) {
                        errorCodeDropdown.addItem(code);
                    }
                }
            }
        });
    }

    private void updateCurlArea() {
        String path = selectedPathsList.getSelectedValue();
        String code = (String) errorCodeDropdown.getSelectedItem();
        if (path != null && code != null) {
            curlTextArea.setText(pathErrorsMap.getOrDefault(path, new HashMap<>()).getOrDefault(code, ""));
            curlTextArea.setVisible(true);
        } else {
            curlTextArea.setVisible(false);
        }
    }

    private void saveConfig() {
        try {
            List<PathConfig> configList = Collections.list(selectedPathsModel.elements()).stream()
                .map(path -> new PathConfig(path, pathErrorsMap.getOrDefault(path, new HashMap<>())))
                .collect(Collectors.toList());
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("configuracion.json"), configList);
        } catch (Exception ex) {
            showError("Error guardando configuración: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}
"""

# === Archivo: PathConfig.java ===
"""
import java.util.Map;

public class PathConfig {
    public String path;
    public Map<String, String> errors;

    public PathConfig() {} // requerido para deserialización

    public PathConfig(String path, Map<String, String> errors) {
        this.path = path;
        this.errors = errors;
    }
}
"""

# === Archivo: OpenApiFileLoader.java ===
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

# === Interfaz funcional para detectar cambios en el JTextArea ===
# Archivo: SimpleDocumentListener.java
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
