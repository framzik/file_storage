package client;

import io.netty.channel.ChannelHandlerContext;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

import static command.Commands.*;


public class Controller implements Initializable {
    @FXML
    public TextField dirName;
    @FXML
    public Button btnCreateDir;
    @FXML
    public Button btnRemove;
    @FXML
    VBox leftPanel, rightPanel;
    @FXML
    public Button btnConnectCloud;


    private LeftPanelController leftPC = null;
    private RightPanelController rightPC = null;
    private Network network;
    public static byte[] fromFile;
    public static List<FileInfo> fileInfoList;
    public static String userName = "framzik";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        leftPC = (LeftPanelController) leftPanel.getProperties().get("ctrl");
        rightPC = (RightPanelController) rightPanel.getProperties().get("ctrl");
        btnCreateDir.setVisible(false);
        dirName.setVisible(false);

        leftPC.filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path path = Paths.get(leftPC.pathField.getText()).resolve(leftPC.filesTable.getSelectionModel().getSelectedItem().getFilename());
                    if (!Paths.get(leftPC.pathField.getText()).startsWith(CLOUD)) {
                        if (Files.isDirectory(path)) {
                            leftPC.updateList(path);
                        }
                    } else if (Files.isDirectory(path)) {
                        network.sendMessage(CD + leftPC.pathField.getText() + " " + leftPC.filesTable.getSelectionModel().getSelectedItem().getFilename());
                        fillingFileInfoList();
                        leftPC.updateList(Paths.get(leftPC.pathField.getText(), leftPC.filesTable.getSelectionModel().getSelectedItem().getFilename()), fileInfoList);
                        fileInfoList.clear();
                    }
                }
            }
        });
    }

    public void copyBtnAction(ActionEvent actionEvent) {

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        Path srcPath = null, dstPath = null;

        if (leftPC.getSelectedFilename() != null) {
            srcPath = Paths.get(leftPC.pathField.getText(), leftPC.getSelectedFilename());
            dstPath = Paths.get(rightPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
            if (!leftPC.pathField.getText().startsWith(CLOUD)) {
                try {
                    Files.copy(srcPath, dstPath);
                    rightPC.updateList(Paths.get(rightPC.getCurrentPath()));
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
                    alert.showAndWait();
                }
            } else {
                network.sendMessage(DOWNLOAD + srcPath);
                fillingFileInfoList();
                if (Files.exists(dstPath)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать, файл с таким именем уже существует", ButtonType.OK);
                    alert.showAndWait();
                    fromFile = " ".getBytes(StandardCharsets.UTF_8);
                    fileInfoList.clear();
                } else {
                    try {
                        Files.write(dstPath, fromFile);
                        rightPC.updateList(Paths.get(rightPC.getCurrentPath()));
                    } catch (IOException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Что-то пошло не так с загрузкой файла. Повторите попытку.", ButtonType.OK);
                        alert.showAndWait();
                    } finally {
                        fromFile = " ".getBytes(StandardCharsets.UTF_8);
                        fileInfoList.clear();
                    }
                }
            }
        }
        if (rightPC.getSelectedFilename() != null) {
            srcPath = Paths.get(rightPC.getCurrentPath(), rightPC.getSelectedFilename());
            dstPath = Paths.get(leftPC.pathField.getText()).resolve(srcPath.getFileName().toString());
            if (!leftPC.pathField.getText().startsWith(CLOUD)) {
                try {
                    Files.copy(srcPath, dstPath);
                    leftPC.updateList(Paths.get(leftPC.pathField.getText()));
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
                    alert.showAndWait();
                }
            } else {
                try {
                    sendFile(srcPath, dstPath);
                    fillingFileInfoList();
                    leftPC.updateList(Paths.get(leftPC.pathField.getText()));
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось отправить на сервер указанный файл", ButtonType.OK);
                    alert.showAndWait();
                }
            }
        }
    }

    /**
     * Передаем байтовое представление файла  и отдельно команду о завершении передачи.
     *
     * @param srcFile
     * @throws IOException
     */
    private void sendFile(Path srcFile, Path dstPath) throws IOException {
        byte[] readFileBytes = Files.readAllBytes(srcFile);
        byte[] uploadByte = UPLOAD.getBytes(StandardCharsets.UTF_8);
        byte[] msg = new byte[readFileBytes.length + uploadByte.length];

        System.arraycopy(uploadByte, 0, msg, 0, uploadByte.length);
        System.arraycopy(readFileBytes, 0, msg, uploadByte.length, readFileBytes.length);
        network.sendMessage(msg);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        network.sendMessage((END_FILE + dstPath).getBytes(StandardCharsets.UTF_8));
    }

    public void btnExitAction(ActionEvent actionEvent) {
        if (network != null) {
            network.sendMessage(END);
        }
        Platform.exit();
    }

    /**
     * Подключаемся к серверу
     */
    public void btnCloudConnect(ActionEvent actionEvent) {
        network = new Network(arg -> {
            while ((fileInfoList = (List<FileInfo>) arg[0]).isEmpty()) {
                fileInfoList = (List<FileInfo>) arg[0];
            }
        });
        while (fileInfoList == null) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        leftPC.setNetwork(network);
        leftPC.updateDisksBox();
        btnConnectCloud.setVisible(false);
        btnConnectCloud.setMaxWidth(0);
        btnCreateDir.setVisible(true);
        dirName.setVisible(true);
        fileInfoList.clear();
    }

    /**
     * Создаем новую папку на сервере
     *
     * @param actionEvent
     */
    public void btnCreateDir(ActionEvent actionEvent) {
        if (dirName.getText().trim().length() == 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Введите имя", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        network.sendMessage(TOUCH + leftPC.pathField.getText() + " " + dirName.getText());
        dirName.setText("");
        fillingFileInfoList();
        leftPC.updateList(Paths.get(leftPC.pathField.getText()), fileInfoList);
        fileInfoList.clear();
    }

    public void btnRemove(ActionEvent actionEvent) {
        if (leftPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран на сервере", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        network.sendMessage(REMOVE + leftPC.pathField.getText() + " " + leftPC.getSelectedFilename());
        fillingFileInfoList();
        leftPC.updateList(Paths.get(leftPC.pathField.getText()), fileInfoList);
        fileInfoList.clear();
    }

    /**
     * Останавливаем поток, и ждем пока не обработается информация с сервера = заполнится @ fileInfoList
     */
    private void fillingFileInfoList() {
        while (fileInfoList.isEmpty()) {
            network.setOnMessageReceivedAnswer(arg -> {
                while ((fileInfoList = (List<FileInfo>) arg[0]).isEmpty()) {
                    fileInfoList = (List<FileInfo>) arg[0];
                }
            });
        }
    }
}