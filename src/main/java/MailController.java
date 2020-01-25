import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;

public class MailController {

  private static MailController ourInstance = new MailController();

  public static MailController getInstance() {
    return ourInstance;
  }

  public MailController() {
  }

  public void getMail(){

    //    String host = "pop.mail.ru";// change accordingly
//    String mailStoreType = "pop3";
//    String username = "and.solomatin@mail.ru";// change accordingly
//    String password = "pingvine6322qqq";// change accordingly
//
//    GoogleDrive.getInstance().check(host, mailStoreType, username, password);

  }


  public void check(String host, String storeType, String user,
      String password) {
    try {

      //create properties field
      Properties properties = new Properties();

      properties.put("mail.pop3.host", host);
      properties.put("mail.pop3.port", "995");
      properties.put("mail.pop3.starttls.enable", "true");
      Session emailSession = Session.getDefaultInstance(properties);

      //create the POP3 store object and connect with the pop server
      Store store = emailSession.getStore("pop3s");

      store.connect(host, user, password);

      //create the folder object and open it
      Folder emailFolder = store.getFolder("INBOX");
      emailFolder.open(Folder.READ_ONLY);

      // retrieve the messages from the folder in an array and print it
      Message[] messages = emailFolder.getMessages();
      System.out.println("messages.length---" + messages.length);

//      for (int i = 0, n = messages.length; i < n; i++) {
//        Message message = messages[i];
//        System.out.println("---------------------------------");
//        System.out.println("Email Number " + (i + 1));
//        System.out.println("Subject: " + message.getSubject());
//        System.out.println("From: " + message.getFrom()[0]);
//        System.out.println("Text: " + message.getContent().toString());
//      }

      Message message = emailFolder.getMessage(emailFolder.getMessageCount());
      Multipart mp = (Multipart) message.getContent();
      // Вывод содержимого в консоль
      for (int i = 0; i < mp.getCount(); i++) {
        BodyPart bp = mp.getBodyPart(i);
        if (bp.getFileName() == null) {
          System.out.println("    " + i + ". сообщение : '" +
              bp.getContent() + "'");
        } else {
          System.out.println("    " + i + ". файл : '" +
              bp.getFileName() + "'");
        }
      }

      //close the store and folder objects
      emailFolder.close(false);
      store.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
