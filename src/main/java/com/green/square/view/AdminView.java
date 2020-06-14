package com.green.square.view;

import com.green.square.model.CommandArgumentsResult;
import com.green.square.youtubedownloader.ProgramArgumentsController;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route("admin/")
public class AdminView extends VerticalLayout {

  @Autowired
  ProgramArgumentsController programArgumentsController;

  CommandArgumentsResult arguments;

  public TextField pathToYoutubedlTextField = new TextField();
  public TextField outputFolderPathTextField = new TextField();
  public TextField linkIdTextField = new TextField();
  public TextField ffmpegPathTextField = new TextField();

  public Button checkPathToYoutubedlTextField = new Button("Check");
  public Button checkFolderPathTextField = new Button("Check");
  public Button checkIdTextField = new Button("Check");
  public Button checkFfmpegPathTextField = new Button("Check");

  public Button saveConfigurationButton = new Button("Save");
  public Button resetConfigurationButton = new Button("Reset");
  public Button defaultConfigurationButton = new Button("Default");

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);

    arguments = programArgumentsController.getArguments();

    initView(arguments);

    pathToYoutubedlTextField.setWidthFull();
    outputFolderPathTextField.setWidthFull();
    linkIdTextField.setWidthFull();
    ffmpegPathTextField.setWidthFull();

    saveConfigurationButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
      arguments = arguments.toBuilder()
          .pathToYoutubedl(pathToYoutubedlTextField.getValue())
          .outputFolderPath(outputFolderPathTextField.getValue())
          .linkId(linkIdTextField.getValue())
          .ffmpegPath(ffmpegPathTextField.getValue())
          .build();

      programArgumentsController.setArguments(arguments);

    });

    resetConfigurationButton
        .addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> initView(arguments));

    defaultConfigurationButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
      arguments = programArgumentsController.setArgumentsWithValue(new String[0]);
      initView(arguments);
    });

    add(pathToYoutubedlTextField);
    add(outputFolderPathTextField);
    add(linkIdTextField);
    add(ffmpegPathTextField);
    add(saveConfigurationButton);
    add(resetConfigurationButton);
    add(defaultConfigurationButton);
  }

  public void initView(CommandArgumentsResult arguments) {
    pathToYoutubedlTextField.setValue(arguments.getPathToYoutubedl());
    outputFolderPathTextField.setValue(arguments.getOutputFolderPath());
    linkIdTextField.setValue(arguments.getLinkId());
    ffmpegPathTextField.setValue(arguments.getFfmpegPath());
  }
}