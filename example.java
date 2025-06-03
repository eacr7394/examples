import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenApiApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OpenApiPathSelectorUI().setVisible(true));
    }
}

class OpenApiPathSelectorUI extends JFrame {
    private final DefaultListModel<String> allPathsModel = new DefaultListModel<>();
    private final DefaultListModel<String> selectedPathsModel = new DefaultListModel<>();
    private final JList<String> allPathsList = new JList<>(allPathsModel);
    private final JList<String> selectedPathsList = new JList<>(selectedPathsModel);
    private final JTextArea curlTextArea = new JTextArea(5, 40);
    private final JTextField filePathField = new JTextField(40);
    private Map<String, Object> openApi;

    public OpenApiPathSelectorUI() {
        setTitle("OpenAPI Path Selector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLayout(new BorderLayout());

        JButton loadButton = new JButton("Cargar archivo OpenAPI");
        loadButton.addActionListener(this::loadOpenApiFile);

        JButton addButton = new JButton("Agregar >>");
        addButton.addActionListener(e -> moveSelectedPath(allPathsList, allPathsModel, selectedPathsModel));

        JButton removeButton = new JButton("<< Quitar");
        removeButton.addActionListener(e -> moveSelectedPath(selectedPathsList, selectedPathsModel, allPathsModel));

        JPanel topPanel = new JPanel();
        topPanel.add(loadButton);
        topPanel.add(filePathField);

        JPanel centerPanel = new JPanel(new GridLayout(1, 3));
        centerPanel.add(new JScrollPane(allPathsList));

        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1));
        buttonsPanel.add(addButton);
        buttonsPanel.add(removeButton);
        centerPanel.add(buttonsPanel);

        centerPanel.add(new JScrollPane(selectedPathsList));

        JPanel curlPanel = new JPanel(new BorderLayout());
        curlPanel.setBorder(BorderFactory.createTitledBorder("Ejemplo curl por error"));
        curlPanel.add(new JScrollPane(curlTextArea), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(curlPanel, BorderLayout.SOUTH);
    }

    private void loadOpenApiFile(ActionEvent e) {
        File file = OpenApiFileLoader.chooseFile(this);
        if (file != null) {
            filePathField.setText(file.getAbsolutePath());
            try {
                openApi = OpenApiFileLoader.loadOpenApi(file);
                Map<String, Object> paths = (Map<String, Object>) openApi.get("paths");
                allPathsModel.clear();
                selectedPathsModel.clear();
                for (String path : paths.keySet()) {
                    allPathsModel.addElement(path);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error cargando el archivo: " + ex.getMessage());
            }
        }
    }

    private void moveSelectedPath(JList<String> sourceList, DefaultListModel<String> sourceModel, DefaultListModel<String> targetModel) {
        List<String> selected = sourceList.getSelectedValuesList();
        for (String item : selected) {
            sourceModel.removeElement(item);
            targetModel.addElement(item);
        }
    }
}

class OpenApiFileLoader {
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
