package com.green.square.youtubedownloader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;
import org.springframework.stereotype.Component;

@Component
public class ProgramArgumentsController {

  static class ProgramArgumentsHolder {

    public static final ProgramArgumentsController HOLDER_INSTANCE = new ProgramArgumentsController();
  }

  public static ProgramArgumentsController getInstance() {
    return ProgramArgumentsHolder.HOLDER_INSTANCE;
  }

  public static final String pathToYoutubedlOptionsName = "pathToYoutubedl";
  public static final String outputFolderOptionsName = "outputFolder";
  public static final String linkIdOptionsName = "linkId";
  public static final String ffmpegPathOptionsName = "ffmpegPath";

  CommandArgumentsResult commandArgumentsResult;

  public CommandArgumentsResult getArguments() {
    if (commandArgumentsResult == null) {
      setArguments(getDefaultArguments());
    }
    return commandArgumentsResult;
  }

  public void setArguments(CommandArgumentsResult arguments) {
    this.commandArgumentsResult = arguments;
  }

  public CommandArgumentsResult setArgumentsWithValue(String[] args, Logger logger) {
    setArguments(getDefaultArguments(args, logger));
    return getArguments();
  }

  public CommandArgumentsResult setArgumentsWithValue(String[] args) {
    setArguments(getDefaultArguments(args, NOPLogger.NOP_LOGGER));
    return getArguments();
  }

  private CommandArgumentsResult getDefaultArguments() {
    return getDefaultArguments(new String[]{}, LoggerFactory.getLogger(""));
  }

  private CommandArgumentsResult getDefaultArguments(String[] args, Logger logger) {

    String outFolder;
    String pathToYoutubedl;
    String linkId;
    String ffmpegPath;

    if (SystemUtils.IS_OS_LINUX) {

      String homeFolder = System.getProperty("user.home");

      outFolder = homeFolder + "/youtubeNew/";
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
    return parsingArguments(args, defaultArguments, logger);
  }


  private CommandArgumentsResult parsingArguments(String[] args, CommandArgumentsResult defaultValue, Logger logger) {

    Options options = new Options();

    Option optionYoutubedlOption = new Option(pathToYoutubedlOptionsName, pathToYoutubedlOptionsName, true,
        "PathToYoutubedl");
    optionYoutubedlOption.setRequired(false);
    options.addOption(optionYoutubedlOption);

    Option outputFolderOption = new Option(outputFolderOptionsName, outputFolderOptionsName, true, "OutputFolder");
    outputFolderOption.setRequired(false);
    options.addOption(outputFolderOption);

    Option linkIdOption = new Option(linkIdOptionsName, linkIdOptionsName, true, "LinkId");
    linkIdOption.setRequired(false);
    options.addOption(linkIdOption);

    Option ffmpegPath = new Option(ffmpegPathOptionsName, ffmpegPathOptionsName, true, "FfmpegPath");
    ffmpegPath.setRequired(false);
    options.addOption(ffmpegPath);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);

      if (cmd.hasOption(pathToYoutubedlOptionsName)) {
        defaultValue.pathToYoutubedl = cmd.getOptionValue(pathToYoutubedlOptionsName);
      }

      if (cmd.hasOption(outputFolderOptionsName)) {
        defaultValue.outputFolderPath = cmd.getOptionValue(outputFolderOptionsName);
      }

      if (cmd.hasOption(linkIdOptionsName)) {
        defaultValue.linkId = cmd.getOptionValue(linkIdOptionsName);
      }

      if (cmd.hasOption(ffmpegPathOptionsName)) {
        defaultValue.ffmpegPath = cmd.getOptionValue(ffmpegPathOptionsName);
      }

    } catch (ParseException e) {
      logger.debug(e.getMessage());
      formatter.printHelp("utility-name", options);
      System.exit(1);
    }

    return defaultValue;
  }

}
