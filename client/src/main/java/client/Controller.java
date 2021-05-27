package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static command.Commands.END;
import static command.Commands.TOUCH;


public class Controller {
    @FXML
    public TextField dirName;
    @FXML
    VBox leftPanel, rightPanel;
    @FXML
    public Button btnConnectCloud;

    LeftPanelController leftPC = null;
    RightPanelController rightPC = null;
    private Network network;
    public static String userName = "framzik";

    public void copyBtnAction(ActionEvent actionEvent) {
        leftPC = (LeftPanelController) leftPanel.getProperties().get("ctrl");
        rightPC = (RightPanelController) rightPanel.getProperties().get("ctrl");
        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        Path srcPath = null, dstPath = null;

        if (leftPC.getSelectedFilename() != null) {
            srcPath = Paths.get(leftPC.getCurrentPath(), leftPC.getSelectedFilename());
            dstPath = Paths.get(rightPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
            try {
                Files.copy(srcPath, dstPath);
                rightPC.updateList(Paths.get(rightPC.getCurrentPath()));
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
                alert.showAndWait();
            }
        }
        if (rightPC.getSelectedFilename() != null) {

            srcPath = Paths.get(rightPC.getCurrentPath(), rightPC.getSelectedFilename());
            dstPath = Paths.get(leftPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
            try {
                Files.copy(srcPath, dstPath);
                leftPC.updateList(Paths.get(leftPC.getCurrentPath()));
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    public void btnExitAction(ActionEvent actionEvent) {
        if (network != null) {
            network.sendMessage(END);
        }
        Platform.exit();
    }

    public void btnCloudConnect(ActionEvent actionEvent) {
        leftPC = (LeftPanelController) leftPanel.getProperties().get("ctrl");
        rightPC = (RightPanelController) rightPanel.getProperties().get("ctrl");
        network = new Network();
        btnConnectCloud.setVisible(false);
        btnConnectCloud.setMaxWidth(0);
        Alert alertCon = new Alert(Alert.AlertType.INFORMATION, "Connected!", ButtonType.CLOSE);
        alertCon.showAndWait();
        leftPC.updateDisksBox();
    }

    public void btnCreateDir(ActionEvent actionEvent) {
        network.sendMessage(TOUCH + leftPC.pathField.getText() + " " + dirName.getText());
        dirName.setText("");
        leftPC.updateList(Paths.get(leftPC.pathField.getText()));

        System.out.println(TOUCH + leftPC.pathField.getText() + " " + dirName.getText());
    }
}