package com.green.square.youtubedownloader;

import com.green.square.model.CommandArgumentsResult;
import com.green.square.model.DownloadState;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class YoutubeDownloaderMain {

  @Autowired
  ProgramArgumentsController programArgumentsController;

  @Autowired
  YoutubeDownloaderAndCutter youtubeDownloaderAndCutter;

  public static void main(String... args) {
    new YoutubeDownloaderMain().getMusic(args);
  }

  public void getMusic(String[] args) {

    Logger logger = LoggerFactory.getLogger(YoutubeDownloaderMain.class);

    CommandArgumentsResult arguments = programArgumentsController.setArgumentsWithValue(args, logger);

    String outFolder = arguments.outputFolderPath;
    String pathToYoutubedl = arguments.pathToYoutubedl;
    String linkId = arguments.linkId;
    String ffmpegPath = arguments.ffmpegPath;

    logger.debug("pathToYoutubedl = " + pathToYoutubedl);
    logger.debug("outFolder = " + outFolder);
    logger.debug("linkId = " + linkId);
    logger.debug("ffmpegPath = " + ffmpegPath);

    List<String> links = new ArrayList<>();
    links.add(linkId);

    @NonNull Single<DownloadState> state = youtubeDownloaderAndCutter.getPairs(pathToYoutubedl, linkId, logger);

    state.doOnSuccess(new Consumer<DownloadState>() {
      @Override
      public void accept(DownloadState downloadState) throws Throwable {
        logger.info("downloadState = " + downloadState);
      }
    }).doOnError(new Consumer<Throwable>() {
      @Override
      public void accept(Throwable throwable) throws Throwable {
        logger.error(throwable.getMessage());
      }
    }).map(new Function<DownloadState, DownloadState>() {
      @Override
      public DownloadState apply(DownloadState downloadState) throws Throwable {

        String createdFolderPath = youtubeDownloaderAndCutter
            .createFolder(arguments.getOutputFolderPath(), downloadState.getVideoId(),
                downloadState.getAudioFileName(), logger);

        return downloadState.toBuilder().createdFolderPath(createdFolderPath).build();
      }
    }).map(new Function<DownloadState, DownloadState>() {
      @Override
      public DownloadState apply(DownloadState downloadState) throws Throwable {

        File downloadedVideoFilePath = youtubeDownloaderAndCutter
            .downloadVideo(logger, pathToYoutubedl, downloadState.getAudioFileName(),
                downloadState.getCreatedFolderPath(), downloadState.getVideoId());

        if (downloadedVideoFilePath == null) {
          throw new NullPointerException();
        }

        return downloadState.toBuilder().downloadedAudioFilePath(downloadedVideoFilePath.getAbsolutePath()).build();
      }
    }).doOnSuccess(new Consumer<DownloadState>() {
      @Override
      public void accept(DownloadState downloadState) throws Throwable {
        logger.info("downloadState.getDownloadedAudioFilePath() = " + downloadState.getDownloadedAudioFilePath());

        List<File> files = youtubeDownloaderAndCutter
            .cutTheFileIntoPieces(downloadState.getDownloadedAudioFilePath(), downloadState.getPairs(), logger,
                arguments, downloadState.getCreatedFolderPath(), downloadState.getDurationInSeconds(), "mp3");

        logger.info("files = " + files.toString());
      }
    }).subscribe();
  }


}
