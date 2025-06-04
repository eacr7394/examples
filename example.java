Excelente pregunta. Para que al abrir el programa se **autopoblen los campos (paths, versión, autor, descripción y ejemplos curl)** desde un `.json` guardado anteriormente, debes:

---

## ✅ PASOS PARA AUTOCARGA DESDE `configuracion.json`

---

### 🟦 1. Agrega método `loadConfigFromJson()`

En `OpenApiPathSelectorUI.java`, agrega:

```java
private void loadConfigFromJson(File jsonFile) {
    try {
        ObjectMapper mapper = new ObjectMapper();
        DocumentationConfig config = mapper.readValue(jsonFile, DocumentationConfig.class);

        versionField.setText(config.version);
        authorField.setText(config.developer);
        changeDescArea.setText(config.description);

        selectedPathsModel.clear();
        pathErrorsMap.clear();

        for (PathConfig pathCfg : config.paths) {
            selectedPathsModel.addElement(pathCfg.path);
            pathErrorsMap.put(pathCfg.path, new HashMap<>(pathCfg.errors));
        }

        showInfo("Configuración cargada desde JSON");
    } catch (Exception ex) {
        showError("Error cargando configuración JSON: " + ex.getMessage());
    }
}
```

---

### 🟦 2. Llama a ese método en el constructor si el archivo existe:

```java
File configFile = new File("configuracion.json");
if (configFile.exists()) {
    loadConfigFromJson(configFile);
}
```

Colócalo al final del constructor de `OpenApiPathSelectorUI`.

---

### 🟦 3. Asegúrate que estas clases están bien definidas:

#### 📄 `DocumentationConfig.java`

```java
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
```

#### 📄 `PathConfig.java`

```java
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
```

---

## ✅ Resultado

* Al abrir la aplicación, si existe `configuracion.json`, todos los campos se cargarán automáticamente.
* Los `paths` se mostrarán en la lista seleccionada.
* Los ejemplos `curl` estarán disponibles en su campo correspondiente al seleccionar path + error.

¿Quieres que también recargue el último OpenAPI usado si estaba guardado en ese JSON?
