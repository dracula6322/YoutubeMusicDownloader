import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import java.net.UnknownHostException;
import java.util.ArrayList;
import javafx.util.Pair;

public class MongoDBHelper {

  private static MongoDBHelper ourInstance = new MongoDBHelper();

  MongoClient mongoClient;

  public static MongoDBHelper getInstance() {
    return ourInstance;
  }

  public MongoDBHelper() {
    try {
      mongoClient = new MongoClient();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }

  public void writeComparePairResult(String id,
      ArrayList<Pair<String, String>> descPairs,
      ArrayList<Pair<String, String>> chaptersPairs, String json) {

    DB YoutubeDownloader = mongoClient.getDB("YoutubeDownloader");
    DBCollection descParsing = YoutubeDownloader.getCollection("descParsing");
    DBCollection descParsingResult = descParsing.getCollection("descParsingResult");
    DBCollection collectionId = descParsingResult.getCollection(id);

    clearCollection(collectionId);

    DBObject newValue = new BasicDBObject(0);
    newValue.put("json", json);

    if (descPairs.size() != chaptersPairs.size()) {
      System.err.println("Error size pairs");
      newValue.put("message", "Error size pairs");
      newValue.put("descPairs", descPairs.toString());
      newValue.put("chapterPairs", chaptersPairs.toString());
    }
    collectionId.insert(newValue);

    for (int i = 0; i < Math.min(descPairs.size(), chaptersPairs.size()); i++) {

      newValue = new BasicDBObject(0);
      boolean isTheSame = descPairs.get(i).toString().equals(chaptersPairs.get(i).toString());
      if(!isTheSame)
        System.err.println(descPairs.get(i).toString() + " " + chaptersPairs.get(i).toString());
      newValue.put("isTheSame", isTheSame);
      newValue.put("desc", descPairs.get(i).toString());
      newValue.put("chapter", chaptersPairs.get(i).toString());

      collectionId.insert(newValue);

    }
  }

  public void clearCollection(DBCollection collection) {
    DBCursor cursor = collection.find();
    while (cursor.hasNext()) {
      collection.remove(cursor.next());
    }
  }


}