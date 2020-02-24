import com.green.square.youtubedownloader.YoutubeDownloader;
import java.util.ArrayList;
import javafx.util.Pair;

public class Test {

  @org.junit.Test
  public void qew(){
    System.out.println("\"Hello\" = " + "Hello");

    ArrayList<Pair<String, String>> pairs = new ArrayList<>();
    pairs.add(new Pair<>("asd1", "zxc"));
    pairs.add(new Pair<>("asd2", "zxc (2)"));
    pairs.add(new Pair<>("asd3", "zxc"));

    pairs = YoutubeDownloader.findEqualsName(pairs);
    for (Pair<String, String> pair : pairs) {
      System.out.println("pair = " + pair);
    }
  }

}
