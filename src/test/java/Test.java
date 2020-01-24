import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javafx.util.Pair;
import org.glassfish.jersey.process.internal.ExecutorProviders;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
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

  @org.junit.Test
  public void testSingleFixedThread(){

    ExecutorService fixedSingle = Executors.newSingleThreadExecutor();
    Future<?> future = fixedSingle.submit(new Runnable() {
      @Override
      public void run() {
        try {
          System.out.println("1 = " + 1);
          Thread.sleep(10000);
          System.out.println("1 end " + 1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    fixedSingle.shutdown();
    System.out.println("\"awaitTermination\" = " + "awaitTermination");
    try {
      boolean done = fixedSingle.awaitTermination(19, TimeUnit.SECONDS);
      System.out.println("done = " + done);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("runSoutv " + 2);
    fixedSingle.execute(new Runnable() {
      @Override
      public void run() {
        try {
          System.out.println("2 = " + 2);
          Thread.sleep(15000);
          System.out.println("2 end " + 2);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }


}
