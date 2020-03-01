package com.green.square.youtubedownloader;

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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.util.TextUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

public class YoutubeDownloaderAndCutter {

    private static YoutubeDownloaderAndCutter ourInstance = new YoutubeDownloaderAndCutter();

    public static YoutubeDownloaderAndCutter getInstance() {
        return ourInstance;
    }

    public YoutubeDownloaderAndCutter() {
    }

    public void downloadAndCutMusic(String pathToYoutubedl, String outFolder, List<String> links, String ffmpegPath) {

        ArrayList<String> ids = new ArrayList<>();

        ExecutorService inputThread = Executors.newSingleThreadExecutor();
        ExecutorService errorThread = Executors.newSingleThreadExecutor();

        for (String videoLink : links) {
            String id = getIdFromLink(pathToYoutubedl, videoLink, inputThread, errorThread);
            System.out.println("id = " + id);
            if (TextUtils.isEmpty(id)) {
                throw new NullPointerException();
            }
            ids.add(id);
        }

        for (String id : ids) {
//      String name = getFileName(pathToYoutubedl, id, inputThread, errorThread);
//      System.out.println("name = " + name);
//      if (TextUtils.isEmpty(name)) {
//        throw new NullPointerException();
//      }
            String name = "";

            String jsonData = downloadJsonInMemory(pathToYoutubedl, id, inputThread, errorThread);
            //System.out.println("jsonData = " + jsonData);

            String audioFileName = getAudioFileNameFromJsonData(jsonData);
            System.out.println("audioFileName = " + audioFileName);
            //jsonData = StringEscapeUtils.unescapeJava(audioFileName);
            if (TextUtils.isEmpty(audioFileName)) {
                throw new NullPointerException();
            }

            File createdFolder = deleteAndCreateFolder(outFolder + File.separator + id, audioFileName);
            System.out.println("createdFolder = " + createdFolder);
            if (!createdFolder.exists()) {
                throw new NullPointerException();
            }

            File downloadedAudioFile;
            Path path = Paths.get(createdFolder + File.separator + audioFileName);
            boolean downloadedFileIsExists = Files.exists(path);
            if (downloadedFileIsExists) {
                System.out.println("File exists and don't need download it");
                downloadedAudioFile = path.toFile();
            } else {
                downloadedAudioFile = downloadFile(pathToYoutubedl, id, createdFolder.getAbsolutePath(), name, inputThread,
                        errorThread);
                System.out.println("downloadedAudioFile = " + downloadedAudioFile);
            }

//
            try {
                checkAudioFile(downloadedAudioFile);
////        convertFromM4atoAac(audioFile.getAbsolutePath(), outFolder, "new.aac");
//        //     audioFile = new File(audioFile.getAbsolutePath() + ".aac");
//        //     checkAudioFile(audioFile);
//        //
                //String jsonData = readJsonFile(outFolder, id);
                String duration = getTimeFromJson(jsonData);
                ArrayList<Pair<String, String>> pairs = getPairs(id, jsonData, name, inputThread, errorThread,
                        pathToYoutubedl);

                for (Pair<String, String> pair : pairs) {
                    System.out.println("pair = " + pair);
                }

                ArrayList<String> cutFiles = cutFileByPairs(ffmpegPath, downloadedAudioFile, pairs, duration, inputThread,
                        errorThread, createdFolder.getAbsolutePath());

                System.out.println("cutFiles = " + cutFiles.toString());

                if (cutFiles.size() == pairs.size()) {
                    int goodCount = 0;

                    for (int i = 0; i < cutFiles.size(); i++) {
                        if (cutFiles.get(i).contains(pairs.get(i).second)) {
                            goodCount++;
                        } else {
                            System.err.println(cutFiles.get(i));
                            System.err.println(pairs.get(i).second);
                        }
                    }
                    System.out.println("Good cut " + goodCount + "/" + pairs.size());
                } else {
                    System.err.println("Bad cut");
                }

                //uploadFileInGoogleDrive(Arrays.asList("Audio"), name, cutFiles);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.err.println(e);
            }

        }

        inputThread.shutdown();
        errorThread.shutdown();

    }

    public CommandArgumentsResult parsingArguments(String[] args, CommandArgumentsResult defaultValue) {

        Options options = new Options();

        Option optionYoutubedlOption = new Option("a", "pathToYoutubedl", true, "PathToYoutubedl");
        optionYoutubedlOption.setRequired(false);
        options.addOption(optionYoutubedlOption);

        Option outputFolderOption = new Option("b", "outputFolder", true, "OutputFolder");
        outputFolderOption.setRequired(false);
        options.addOption(outputFolderOption);

        Option linkIdOption = new Option("linkId", "linkId", true, "LinkId");
        linkIdOption.setRequired(true);
        options.addOption(linkIdOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("a")) {
                defaultValue.pathToYoutubedl = cmd.getOptionValue("a");
            }

            if (cmd.hasOption("b")) {
                defaultValue.outputFolderPath = cmd.getOptionValue("b");
            }

            if (cmd.hasOption("linkId")) {
                defaultValue.linkId = cmd.getOptionValue("linkId");
            }

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            // System.exit(1);
        }

        return defaultValue;
    }

    public static class CommandArgumentsResult {

        public String pathToYoutubedl;
        public String outputFolderPath;
        public String linkId;

        public CommandArgumentsResult(String pathToYoutubedl, String outputFolderPath, String linkId) {
            this.pathToYoutubedl = pathToYoutubedl;
            this.outputFolderPath = outputFolderPath;
            this.linkId = linkId;
        }

        @Override
        public String toString() {
            return "CommandArgumentsResult{" +
                    "pathToYoutubedl='" + pathToYoutubedl + '\'' +
                    ", outputFolderPath='" + outputFolderPath + '\'' +
                    ", shortId='" + linkId + '\'' +
                    '}';
        }
    }

    private ArrayList<Pair<String, String>> getDescFromYoutubeApi(String videoId) {

        List<String> desc = YoutubeAPIController.getInstance().getComments(videoId);
        ArrayList<Pair<String, String>> result = new ArrayList<>();
        for (String s : desc) {
            ArrayList<Pair<String, String>> parsingDescriptionResult = parsingDescriptionInfo(s);
            if (parsingDescriptionResult.size() > result.size()) {
                result = parsingDescriptionResult;
            }
        }

        return result;
    }

    private void uploadFileInGoogleDrive(List<String> pathToSave, String title, List<String> files) {
        GoogleDrive.getInstance().saveFileInGoogleDrive(pathToSave, title, files);
    }

    private ArrayList<Pair<String, String>> getPairs(String videoId, String jsonData, String name,
                                                     ExecutorService inputThread, ExecutorService errorThread, String pathToYoutubedl) {

        ArrayList<Pair<String, String>> result;

        String descriptionFromJson = getDescriptionFromJson(jsonData);
        ArrayList<Pair<String, String>> descPairs = parsingDescriptionInfo(descriptionFromJson);

        JSONArray chapters = getChaptersFromJson(jsonData);
        ArrayList<Pair<String, String>> chaptersPairs = parsingChaptersInfo(chapters);
        if (descPairs.size() > chaptersPairs.size()) {
            result = descPairs;
        } else {
            result = chaptersPairs;
        }

        ArrayList<Pair<String, String>> commentPairs = getDescFromYoutubeApi(videoId);
        if (commentPairs.size() > result.size()) {
            result = commentPairs;
        }

        if (result.size() == 0) {
            result.add(new Pair<>("00:00:00", name));
        }

        return result;
    }

    public void checkAudioFile(File file) throws FileNotFoundException {

        if (file == null) {
            throw new FileNotFoundException();
        }
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
//    if (!file.getName().contains(name)) {
//      throw new IllegalArgumentException();
//    }
    }

    public String makeGoodString(String value) {
        return value.replaceAll("[/\\-+.^:,]", "").trim();
    }

    public ArrayList<Pair<String, String>> findEqualsName(ArrayList<Pair<String, String>> pairs) {

        ArrayList<Pair<String, String>> result = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for (Pair<String, String> pair : pairs) {
            boolean contain = set.contains(pair.second);
            if (!contain) {
                set.add(pair.second);
                result.add(pair);
            } else {
                for (long i = 1; i < Long.MAX_VALUE; i++) {
                    String name = pair.second + " (" + i + ")";
                    if (!set.contains(name)) {
                        set.add(name);
                        Pair<String, String> tmpPair = new Pair<>(pair.first, name);
                        result.add(tmpPair);
                        break;
                    }
                }
            }
        }

        return result;
    }

    private String getAudioFileNameFromJsonData(String jsonData) {
        String result;
        JSONObject jsonObject = new JSONObject(jsonData);
        result = jsonObject.getString("_filename");
        return result;
    }

    private String getIdFromLink(String pathToYoutubedl, String link, ExecutorService inputThread,
                                 ExecutorService errorThread) {

        ArrayList<String> command = new ArrayList<>();
        command.add(pathToYoutubedl);
        command.add("--get-id");
        command.add(link);

        if (SystemUtils.IS_OS_LINUX) {
//      command.add("&");
//        command.add("wait");
        }

        ArrayList<List<String>> result = executeFunctionAndGetStringOutput(command.toArray(new String[0]), inputThread,
                errorThread);
        System.out.println("result = " + result);
        String id = result.get(0).get(0);

        return id;
    }


    private String getTimeFromJson(String json) {

        JSONObject jsonObject = new JSONObject(json);
        int duration = jsonObject.getInt("duration");

        return String.valueOf(duration);
    }

    private String getDescriptionFromJson(String json) {

        JSONObject jsonObject = new JSONObject(json);
        String description = jsonObject.getString("description");

        return description;
    }

    private JSONArray getChaptersFromJson(String json) {

        JSONObject jsonObject = new JSONObject(json);
        JSONArray chapters = new JSONArray();
        if (!jsonObject.isNull("chapters")) {
            chapters = jsonObject.getJSONArray("chapters");
        }

        return chapters;
    }

//  public ArrayList<List<String>> executeFunctionAndGetStringOutput(String stringCommand,
//      String rootDir, String[] env, ExecutorService inputThread, ExecutorService errorThread) {
//
//    ArrayList<String> commandArray = new ArrayList<>();
//
//    if (SystemUtils.IS_OS_LINUX) {
//      commandArray.add("sh");
//      commandArray.add("-c");
//    }
//    commandArray.add(stringCommand);
//    System.out.println("commandArray = " + commandArray);
//
//    ArrayList<List<String>> result = new ArrayList<>();
//    for (int i = 0; i < 2; i++) {
//      result.add(Collections.emptyList());
//    }
//    CountDownLatch countDownLatch = new CountDownLatch(2);
//
//    try {
//      Runtime runtime = Runtime.getRuntime();
//      Process command;
//      if (TextUtils.isEmpty(rootDir)) {
//        command = runtime.exec(commandArray.toArray(new String[]{}), env);
//      } else {
//        command = runtime.exec(commandArray.toArray(new String[]{}), env, new File(rootDir));
//      }
//      inputThread.execute(() -> {
//        try {
//          InputStream inputString = command.getInputStream();
//          List<String> resultInputString = getStringsFromInputStream(inputString);
//          inputString.close();
//          result.set(0, resultInputString);
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//        countDownLatch.countDown();
//      });
//
//      errorThread.execute(() -> {
//        try {
//          InputStream inputString = command.getErrorStream();
//          List<String> resultInputString = getStringsFromInputStream(inputString);
//          inputString.close();
//          result.set(1, resultInputString);
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//        countDownLatch.countDown();
//      });
//      int executionCode = command.waitFor();
//      System.out.println("executionCode = " + executionCode);
//
//    } catch (IOException | InterruptedException e) {
//      e.printStackTrace();
//      System.err.println(e);
//    }
//
//    try {
//      countDownLatch.await();
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
//
//    Objects.requireNonNull(result);
//    assert result.size() == 2;
//
//    return result;
//  }

    public Process getProcessInWindows(String[] stringCommandArray, String rootDir, String[] env) {

        ArrayList<String> commandArray = new ArrayList<>();
        commandArray.addAll(Arrays.asList(stringCommandArray));
        System.out.println("commandArray = " + commandArray);

        ArrayList<List<String>> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            result.add(Collections.emptyList());
        }
        CountDownLatch countDownLatch = new CountDownLatch(2);

        try {
            Runtime runtime = Runtime.getRuntime();
            Process command;
            if (TextUtils.isEmpty(rootDir)) {
                return command = runtime.exec(commandArray.toArray(new String[]{}), env);
            } else {
                return command = runtime.exec(commandArray.toArray(new String[]{}), env, new File(rootDir));
            }
        } catch (IOException e) {
            System.out.println("e = " + e);
        }
        return null;
    }

    public ArrayList<List<String>> executeFunctionAndGetStringOutputWithRoot(String[] stringCommandArray, String rootDir,
                                                                             ExecutorService inputThread, ExecutorService errorThread) {

        ArrayList<String> commandArray = new ArrayList<>();

//    if (SystemUtils.IS_OS_LINUX) {
//      commandArray.add("sh");
//      commandArray.add("-c");
//    }
        commandArray.addAll(Arrays.asList(stringCommandArray));
        System.out.println("rootDir = " + rootDir);
        System.out.println("commandArray = " + commandArray);

        ArrayList<List<String>> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            result.add(Collections.emptyList());
        }
        CountDownLatch countDownLatch = new CountDownLatch(2);

        try {
            Runtime runtime = Runtime.getRuntime();
            Process command;
            command = runtime.exec(commandArray.toArray(new String[]{}), new String[0], new File(rootDir));
            inputThread.execute(() -> {
                try {
                    InputStream inputString = command.getInputStream();
                    List<String> resultInputString = getStringsFromInputStream(inputString);
                    inputString.close();
                    result.set(0, resultInputString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            });

            errorThread.execute(() -> {
                try {
                    InputStream inputString = command.getErrorStream();
                    List<String> resultInputString = getStringsFromInputStream(inputString);
                    inputString.close();
                    result.set(1, resultInputString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            });
            int executionCode = command.waitFor();
            System.out.println("executionCode = " + executionCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println(e);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Objects.requireNonNull(result);
        assert result.size() == 2;

        return result;
    }

    public ArrayList<List<String>> executeFunctionAndGetStringOutput(String[] stringCommandArray,
                                                                     ExecutorService inputThread, ExecutorService errorThread) {

        ArrayList<String> commandArray = new ArrayList<>();

//    if (SystemUtils.IS_OS_LINUX) {
//      commandArray.add("sh");
//      commandArray.add("-c");
//    }
        commandArray.addAll(Arrays.asList(stringCommandArray));
        System.out.println("commandArray = " + commandArray);

        ArrayList<List<String>> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            result.add(Collections.emptyList());
        }
        CountDownLatch countDownLatch = new CountDownLatch(2);

        try {
            Runtime runtime = Runtime.getRuntime();
            Process command;
            command = runtime.exec(commandArray.toArray(new String[]{}));
            inputThread.execute(() -> {
                try {
                    InputStream inputString = command.getInputStream();
                    List<String> resultInputString = getStringsFromInputStream(inputString);
                    inputString.close();
                    result.set(0, resultInputString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            });

            errorThread.execute(() -> {
                try {
                    InputStream inputString = command.getErrorStream();
                    List<String> resultInputString = getStringsFromInputStream(inputString);
                    inputString.close();
                    result.set(1, resultInputString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            });
            int executionCode = command.waitFor();
            System.out.println("executionCode = " + executionCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println(e);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Objects.requireNonNull(result);
        assert result.size() == 2;

        return result;
    }

    private static List<String> getStringsFromInputStream(InputStream inputStream) {

        String line;
        List<String> result = new ArrayList<>();
        try {
            Reader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader stdInput = new BufferedReader(inputStreamReader);
            while ((line = stdInput.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;

    }

    private File deleteAndCreateFolder(String pathToFolder, String audioFilePath) {

        File file = new File(pathToFolder);
        if (file.exists()) {
            for (File listFile : file.listFiles()) {
                if (listFile.getName().equals(audioFilePath)) {
                    System.out.println("We found file");
                    continue;
                }
                listFile.delete();
            }
        }
        file.delete();
        file.mkdir();
        return file;
    }

    private File downloadFile(String pathToYoutubedl, String id, String saveFolder, String name,
                              ExecutorService inputThread, ExecutorService errorThread) {

        //String pathFile = saveFolder + id + "\\" + name;

//    String commandPath = pathToYoutubedl
//        + " -f bestaudio -o "
//        + " \"" + saveFolder + File.separator + "original_%(id)s.%(ext)s\" "
//        //+ " \"" + pathFile + "\" "
//        //+ " -q "
//        //    + " --no-warnings "
//        + " --no-progress -v --no-cache-dir --rm-cache-dir  --no-continue  "
//        //+ " --write-info-json "
//        + id;

        String rootDirPath;
        String fileName;
        if (SystemUtils.IS_OS_LINUX) {
            rootDirPath = "/";
            fileName = "" + saveFolder + File.separator + "original_%(id)s.%(ext)s";
        } else {
            rootDirPath = "";
            fileName = "\"" + saveFolder + File.separator + "original_%(id)s.%(ext)s\"";
        }

        ArrayList<String> command = new ArrayList<>();
        command.add(pathToYoutubedl);
        command.add("-f");
        command.add("bestaudio");
        command.add("-o");
        command.add(fileName);
        command.add("--no-progress");
        command.add("-v");
        command.add("--no-cache-dir");
        command.add("--rm-cache-dir");
        command.add("--no-continue");
        command.add(id);

        System.out.println("command = " + command);

        //System.out.println("commandPath = " + commandPath);
        ArrayList<List<String>> outputResult = executeFunctionAndGetStringOutputWithRoot(command.toArray(new String[0]), rootDirPath,
                inputThread, errorThread);
        System.out.println("outputResult = " + outputResult);
        String[] standardOutputResult = outputResult.get(0).toArray(new String[0]);
        for (String s : standardOutputResult) {
            final String downloadString = "[download]";
            if (s.startsWith(downloadString)) {
                int firstPoint = s.indexOf(downloadString) + downloadString.length();
                String downloadSubString = s.substring(firstPoint).trim();
                final String destinationString = "Destination:";
                if (downloadSubString.startsWith(destinationString)) {
                    String pathToFile = downloadSubString.substring(destinationString.length()).trim();
                    return new File(pathToFile);
                }
            }
        }

        return null;
    }

    private String downloadJsonInMemory(String pathToYoutubedl, String id, ExecutorService inputThread,
                                        ExecutorService errorThread) {

        ArrayList<String> commandArray = new ArrayList<>();

        commandArray.add(pathToYoutubedl);
        commandArray.add("--skip-download");
        commandArray.add("-f");
        commandArray.add("bestaudio");
        commandArray.add("-o");
        commandArray.add("\"" + "original_%(id)s.%(ext)s" + "\"");
        commandArray.add("--print-json");
        commandArray.add(id);

        System.out.println("commandPath = " + commandArray.toString());

        ArrayList<List<String>> result = executeFunctionAndGetStringOutput(commandArray.toArray(new String[0]), inputThread,
                errorThread);
        System.out.println("result = " + result);

        List<String> standardOutput = result.get(0);
        return standardOutput.get(0);
    }


    private ArrayList<String> cutFileByPairs(String ffmpegPath, File audioFile, ArrayList<Pair<String, String>> pairs,
                                             String duration, ExecutorService inputThread, ExecutorService errorThread, String absolutePath) {

        ArrayList<String> result = new ArrayList<>();

        for (int i = 0; i < pairs.size(); i++) {

            String startTime = pairs.get(i).first;
            String endTime;
            if (i != pairs.size() - 1) {
                endTime = pairs.get(i + 1).first;
            } else {
                endTime = String.valueOf(Integer.parseInt(duration));
            }

            String fileName = pairs.get(i).second.trim();
            fileName = makeGoodString(fileName);

            String audioOutName = (audioFile.getParent() + File.separator + fileName) + ".mp4";

            String outputFilePath;

            ArrayList<String> commandArray = new ArrayList<>();
            commandArray.add(ffmpegPath);
            // commandArray.add(" -loglevel debug");
            commandArray.add("-y");
            commandArray.add("-i");
            commandArray.add(audioFile.getAbsolutePath());
            commandArray.add("-ss");
            commandArray.add(startTime);
            commandArray.add("-to");
            commandArray.add(endTime);

            String rootDirPath;
            if (SystemUtils.IS_OS_LINUX) {
                rootDirPath = absolutePath + File.separator;
                outputFilePath = "" + fileName + ".mp4" + "";
            } else {
                outputFilePath = audioOutName;
                rootDirPath = "";
            }

            commandArray.add(outputFilePath);

            ArrayList<List<String>> executeResult = executeFunctionAndGetStringOutputWithRoot(
                    commandArray.toArray(new String[0]), rootDirPath, inputThread, errorThread);

            System.out.println("executeResult = " + executeResult);

            String[] error = executeResult.get(1).toArray(new String[0]);

            audioOutName = getFileNameFromFfmpegCut(error);
            //System.out.println("audioOutName = " + audioOutName);
            File file = new File(audioOutName);
            if (!file.exists()) {
                System.err.println("audioOutName = " + audioOutName);
            } else {
                result.add(audioOutName);
            }

        }

        return result;
    }

    private String getFileNameFromFfmpegCut(String[] executeResult) {

        String result = "";

        for (String s : executeResult) {
            if (s.startsWith("Output #0")) {
                int firstPoint = s.indexOf(" to ") + " to ".length() + "\'".length();
                return s.substring(firstPoint, s.length() - 2);
            }
        }

        return result;
    }

    private ArrayList<Pair<String, String>> parsingChaptersInfo(JSONArray chapters) {

        ArrayList<Pair<String, String>> pairs = new ArrayList<>();
        DateTimeFormatter pattern = DateTimeFormat.forPattern("HH:mm:ss");

        for (int i = 0; i < chapters.length(); i++) {
            JSONObject jsonObject = chapters.getJSONObject(i);
            double endTime = jsonObject.getDouble("end_time");
            double startTime = jsonObject.getDouble("start_time");
            int duration = (int) (endTime - startTime);
            String title = jsonObject.getString("title");

            DateTime dt = new DateTime((int) startTime * 1000, DateTimeZone.UTC);
            DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm:ss");
            fmt.withZone(DateTimeZone.UTC);
            String dtStr = fmt.print(dt);

            title = makeGoodString(title);

            pairs.add(new Pair<>(dtStr, title));
        }

        pairs.sort((o1, o2) -> {
            DateTime dateTime = pattern.parseDateTime(o1.first);
            int o1s = dateTime.secondOfDay().get();
            dateTime = pattern.parseDateTime(o2.first);
            int o2s = dateTime.secondOfDay().get();
            return Integer.compare(o1s, o2s);
        });

        return pairs;
    }

    private ArrayList<Pair<String, String>> parsingDescriptionInfo(String description) {

        String[] lines = description.split("\n");
        ArrayList<Pair<String, String>> pairs = new ArrayList<>();

        for (String line : lines) {

            int firstPoint = -1;
            do {
                firstPoint = line.indexOf(":", firstPoint + 1);
                if (firstPoint == -1) {
                    break;
                }
                int firstPointNumber = firstPoint;
                while (Character.isDigit(line.charAt(firstPointNumber)) || line.charAt(firstPointNumber) == ':') {
                    firstPointNumber--;
                    if (firstPointNumber < 0) {
                        firstPointNumber = 0;
                        break;
                    }
                }
                int secondPointNumber = firstPoint + 1;
                if (secondPointNumber < line.length()) {
                    while (Character.isDigit(line.charAt(secondPointNumber)) || line.charAt(secondPointNumber) == ':') {
                        secondPointNumber++;
                        if (secondPointNumber >= line.length()) {
                            break;
                        }
                    }
                }
                secondPointNumber++;
                secondPointNumber = Math.min(secondPointNumber, line.length());
                String time = line.substring(firstPointNumber, secondPointNumber);
                time = time.trim();
                firstPoint = secondPointNumber;
                if (time.length() < 4) {
                    continue;
                }
//
                String clearTime = getAllBadCharacterFromString(time);

                String goodTime = setFullFormatTime(clearTime);

                String goodLine = line.substring(0, firstPointNumber) + line.substring(secondPointNumber);

                goodLine = makeGoodString(goodLine);

                pairs.add(new Pair<>(goodTime, goodLine));

            } while (firstPoint != -1);
        }
        DateTimeFormatter pattern = DateTimeFormat.forPattern("HH:mm:ss");

        pairs.sort((o1, o2) -> {
            DateTime dateTime = pattern.parseDateTime(o1.first);
            int o1s = dateTime.secondOfDay().get();
            dateTime = pattern.parseDateTime(o2.first);
            int o2s = dateTime.secondOfDay().get();
            return Integer.compare(o1s, o2s);
        });

        return pairs;
    }

    private String getAllBadCharacterFromString(String substring) {

        int firstPoint = 0;
        while (!Character.isDigit(substring.charAt(firstPoint))) {
            firstPoint++;
        }
        int secondPoint = substring.length() - 1;
        while (!Character.isDigit(substring.charAt(secondPoint))) {
            secondPoint--;
        }

        return substring.substring(firstPoint, secondPoint + 1);
    }

    public String setFullFormatTime(String time) {
        String[] splitTime = time.split(":");
        List<String> arraySplitTime = new ArrayList<String>(Arrays.asList(splitTime));
        if (arraySplitTime.size() == 3) {
            String hour = arraySplitTime.get(0);
            if (hour.length() == 1) {
                arraySplitTime.set(0, '0' + arraySplitTime.get(0));
            }
        }
        if (arraySplitTime.size() == 2) {
            arraySplitTime.add(0, "00");
        }
        String minute = arraySplitTime.get(1);
        if (minute.length() == 1) {
            arraySplitTime.set(1, '0' + arraySplitTime.get(1));
        }

        return String.join(":", arraySplitTime);
    }

}
