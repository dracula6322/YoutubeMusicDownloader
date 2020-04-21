package com.green.square;

import static com.green.square.youtubedownloader.YoutubeDownloader.getDefaultArguments;
import static com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.cutFileByCutValueVersion2;
import static com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.deleteAndCreateFolder;
import static com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.downloadFile;
import static com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.downloadJsonInMemory;
import static com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.getAudioFileNameFromJsonData;
import static com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.getIdFromLink;
import static com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.getPairsVersion2;
import static com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.getTimeFromJson;
import static com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.makeGoodString;

import com.green.square.youtubedownloader.CommandArgumentsResult;
import com.green.square.youtubedownloader.YoutubeDownloader;
import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route
public class MainView extends VerticalLayout {

  Grid<CutValue> grid = new Grid<>();
  Button cutFile = new Button("Cut chosen file");
  String idVideo = "";
  String jsonDataGlobal = "";

  ExecutorService inputThread;
  ExecutorService errorThread;
  Logger logger = LoggerFactory.getLogger(YoutubeDownloader.class);

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    errorThread = Executors.newSingleThreadExecutor();
    inputThread = Executors.newSingleThreadExecutor();
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    inputThread.shutdown();
    errorThread.shutdown();
  }

  public MainView() {

    String videoHintLint = "https://www.youtube.com/watch?v=m81koYhgc5o&t=215s";
    TextField youtubeLink = new TextField();
    youtubeLink.setValue(videoHintLint);
    youtubeLink.setWidthFull();
    add(youtubeLink);

    Button getChapters = new Button("GetChapters");
    getChapters.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
      @Override
      public void onComponentEvent(ClickEvent<Button> event) {
        String url = youtubeLink.getValue();

        String[] args = new String[]{"--" + YoutubeDownloaderAndCutter.linkIdOptionsName, url};

        CommandArgumentsResult arguments = getDefaultArguments(args, logger);
        String id = getIdFromLink(arguments.pathToYoutubedl, url, inputThread, errorThread, logger);
        idVideo = id;

        String jsonData = downloadJsonInMemory(arguments.pathToYoutubedl, id, inputThread, errorThread, logger);
        logger.info("jsonData = " + jsonData);
        jsonDataGlobal = jsonData;
        String audioFileName = getAudioFileNameFromJsonData(jsonData);
        String duration = getTimeFromJson(jsonData);
        ArrayList<CutValue> pairs = getPairsVersion2(id, jsonData, audioFileName, duration);

        Grid<CutValue> newGrid = getGridView(pairs);
        remove(cutFile);
        remove(grid);
        add(newGrid);
        add(cutFile);
        grid = newGrid;
      }
    });

    cutFile.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
      @Override
      public void onComponentEvent(ClickEvent<Button> event) {
        Set<CutValue> selectedItemsSet = grid.getSelectedItems();
        if (selectedItemsSet == null || selectedItemsSet.size() == 0) {
          Notification.show("Files not selected");
          return;
        }
        ArrayList<CutValue> selectedItemsArrayList = new ArrayList<>(selectedItemsSet);
        String[] args = new String[]{};
        CommandArgumentsResult arguments = getDefaultArguments(args, logger);

        String audioFileName = getAudioFileNameFromJsonData(jsonDataGlobal);
        logger.info("audioFileName = " + audioFileName);
        audioFileName = makeGoodString(audioFileName);
        logger.info("audioFileName = " + audioFileName);
        String pathToYoutubeFolder = arguments.outputFolderPath + File.separator + idVideo + File.separator;
        logger.info("pathToYoutubeFolder = " + pathToYoutubeFolder);

        Path path = Paths.get(pathToYoutubeFolder + audioFileName);
        logger.info("path = " + path.toString());
        File downloadedAudioFile;
        boolean downloadedFileIsExists = Files.exists(path);
        if (downloadedFileIsExists) {
          logger.debug("File exists and don't need download it");
          downloadedAudioFile = path.toFile();
        } else {
          logger.debug("File not exists");
          File createdFolder = deleteAndCreateFolder(pathToYoutubeFolder, audioFileName,
              logger);

          downloadedAudioFile = downloadFile(arguments.pathToYoutubedl, idVideo, pathToYoutubeFolder, inputThread,
              errorThread, logger, "original_%(id)s.%(ext)s");
          logger.debug("downloadedAudioFile = " + downloadedAudioFile);
        }

        ArrayList<String> files = cutFileByCutValueVersion2(arguments.ffmpegPath, downloadedAudioFile,
            selectedItemsArrayList, inputThread, errorThread, pathToYoutubeFolder, logger);


      }
    });

    add(getChapters);
    add(grid);
    add(cutFile);
  }

  public static Grid<CutValue> getGridView(ArrayList<CutValue> values) {
    Grid<CutValue> grid = new Grid<>(CutValue.class);

    grid.setSelectionMode(SelectionMode.MULTI);
    grid.setItems(values);
    grid.addComponentColumn(new ValueProvider<CutValue, Component>() {
      @Override
      public Component apply(CutValue cutValue) {

        Button button = new Button(new Icon(VaadinIcon.DOWNLOAD));
//        button.addClickListener(click ->
//            Notification.show("Clicked: " + cutValue.toString()));
        return button;
      }
    });

    return grid;
  }

}