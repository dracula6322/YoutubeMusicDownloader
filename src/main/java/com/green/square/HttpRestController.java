package com.green.square;

import com.green.square.model.CommandArgumentsResult;
import com.green.square.model.DownloadState;
import com.green.square.youtubedownloader.ProgramArgumentsController;
import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HttpRestController extends HttpServlet {

  @Autowired
  private DownloadStateRepository downloadStateRepository;

  @Autowired
  private FileManagerController fileManagerController;

  @Autowired
  private ZipController zipController;

  public YoutubeDownloaderAndCutter youtubeDownloaderAndCutter;
  public CommandArgumentsResult commandArgumentsResult;
  public Logger logger = LoggerFactory.getLogger(getClass().getName());

  @Autowired
  public HttpRestController(ProgramArgumentsController programArgumentsController,
      YoutubeDownloaderAndCutter youtubeDownloaderAndCutter, ZipController zipController) {
    this.youtubeDownloaderAndCutter = youtubeDownloaderAndCutter;
    this.commandArgumentsResult = programArgumentsController.getArguments();
    this.zipController = zipController;
  }

  @RequestMapping(value = "/files/", method = RequestMethod.GET)
  public String getAllYoutubeFolders() {

    JSONObject result = FileManagerController.getInstance()
        .getFilesByPathInJson(commandArgumentsResult.getOutputFolderPath());
    return result.toString();
  }

  @RequestMapping(value = "/files/{filePath}", method = RequestMethod.GET)
  public String getFiles(@PathVariable("filePath") String filePath) {

    JSONObject result = fileManagerController
        .getFilesByPathInJsonWithBase64(commandArgumentsResult.getOutputFolderPath() + filePath);

    return result.toString();
  }

  @RequestMapping(value = "/files/{filePath}/{hashNameFile}", method = RequestMethod.GET)
  public void getFile(@PathVariable("filePath") String filePath, @PathVariable("hashNameFile") String hashNameFile,
      HttpServletResponse response) throws IOException {
    downloadFile(hashNameFile, filePath, response);
  }

  @RequestMapping(value = "/files/all/{filePath}", method = RequestMethod.GET)
  public void downloadAllFiles(@PathVariable("filePath") String filePath, HttpServletResponse response) {

    downloadAllFilesWithZip(filePath, response);
  }

  @RequestMapping(value = "/downloadAndCutVideo/{videoId}", method = RequestMethod.GET)
  public void downloadAndCutVideo(@PathVariable("videoId") String videoId) {

    CommandArgumentsResult arguments = commandArgumentsResult;

    String videoFullLength = "https://www.youtube.com/watch?v=" + videoId;

    Single<DownloadState> pairsAndCutTheFileIntoPieces = youtubeDownloaderAndCutter
        .getPairsAndCutTheAllByThisPairsFileIntoPieces(arguments.pathToYoutubedl, videoFullLength, logger,
            arguments.ffmpegPath,
            arguments.outputFolderPath);

    pairsAndCutTheFileIntoPieces.subscribe(new SingleObserver<DownloadState>() {
      @Override
      public void onSubscribe(@NonNull Disposable d) {

      }

      @Override
      public void onSuccess(@NonNull DownloadState downloadState) {
        logger.info(downloadState.toString());
      }

      @Override
      public void onError(@NonNull Throwable e) {
        logger.error(e.getMessage(), e);
      }
    });
//
//    rxSinglePairs.subscribe(new SingleObserver<DownloadState>() {
//      @Override
//      public void onSubscribe(@NonNull Disposable d) {
//        logger.info("onSubscribe");
//        logger.info(d.toString());
//      }
//
//      @Override
//      public void onSuccess(@NonNull DownloadState downloadState) {
//
//        logger.info("onSuccess");
//        logger.info(downloadState.toString());
//
//        downloadStateRepository.save(downloadState);
//
//        File downloadedVideoFilePath = HttpRestController.this.youtubeDownloaderAndCutter
//            .downloadVideo(logger, arguments.getPathToYoutubedl(), downloadState.getAudioFileName(),
//                downloadState.getCreatedFolderPath(), downloadState.getVideoId());
//
//        List<File> files = HttpRestController.this.youtubeDownloaderAndCutter
//            .cutTheFileIntoPieces(downloadedVideoFilePath.getAbsolutePath(), downloadState.getPairs(), logger,
//                arguments, downloadState.getCreatedFolderPath(), "mp3");
//
//        logger.info("downloadAndCutVideo is over");
//        logger.info(files.toString());
//      }
//
//      @Override
//      public void onError(@NonNull Throwable e) {
//        logger.error("onError");
//        logger.error(e.getMessage());
//        e.printStackTrace();
//      }
//    });

  }

  public void downloadAllFilesWithZip(String videoId, HttpServletResponse response) {

    File folder = new File(commandArgumentsResult.getOutputFolderPath() + videoId + File.separator);
    File[] files = folder.listFiles();
    if (files == null) {
      logger.info("folder is empty " + folder.getAbsolutePath());
      return;
    }
    Objects.requireNonNull(files);

    File zipFile = zipController.convertFilesToZip(Arrays.asList(files), folder.getAbsolutePath(), "test.zip", logger);
    Objects.requireNonNull(zipFile);

    zipController.sendFileToHttpResponse(zipFile, response, logger);
  }


  public void downloadFile(String hashNameFile, String videoId, HttpServletResponse response) throws IOException {
    File folder = new File(commandArgumentsResult.getOutputFolderPath() + videoId + File.separator);
    File[] files = folder.listFiles();
    if (files == null) {
      logger.error("Folder is empty " + folder.getAbsolutePath());
      return;
    }

    for (File file : files) {
      String localFileHashName = fileManagerController.getHashNameFile(file.getName());
      if (localFileHashName.equals(hashNameFile)) {
        zipController.sendFileToHttpResponse(file, response, logger);
        return;
      }
    }
    logger.error("File with hash not found " + hashNameFile);
  }


}
