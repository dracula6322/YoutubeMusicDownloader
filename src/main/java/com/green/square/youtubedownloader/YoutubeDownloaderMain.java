package com.green.square.youtubedownloader;

import com.green.square.model.CommandArgumentsResult;
import com.green.square.model.DownloadState;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class YoutubeDownloaderMain {

  // [ERROR: Signature extraction failed: Traceback (most recent call last):,   File "C:\Users\dst\AppData\Roaming\Build archive\youtube-dl\ytdl-org\tmpspbsbtmq\build\youtube_dl\extractor\youtube.py", line 1426, in _decrypt_signature,   File "C:\Users\dst\AppData\Roaming\Build archive\youtube-dl\ytdl-org\tmpspbsbtmq\build\youtube_dl\extractor\youtube.py", line 1338, in _extract_signature_function,   File "C:\Users\dst\AppData\Roaming\Build archive\youtube-dl\ytdl-org\tmpspbsbtmq\build\youtube_dl\extractor\youtube.py", line 1402, in <lambda>,   File "C:\Users\dst\AppData\Roaming\Build archive\youtube-dl\ytdl-org\tmpspbsbtmq\build\youtube_dl\jsinterp.py", line 258, in resf,   File "C:\Users\dst\AppData\Roaming\Build archive\youtube-dl\ytdl-org\tmpspbsbtmq\build\youtube_dl\jsinterp.py", line 56, in interpret_statement,   File "C:\Users\dst\AppData\Roaming\Build archive\youtube-dl\ytdl-org\tmpspbsbtmq\build\youtube_dl\jsinterp.py", line 92, in interpret_expression,   File "C:\Users\dst\AppData\Roaming\Build archive\youtube-dl\ytdl-org\tmpspbsbtmq\build\youtube_dl\jsinterp.py", line 189, in interpret_expression,   File "C:\Users\dst\AppData\Roaming\Build archive\youtube-dl\ytdl-org\tmpspbsbtmq\build\youtube_dl\jsinterp.py", line 56, in interpret_statement,   File "C:\Users\dst\AppData\Roaming\Build archive\youtube-dl\ytdl-org\tmpspbsbtmq\build\youtube_dl\jsinterp.py", line 211, in interpret_expression, youtube_dl.utils.ExtractorError: Unsupported JS expression '['; please report this issue on https://yt-dl.org/bug . Make sure you are using the latest version; type  youtube-dl -U  to update. Be sure to call youtube-dl with the --verbose flag and include its complete output.,  (caused by ExtractorError("Unsupported JS expression '['; please report this issue on https://yt-dl.org/bug . Make sure you are using the latest version; type  youtube-dl -U  to update. Be sure to call youtube-dl with the --verbose flag and include its complete output.",)); please report this issue on https://yt-dl.org/bug . Make sure you are using the latest version; type  youtube-dl -U  to update. Be sure to call youtube-dl with the --verbose flag and include its complete output.]])
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

    youtubeDownloaderAndCutter
        .getPairsAndCutTheAllByThisPairsFileIntoPieces(pathToYoutubedl, linkId, logger, ffmpegPath, outFolder)
        .subscribe(new SingleObserver<DownloadState>() {
          @Override
          public void onSubscribe(@NonNull Disposable d) {

          }

          @Override
          public void onSuccess(@NonNull DownloadState downloadState) {
            logger.info(String.valueOf(downloadState));
          }

          @Override
          public void onError(@NonNull Throwable e) {
            logger.error(e.getMessage(), e);
          }
        });
  }
}
