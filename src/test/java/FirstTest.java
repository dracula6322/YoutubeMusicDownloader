import com.green.square.ProgramExecutor;
import com.green.square.model.DownloadState;
import com.green.square.youtubedownloader.ProgramArgumentsController;
import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirstTest {

  @Test
  public void checkPairCountTest() {

    Logger logger = LoggerFactory.getLogger(FirstTest.class);

    ProgramExecutor programExecutor = new ProgramExecutor();
    YoutubeDownloaderAndCutter youtubeDownloaderAndCutter = new YoutubeDownloaderAndCutter(programExecutor, null);
    ProgramArgumentsController arguments = new ProgramArgumentsController();
    String videoLink = "https://www.youtube.com/watch?v=7bQYfBya7K8&t=658s";

    youtubeDownloaderAndCutter
        .getPairsAndCutTheAllByThisPairsFileIntoPieces(arguments.getArguments().getPathToYoutubedl(), videoLink, logger,
            arguments.getArguments().getFfmpegPath(), arguments.getArguments().getOutputFolderPath())
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
