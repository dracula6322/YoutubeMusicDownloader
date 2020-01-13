import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javafx.util.Pair;
import org.json.JSONObject;

public class Test {

  @org.junit.Test
  public void qew(){
    System.out.println("\"Hello\" = " + "Hello");

    ArrayList<Pair<String, String>> pairs = new ArrayList<>();
    pairs.add(new Pair<>("asd1", "zxc"));
    pairs.add(new Pair<>("asd2", "zxc (2)"));
    pairs.add(new Pair<>("asd3", "zxc"));

    pairs = Main.findEqualsName(pairs);
    for (Pair<String, String> pair : pairs) {
      System.out.println("pair = " + pair);
    }

  }

}
