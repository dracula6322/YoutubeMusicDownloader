package com.green.square;

import com.green.square.youtubedownloader.CommandArgumentsResult;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("/admin")
public class AdminView extends VerticalLayout {

  CommandArgumentsResult commandArgumentsResult;
//  pathToYoutubedl
//      outputFolderPath
//  linkId
//      ffmpegPath


  TextField titleField = new TextField();

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);


  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
  }

  private void getPath() {
    TextField pathToYoutubedlTextField = new TextField();
    pathToYoutubedlTextField.setLabel("pathToYoutubedlTextField");
    pathToYoutubedlTextField.setPlaceholder("pathToYoutubedlTextField");
  }

}