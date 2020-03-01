package com.green.square.youtubedownloader;

import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.CommandArgumentsResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.SystemUtils;

public class YoutubeDownloader {

  public static void main(String... args) {
    getMusic(args);
  }

  public static void getMusic(String[] args) {

    String outFolder;
    String pathToYoutubedl;
    String linkId;
    String ffmpegPath;

    if (SystemUtils.IS_OS_LINUX) {
      outFolder = "/home/andrey/youtubeNew/";
      pathToYoutubedl = "/usr/local/bin/youtube-dl";
      linkId = "https://www.youtube.com/watch?v=xULTMMgwLuo&t=1784s";
      ffmpegPath = "/usr/bin/ffmpeg";
    } else {
      outFolder = "C:\\youtubeNew\\";
      pathToYoutubedl = "C:\\Users\\Andrey\\Downloads\\youtube\\youtube-dl.exe";
      linkId = "https://www.youtube.com/watch?v=xULTMMgwLuo&t=1784s";
      ffmpegPath = "E:\\Programs\\ffmpeg\\bin\\ffmpeg.exe";

    }

    CommandArgumentsResult defaultArguments = new CommandArgumentsResult(pathToYoutubedl, outFolder, linkId);
    CommandArgumentsResult arguments = YoutubeDownloaderAndCutter.getInstance()
        .parsingArguments(args, defaultArguments);

    System.out.println("arguments = " + arguments);

    outFolder = arguments.outputFolderPath;
    pathToYoutubedl = arguments.pathToYoutubedl;
    linkId = arguments.linkId;

    List<String> links = new ArrayList<>();
    links.add("https://www.youtube.com/watch?v=VnQ52zzyWMY");
//    links.add("https://www.youtube.com/watch?v=ffLbdhP0auc&t=1535s");
//    links.add("https://www.youtube.com/watch?v=Q7tIqEgRwJY");
//    links.add("https://www.youtube.com/watch?v=dljzZqD3RnU");

      YoutubeDownloaderAndCutter.getInstance().downloadAndCutMusic(pathToYoutubedl, outFolder, links, ffmpegPath);

//    ExecutorService inputThread = Executors.newSingleThreadExecutor();
//    ExecutorService errorThread = Executors.newSingleThreadExecutor();
//    String command = "/usr/local/bin/youtube-dl --get-id https://www.youtube.com/watch?v=ffLbdhP0auc&t=1535s & wait";

//    String[] command = {"/usr/local/bin/youtube-dl",
//        "--get-id","https://www.youtube.com/watch?v=VnQ52zzyWMY", "& wait"};
//
//    ArrayList<String> commandArray = new ArrayList<>();
//    commandArray.add("/usr/local/bin/youtube-dl");
//    commandArray.add("--get-id");
//    commandArray.add("https://www.youtube.com/watch?v=VnQ52zzyWMY");
//
//
//    ArrayList<List<String>> result = YoutubeDownloaderAndCutter.getInstance()
//        .executeFunctionAndGetStringOutput(commandArray.toArray(new String[0]), "", new String[]{}, inputThread, errorThread);
//    System.out.println("result = " + result);
//
//    inputThread.shutdown();
//    errorThread.shutdown();
  }

}
