package com.green.square.youtubedownloader;

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
    logger.error("outFolder = " + outFolder);
    logger.debug("linkId = " + linkId);
    logger.debug("ffmpegPath = " + ffmpegPath);

    List<String> links = new ArrayList<>();
    links.add(linkId);

    youtubeDownloaderAndCutter
        .downloadAndCutMusicRxJavaStyleWithBuilder(pathToYoutubedl, outFolder, links, ffmpegPath, logger);
  }


}
