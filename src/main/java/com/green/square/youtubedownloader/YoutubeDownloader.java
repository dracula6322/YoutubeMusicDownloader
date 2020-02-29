package com.green.square.youtubedownloader;

import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.CommandArgumentsResult;
import org.apache.commons.lang3.SystemUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            ffmpegPath = "/usr/bin/ffmpeg ";
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
        links.add("https://www.youtube.com/watch?v=ffLbdhP0auc&t=1535s");
        links.add("https://www.youtube.com/watch?v=Q7tIqEgRwJY");
        links.add("https://www.youtube.com/watch?v=dljzZqD3RnU");

        YoutubeDownloaderAndCutter.getInstance().downloadAndCutMusic(pathToYoutubedl, outFolder, links, ffmpegPath);


//        ExecutorService inputThread = Executors.newSingleThreadExecutor();
//        ExecutorService errorThread = Executors.newSingleThreadExecutor();
//
//        String[] command = {"bash", "-c", "ffmpeg  -loglevel debug  -y  -i /home/andrey/youtubeNew/VnQ52zzyWMY/original_VnQ52zzyWMY.webm  -ss 00:04:05  -to 00:04:59   /home/andrey/\"поглаживание мягкими рукавчиками.mp4\""};
//        String env = "FILE_NAME=Sdamp4";
//
//        ArrayList<List<String>> result = YoutubeDownloaderAndCutter.getInstance().executeFunctionAndGetStringOutputSyncWithRootDirWithEnv(command, "/", new String[]{env}, inputThread, errorThread);
//        System.out.println("result = " + result);
//
//
//        inputThread.shutdown();
//        errorThread.shutdown();
    }

}
