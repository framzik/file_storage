package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

import static client.ClientHandler.isRegistered;
import static client.ClientHandler.wrong;
import static command.Commands.*;


public class Controller implements Initializable {
    @FXML
    public TextField dirName;
    @FXML
    public Button btnCreateDir;
    @FXML
    public Button btnRemove;
    @FXML
    public Button btnReg;
    @FXML
    public TextField login;
    @FXML
    public TextField password;
    @FXML
    VBox leftPanel, rightPanel;
    @FXML
    public Button btnConnectCloud;

    private Stage regStage;
    private RegController regController;
    private LeftPanelController leftPC = null;
    private RightPanelController rightPC = null;
    private Network network;
    public static byte[] fromFile;
    public static List<FileInfo> fileInfoList;
    public static String userName = "";

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

    /**
     * Копирование файла с/на сервер
     *
     * @param actionEvent
     */
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

    /**
     * Выход ( закрытие клиента)
     *
     * @param actionEvent
     */
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
        if (network == null) {
            connect();
            fileInfoList.clear();
        }
        network.sendMessage(String.format("%s %s %s", AUTH, login.getText().trim(),
                password.getText().trim()));
        fillingFileInfoList();
        if (!wrong.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, wrong, ButtonType.OK);
            alert.showAndWait();
            fileInfoList.clear();
            wrong = "";
            return;
        }
        userName = login.getText();
        leftPC.setNetwork(network);
        leftPC.updateDisksBox();
        btnConnectCloud.setVisible(false);
        btnConnectCloud.setMaxWidth(0);
        btnReg.setVisible(false);
        btnReg.setMaxWidth(0);
        btnCreateDir.setVisible(true);
        login.setVisible(false);
        login.setMaxSize(0, 0);
        password.setVisible(false);
        password.setMaxSize(0, 0);
        dirName.setVisible(true);
        fileInfoList.clear();
    }

    /**
     * Создание нового подключения( при открытии канала отправляется команда CON )
     */
    private void connect() {
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

    /**
     * удаление файла
     *
     * @param actionEvent
     */
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

    /**
     * Инициализация окна регистрации нового пользователя
     */
    private void initRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();

            regController = fxmlLoader.getController();
            regController.setController(this);

            regStage = new Stage();
            regStage.setTitle("File Cloud registration");
            regStage.setScene(new Scene(root, 250, 100));
            regStage.initStyle(StageStyle.UTILITY);
            regStage.initModality(Modality.APPLICATION_MODAL);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Регистрация нового пользователя
     *
     * @param login
     * @param password
     * @param nickname
     */
    public void registration(String login, String password, String nickname) {
        network.sendMessage(String.format("%s %s %s %s", REG, login, password, nickname));
        fillingFileInfoList();
        if (isRegistered) {
            isRegistered = false;
            regStage.close();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Регистрация пользователя прошла успешно", ButtonType.OK);
            alert.showAndWait();
            btnReg.setVisible(false);
            btnReg.setMaxSize(0, 0);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, wrong, ButtonType.OK);
            wrong = "";
            alert.showAndWait();
        }
    }

    /**
     * Открытие окна регистрации, установка соединения с сервером
     *
     * @param actionEvent
     */
    public void btnReg(ActionEvent actionEvent) {
        if (regStage == null) {
            initRegWindow();
        }
        if (network == null) {
            connect();
        }
        regStage.show();
        fileInfoList.clear();
    }
}