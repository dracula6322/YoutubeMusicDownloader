package com.green.square.youtubedownloader;

import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.CommandArgumentsResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.util.Pair;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.util.TextUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

public class YoutubeDownloader {

  public static void main(String... args) {
    getMusic(args);
  }

  public static void getMusic(String[] args) {

    String outFolder = "C:\\youtubeNew\\";
    String pathToYoutubedl = "C:\\Users\\Andrey\\Downloads\\youtube\\youtube-dl.exe";
    String linkId = "https://www.youtube.com/watch?v=xULTMMgwLuo&t=1784s";

    CommandArgumentsResult defaultArguments = new CommandArgumentsResult(pathToYoutubedl, outFolder, linkId);
    CommandArgumentsResult arguments = YoutubeDownloaderAndCutter.getInstance().parsingArguments(args, defaultArguments);

    System.out.println("arguments = " + arguments);

    outFolder = arguments.outputFolderPath;
    pathToYoutubedl = arguments.pathToYoutubedl;
    linkId = arguments.linkId;

    List<String> links = new ArrayList<>();
    links.add("https://www.youtube.com/watch?v=VnQ52zzyWMY");
//    links.add("https://www.youtube.com/watch?v=ffLbdhP0auc&t=1535s");
//    links.add("https://www.youtube.com/watch?v=Q7tIqEgRwJY");
//    links.add("https://www.youtube.com/watch?v=dljzZqD3RnU");

    YoutubeDownloaderAndCutter.getInstance().downloadAndCutMusic(pathToYoutubedl, outFolder, links);

  }



}
