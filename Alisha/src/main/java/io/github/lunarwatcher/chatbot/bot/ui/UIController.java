package io.github.lunarwatcher.chatbot.bot.ui;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import io.datafx.controller.ViewController;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

import javax.annotation.PostConstruct;

@ViewController(value="main.fxml", title="Alisha")
public class UIController {
    /*
    <JFXButton fx:id="start" layoutX="386.0" layoutY="269.0" mnemonicParsing="false" text="Start" />
      <JFXListView fx:id="sites" layoutX="386.0" layoutY="60.0" prefHeight="200.0" prefWidth="200.0" />
      <JFXButton fx:id="terminate" layoutX="450.0" layoutY="269.0" mnemonicParsing="false" text="Terminate " />
      <JFXTextArea fx:id="logs" layoutX="14.0" layoutY="60.0" prefHeight="200.0" prefWidth="200.0" />
      <JFXButton fx:id="shutdown" layoutX="386.0" layoutY="309.0" mnemonicParsing="false" text="Global shutdown" />
     */
    @FXML
    JFXTextArea logs;
    @FXML
    JFXListView<String> sites;
    @FXML
    JFXButton start;
    @FXML
    JFXButton terminate;
    @FXML
    JFXButton shutdown;
    @FXML
    AnchorPane root;


    public UIController() {
    }

    @PostConstruct
    public void init() throws Exception{

        sites.getItems().add("Item");

        terminate.setOnTouchPressed(e -> {

        });

        start.setOnTouchPressed(e -> {

        });

        shutdown.setOnTouchPressed(e ->{
            System.exit(0);
        });
    }

}
