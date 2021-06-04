package client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static client.Controller.*;
import static command.Commands.*;

public class LeftPanelController implements Initializable {
    public static String root = ".";
    private Network network;
    @FXML
    TableView<FileInfo> filesTable;

    @FXML
    ComboBox<String> disksBox;

    @FXML
    TextField pathField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(120);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);

        if (disksBox != null) {
            disksBox.getItems().clear();
            for (Path p : FileSystems.getDefault().getRootDirectories()) {
                disksBox.getItems().add(p.toString());
            }
            disksBox.getSelectionModel().select(0);
        }

        updateList(Paths.get(root));
    }

    public void updateDisksBox() {
        disksBox.getItems().add("ser:");
        disksBox.getSelectionModel().select(disksBox.getItems().size() - 1);
    }

    public void updateList(Path path) {
        try {
            String correctPath = getCorrectPath(path);
            pathField.setText(correctPath);
            filesTable.getItems().clear();
            if (!correctPath.startsWith(CLOUD)) {
                filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            } else {
                filesTable.getItems().addAll(fileInfoList);
            }
            filesTable.sort();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void updateList(Path path, List<FileInfo> fileInfoList) {
        String correctPath = getCorrectPath(path);
        pathField.setText(correctPath);
        filesTable.getItems().clear();
        filesTable.getItems().addAll(fileInfoList);
        filesTable.sort();
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (upperPath != null && !upperPath.startsWith(CLOUD)) {
            updateList(upperPath);
        } else if (pathField.getText().equals(Paths.get(CLOUD, userName).toString())) {
            return;
        } else {
            if (network != null) {
                network.sendMessage(CD + UP + pathField.getText());
                while (fileInfoList.isEmpty()) {
                    network.setOnMessageReceivedAnswer(arg -> {
                        while ((fileInfoList = (List<FileInfo>) arg[0]).isEmpty()) {
                            fileInfoList = (List<FileInfo>) arg[0];
                        }
                    });
                }
                updateList(Paths.get(getCorrectPath(upperPath)), fileInfoList);
                fileInfoList.clear();
            }
        }
    }

    private String getCorrectPath(Path path) {
        if (path.equals(Paths.get(root)) || !(path.toString().startsWith(CLOUD))) {
            return path.normalize().toAbsolutePath().toString();
        } else return path.toString();
    }

    public String getSelectedFilename() {
        if (!filesTable.isFocused()) {
            return null;
        }
        return filesTable.getSelectionModel().getSelectedItem().getFilename();
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        if (!element.getSelectionModel().getSelectedItem().equals("ser:")) {
            updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
        } else {
            updateList(Paths.get(CLOUD, userName), fileInfoList);
        }
    }

    public void setNetwork(Network network) {
        this.network = network;
    }
}
