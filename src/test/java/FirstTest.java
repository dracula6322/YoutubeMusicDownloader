import com.green.square.DownloadState;
import com.green.square.ProgramExecutor;
import com.green.square.youtubedownloader.ProgramArgumentsController;
import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirstTest {

  @Test
  public void checkPairCountTest() {

    Logger logger = LoggerFactory.getLogger(FirstTest.class);

    ProgramExecutor programExecutor = new ProgramExecutor();
    YoutubeDownloaderAndCutter youtubeDownloaderAndCutter = new YoutubeDownloaderAndCutter(programExecutor);
    ProgramArgumentsController arguments = new ProgramArgumentsController();
    String videoLink = "https://www.youtube.com/watch?v=Kuy17LO14OA&t=1925s";

    @NonNull Single<DownloadState> state = youtubeDownloaderAndCutter
        .getPairs(arguments.getArguments().getPathToYoutubedl(), videoLink, logger);

    state.doOnSuccess(new Consumer<DownloadState>() {
      @Override
      public void accept(DownloadState downloadState) throws Throwable {
        System.out.println("downloadState = " + downloadState);
//        assertEquals(downloadState.getPairs().size(), 16);
      }
    }).doOnError(new Consumer<Throwable>() {
      @Override
      public void accept(Throwable throwable) throws Throwable {
        logger.error(throwable.getMessage());
      }
    }).map(new Function<DownloadState, DownloadState>() {
      @Override
      public DownloadState apply(DownloadState downloadState) throws Throwable {

        String createdFolderPath = youtubeDownloaderAndCutter
            .createFolder(arguments.getArguments().getOutputFolderPath(), downloadState.getVideoId(),
                downloadState.getAudioFileName(), logger);
        return downloadState.toBuilder().createdFolderPath(createdFolderPath).build();
      }
    }).map(new Function<DownloadState, DownloadState>() {
      @Override
      public DownloadState apply(DownloadState downloadState) throws Throwable {

        File downloadedVideoFilePath = youtubeDownloaderAndCutter
            .downloadVideo(logger, arguments.getArguments().getPathToYoutubedl(), downloadState.getAudioFileName(),
                downloadState.getCreatedFolderPath(), downloadState.getVideoId());

        Objects.requireNonNull(downloadedVideoFilePath, "File is null");

        return downloadState.toBuilder().downloadedAudioFilePath(downloadedVideoFilePath.getAbsolutePath()).build();
      }
    }).doOnSuccess(new Consumer<DownloadState>() {
      @Override
      public void accept(DownloadState downloadState) throws Throwable {
        logger.info("downloadState.getDownloadedAudioFilePath() = " + downloadState.getDownloadedAudioFilePath());

        ArrayList<File> files = youtubeDownloaderAndCutter
            .cutTheFileIntoPieces(downloadState.getDownloadedAudioFilePath(), downloadState.getPairs(),
                logger, arguments.getArguments(), downloadState.getCreatedFolderPath(),
                downloadState.getDurationInSeconds());

        logger.info("files = " + files.toString());
      }
    }).doOnError(new Consumer<Throwable>() {
      @Override
      public void accept(Throwable throwable) throws Throwable {
        logger.error(throwable.getMessage());
      }
    }).subscribe(new SingleObserver<DownloadState>() {
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
