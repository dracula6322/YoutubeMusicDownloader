package com.green.square;

import com.green.square.youtubedownloader.CommandArgumentsResult;
import com.green.square.youtubedownloader.ProgramArgumentsController;
import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter;
import com.green.square.youtubedownloader.YoutubeDownloaderMain;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
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
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Route
public class MainView extends VerticalLayout {

  Grid<CutValue> grid = new Grid<>();
  Button cutFile = new Button("Cut chosen file as zip");
  ExecutorService inputThread = Executors.newFixedThreadPool(2);
  ExecutorService errorThread = Executors.newFixedThreadPool(2);
  Logger logger = LoggerFactory.getLogger(YoutubeDownloaderMain.class);
  CommandArgumentsResult arguments = CommandArgumentsResult.builder().build();
  DownloadState currentDownloadState = DownloadState.builder().build();

  @Autowired
  DownloadStateRepository downloadStateRepository;

  @Autowired
  YoutubeDownloaderAndCutter youtubeDownloaderAndCutter;

  @Autowired
  ProgramArgumentsController programArgumentsController;

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    arguments = programArgumentsController.getArguments();
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
  }

  public MainView() {

    logger.info("MainView construction");

    String videoHintLint = "https://www.youtube.com/watch?v=m81koYhgc5o&t=215s";
    TextField youtubeLink = new TextField();
    youtubeLink.setValue(videoHintLint);
    youtubeLink.setWidthFull();
    add(youtubeLink);

    Button getChapters = new Button("GetChapters");
    getChapters.setDisableOnClick(true);
    getChapters.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
      @Override
      public void onComponentEvent(ClickEvent<Button> event) {

        if (youtubeLink.isEmpty()) {
          Notification.show("Must enter url");
          getChapters.setEnabled(true);
          return;
        }

        String url = youtubeLink.getValue();
        arguments = arguments.toBuilder().linkId(url).build();

        @NonNull Single<DownloadState> rxSinglePairs = youtubeDownloaderAndCutter
            .getPairs(arguments.pathToYoutubedl, arguments.getLinkId(), logger);

        rxSinglePairs.subscribe(new SingleObserver<DownloadState>() {
          @Override
          public void onSubscribe(@NonNull Disposable d) {
            logger.info("onSubscribe");
            logger.info(d.toString());
          }

          @Override
          public void onSuccess(@NonNull DownloadState downloadState) {

            logger.info("onSuccess");
            logger.info(downloadState.toString());

            downloadStateRepository.save(downloadState);

            currentDownloadState = downloadState;

            Grid<CutValue> newGrid = getGridView(downloadState.getPairs(), downloadState.getVideoId());
            remove(cutFile);
            remove(grid);
            add(newGrid);
            add(cutFile);
            grid = newGrid;

          }

          @Override
          public void onError(@NonNull Throwable e) {
            logger.error("getChapters onError");
            logger.error(e.getMessage());
            e.printStackTrace();
          }
        });

        getChapters.setEnabled(true);
      }
    });

    cutFile.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
      @Override
      public void onComponentEvent(ClickEvent<Button> event) {

        ArrayList<CutValue> selectedItems = new ArrayList<>(grid.getSelectedItems());

//        ArrayList<File> cutFiles = youtubeDownloaderAndCutter
//            .downloadMultipleCut(selectedItems, logger, arguments,
//                currentDownloadState.getAudioFileName(), currentDownloadState.getCreatedFolderPath(),
//                currentDownloadState.getVideoId(), currentDownloadState.getDuration());

        File downloadedVideoFilePath = youtubeDownloaderAndCutter
            .downloadVideo(logger, arguments.getPathToYoutubedl(), currentDownloadState.getAudioFileName(),
                currentDownloadState.getCreatedFolderPath(),
                currentDownloadState.getVideoId());

        ArrayList<File> cutFiles = youtubeDownloaderAndCutter
            .cutTheFileIntoPieces(downloadedVideoFilePath.getAbsolutePath(), selectedItems, logger,
                arguments, currentDownloadState.getCreatedFolderPath(), currentDownloadState.getDurationInSeconds());

        if (cutFiles == null || cutFiles.isEmpty()) {
          System.out.println("cutFiles.isEmpty() = ");
          return;
        }
        try {
          String saveFolder = cutFiles.get(0).getParent();
          System.out.println("saveFolder = " + saveFolder);
          File zipFile = GreetingController
              .zip(cutFiles, saveFolder, "tmpZipFile" + UUID.randomUUID().toString() + ".zip");
          System.out.println("zipFile.getAbsolutePath() = " + zipFile.getAbsolutePath());
          ArrayList<File> filesArrayList = new ArrayList<>();
          filesArrayList.add(zipFile);
          startDownloadFile(filesArrayList, getUI(), logger, currentDownloadState.getVideoId());
        } catch (IOException e) {
          e.printStackTrace();
          logger.error(e.getMessage());
        }


      }
    });

    add(getChapters);
    add(grid);
    add(cutFile);
  }


  public Grid<CutValue> getGridView(ArrayList<CutValue> values, String id) {
    Grid<CutValue> grid = new Grid<>(CutValue.class);

    ArrayList<CutValue> newArray = new ArrayList<>();

    for (CutValue value : values) {
      //String hashName = GreetingController.getHashNameFile(value.title);
      //value = value.toBuilder().hashName(hashName).build();
      newArray.add(value);
    }
    values = newArray;

    grid.setSelectionMode(SelectionMode.MULTI);
    grid.setItems(values);
    grid.addComponentColumn(new ValueProvider<CutValue, Component>() {
      @Override
      public Component apply(CutValue cutValue) {
        Button button = new Button(new Icon(VaadinIcon.DOWNLOAD));
        button.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
          @Override
          public void onComponentEvent(ClickEvent<Button> event) {

            ArrayList<CutValue> selectedItems = new ArrayList<>(grid.getSelectedItems());
            selectedItems.add(cutValue);

            File downloadedVideoFilePath = youtubeDownloaderAndCutter
                .downloadVideo(logger, arguments.getPathToYoutubedl(), currentDownloadState.getAudioFileName(),
                    currentDownloadState.getCreatedFolderPath(),
                    currentDownloadState.getVideoId());

            ArrayList<File> cutFiles = youtubeDownloaderAndCutter
                .cutTheFileIntoPieces(downloadedVideoFilePath.getAbsolutePath(), selectedItems, logger,
                    arguments, currentDownloadState.getCreatedFolderPath(),
                    currentDownloadState.getDurationInSeconds());

            startDownloadFile(cutFiles, getUI(), logger, currentDownloadState.getVideoId());
          }
        });
        return button;
      }
    });

    return grid;
  }

  public static void startDownloadFile(ArrayList<File> files, Optional<UI> ui, Logger logger, String id) {

    if (files.size() == 1) {
      if (ui.isPresent()) {
        String hashName = GreetingController.getHashNameFile(files.get(0).getName());
        ui.get().getPage().open(String.format("http://localhost:8080/files/%s/%s", id, hashName));
      } else {
        logger.info("Something wrong " + files.toString());
      }
    }
  }

}