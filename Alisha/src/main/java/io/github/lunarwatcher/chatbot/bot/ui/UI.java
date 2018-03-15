package io.github.lunarwatcher.chatbot.bot.ui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import io.github.lunarwatcher.chatbot.Database;
import io.github.lunarwatcher.chatbot.bot.Bot;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;


public class UI extends Application {

    public static Properties botProps;
    public static Database database;
    public static Bot bot;
    public UIController uiController;
    public UI(){

    }

    public UI(String[] args){
        launch(args);
    }
    @Override
    public void start(Stage stage) throws Exception {

        JFXListView<String> sites = new JFXListView<>();

        for(Chat c : bot.getChats()){
            sites.getItems().add(c.getName());
        }

        AnchorPane root = null;
        uiController = new UIController();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));

        loader.setController(uiController);
        root = loader.load();

        Scene scene = new Scene(root, 600, 600, javafx.scene.paint.Color.WHITE);
        stage.setTitle("Alisha");
        stage.setScene(scene);
        stage.setResizable(false);

        stage.setOnCloseRequest(event -> System.exit(0));
        stage.show();

    }
}
