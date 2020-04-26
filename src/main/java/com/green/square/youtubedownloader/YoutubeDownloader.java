package com.green.square.youtubedownloader;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YoutubeDownloader {

  public static void main(String... args) {
    getMusic(args);
  }

  public static void getMusic(String[] args) {

    Logger logger = LoggerFactory.getLogger(YoutubeDownloader.class);

    CommandArgumentsResult arguments = getDefaultArguments(args, logger);

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

    YoutubeDownloaderAndCutter.getInstance().downloadAndCutMusic(pathToYoutubedl, outFolder, links, ffmpegPath, logger);
  }

  public static CommandArgumentsResult getDefaultArguments(String[] args, Logger logger) {

    String outFolder;
    String pathToYoutubedl;
    String linkId;
    String ffmpegPath;

    if (SystemUtils.IS_OS_LINUX) {
      outFolder = "/home/andrey/youtubeNew/";
      pathToYoutubedl = "/usr/local/bin/youtube-dl";
      ffmpegPath = "/usr/bin/ffmpeg";
    } else {
      outFolder = "C:\\youtubeNew\\";
      pathToYoutubedl = "C:\\Users\\Andrey\\Downloads\\youtube\\youtube-dl.exe";
      ffmpegPath = "E:\\Programs\\ffmpeg\\bin\\ffmpeg.exe";
    }

    linkId = "https://www.youtube.com/watch?v=ignvgjJwzGk";

    CommandArgumentsResult defaultArguments = new CommandArgumentsResult(pathToYoutubedl, outFolder, linkId,
        ffmpegPath);
    return YoutubeDownloaderAndCutter.getInstance().parsingArguments(args, defaultArguments, logger);
  }

}
