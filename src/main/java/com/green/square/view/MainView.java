package com.green.square.view;

import com.green.square.DownloadStateRepository;
import com.green.square.FileManagerController;
import com.green.square.ZipController;
import com.green.square.model.CommandArgumentsResult;
import com.green.square.model.CutValue;
import com.green.square.model.DownloadState;
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
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Route
public class MainView extends VerticalLayout {

  Grid<CutValue> grid = new Grid<>();
  Button cutFile = new Button("Cut chosen file as zip");
  Logger logger = LoggerFactory.getLogger(YoutubeDownloaderMain.class);
  CommandArgumentsResult arguments = CommandArgumentsResult.builder().build();
  DownloadState currentDownloadState = DownloadState.builder().build();

  @Autowired
  ZipController zipController;

  @Autowired
  DownloadStateRepository downloadStateRepository;

  @Autowired
  YoutubeDownloaderAndCutter youtubeDownloaderAndCutter;

  @Autowired
  ProgramArgumentsController programArgumentsController;

  @Autowired
  FileManagerController fileManagerController;

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
    getChapters.addClickListener((ComponentEventListener<ClickEvent<Button>>) event -> {

      if (youtubeLink.isEmpty()) {
        Notification.show("Must enter url");
        getChapters.setEnabled(true);
        return;
      }

      String url = youtubeLink.getValue();
      arguments = arguments.toBuilder().linkId(url).build();

      //@NonNull insane annotation that ruins the assembly
      Single<DownloadState> rxSinglePairs = youtubeDownloaderAndCutter
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

          Grid<CutValue> newGrid = getGridView(downloadState.getPairs());
          remove(cutFile);
          remove(grid);
          add(newGrid);
          add(cutFile);
          grid = newGrid;
        }

        @Override
        public void onError(@NonNull Throwable e) {
          logger.error("getChapters onError");
          logger.error(e.getMessage(), e);
          e.printStackTrace();
        }
      });

      getChapters.setEnabled(true);
    });

    cutFile.addClickListener((ComponentEventListener<ClickEvent<Button>>) event -> {

      ArrayList<CutValue> selectedItems = new ArrayList<>(grid.getSelectedItems());

      List<File> cutFiles = downloadAndCutFile(selectedItems);
      if (cutFiles == null || cutFiles.isEmpty()) {
        logger.info("cutFiles.isEmpty() = ");
        return;
      }

      String saveFolder = cutFiles.get(0).getParent();
      logger.info("saveFolder = " + saveFolder);
      File zipFile = zipController
          .convertFilesToZip(cutFiles, saveFolder, "tmpZipFile" + UUID.randomUUID().toString() + ".zip", logger);
      logger.info("zipFile.getAbsolutePath() = " + zipFile.getAbsolutePath());
      ArrayList<File> filesArrayList = new ArrayList<>();
      filesArrayList.add(zipFile);
      downloadFilesFromCurrentPage(filesArrayList);
    });

    add(getChapters);
    add(grid);
    add(cutFile);
  }

  private List<File> downloadAndCutFile(List<CutValue> selectedItems) {

    String createdFolderPath = youtubeDownloaderAndCutter
        .createFolder(arguments.getOutputFolderPath(), currentDownloadState.getVideoId(),
            currentDownloadState.getAudioFileName(), logger);
    currentDownloadState = currentDownloadState.toBuilder().createdFolderPath(createdFolderPath).build();

    File downloadedVideoFilePath = youtubeDownloaderAndCutter
        .downloadVideo(logger, arguments.getPathToYoutubedl(), currentDownloadState.getAudioFileName(),
            currentDownloadState.getCreatedFolderPath(), currentDownloadState.getVideoId());
    currentDownloadState = currentDownloadState.toBuilder()
        .downloadedAudioFilePath(downloadedVideoFilePath.getAbsolutePath()).build();

    return youtubeDownloaderAndCutter
        .cutTheFileIntoPieces(downloadedVideoFilePath.getAbsolutePath(), selectedItems, logger,
            arguments, currentDownloadState.getCreatedFolderPath(), currentDownloadState.getDurationInSeconds(), "mp3");
  }

  public Grid<CutValue> getGridView(ArrayList<CutValue> values) {

    Grid<CutValue> grid = new Grid<>(CutValue.class);
    grid.setSelectionMode(SelectionMode.MULTI);
    grid.setItems(values);
    grid.removeAllColumns();

    grid.addColumn(new TextRenderer<>(CutValue::getTitle))
        .setHeader("Name")
        .setResizable(true);

    grid.addColumn(new TextRenderer<>(cutValue -> (cutValue.getStartTime() + " - " + cutValue.getEndTime())))
        .setHeader("Time")
        .setResizable(true);

    grid.addComponentColumn((ValueProvider<CutValue, Component>) cutValue -> {
      Button button = new Button(new Icon(VaadinIcon.DOWNLOAD));
      button.addClickListener((ComponentEventListener<ClickEvent<Button>>) event -> {
        List<CutValue> selectedItems = new ArrayList<>(grid.getSelectedItems());
        selectedItems.add(cutValue);
        List<File> cutFiles = downloadAndCutFile(selectedItems);
        downloadFilesFromCurrentPage(cutFiles);
      });
      return button;
    });

    return grid;
  }


  private void downloadFilesFromCurrentPage(List<File> cutFiles) {
    Optional<UI> ui = getUI();
    for (File file : cutFiles) {
      String fileName = file.getName();
      ui.ifPresent(value -> openPageDownloadFile(fileName, value, logger, currentDownloadState.getVideoId()));
    }
  }

  private void openPageDownloadFile(String fileName, UI ui, Logger logger, String id) {

    if (StringUtils.isEmpty(fileName)) {
      logger.error("Files is empty");
      return;
    }

    if (ui == null) {
      logger.error("Something wrong with ui");
      return;
    }
    String hashName = fileManagerController.getHashNameFile(fileName);
    ui.getPage().open(String.format("/files/%s/%s", id, hashName));
  }
}
