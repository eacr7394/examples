
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
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

    private final JTextField versionField = new JTextField(10);
    private final JTextField authorField = new JTextField(20);
    private final JTextArea changeDescArea = new JTextArea(3, 40);

    private final Map<String, Map<String, String>> pathErrorsMap = new HashMap<>();
    private Map<String, Object> openApi;
    private File currentFile;

    public OpenApiPathSelectorUI() {
        setTitle("OpenAPI Path Selector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
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

        JButton saveJsonButton = new JButton("Guardar configuración JSON");
        saveJsonButton.addActionListener(e -> saveConfig());

        JButton generateMdButton = new JButton("Generar Markdown");
        generateMdButton.addActionListener(e -> MarkdownGenerator.generate(getConfigList(), getVersion(), getAuthor(), getDescription()));

        JButton generatePdfButton = new JButton("Generar PDF");
        generatePdfButton.addActionListener(e -> PdfGenerator.generate("documentacion.md", "documentacion.pdf"));

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

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(loadButton);
        topPanel.add(saveJsonButton);
        topPanel.add(generateMdButton);
        topPanel.add(generatePdfButton);

        JPanel metaPanel = new JPanel(new GridLayout(3, 2));
        metaPanel.setBorder(BorderFactory.createTitledBorder("Metadatos"));
        metaPanel.add(new JLabel("Versión:"));
        metaPanel.add(versionField);
        metaPanel.add(new JLabel("Desarrollador:"));
        metaPanel.add(authorField);
        metaPanel.add(new JLabel("Descripción de cambios:"));
        metaPanel.add(new JScrollPane(changeDescArea));

        JPanel centerPanel = new JPanel(new GridLayout(1, 3));
        centerPanel.add(new JScrollPane(allPathsList));

        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1));
        buttonsPanel.add(addButton);
        buttonsPanel.add(removeButton);
        centerPanel.add(buttonsPanel);
        centerPanel.add(new JScrollPane(selectedPathsList));

        JPanel middlePanel = new JPanel(new BorderLayout());
        middlePanel.add(centerPanel, BorderLayout.CENTER);
        middlePanel.add(metaPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(middlePanel, BorderLayout.CENTER);
        add(curlPanel, BorderLayout.SOUTH);

        // Drag & Drop
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

    private List<PathConfig> getConfigList() {
        return Collections.list(selectedPathsModel.elements()).stream()
            .map(path -> new PathConfig(path, pathErrorsMap.getOrDefault(path, new HashMap<>())))
            .collect(Collectors.toList());
    }

    private String getVersion() {
        return versionField.getText().trim();
    }

    private String getAuthor() {
        return authorField.getText().trim();
    }

    private String getDescription() {
        return changeDescArea.getText().trim();
    }

    private void saveConfig() {
        try {
            DocumentationConfig config = new DocumentationConfig(getVersion(), getAuthor(), getDescription(), getConfigList());
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("configuracion.json"), config);
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
