package com.green.square;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringMainClass {

  public static void main(String[] args) {
    SpringApplication.run(SpringMainClass.class, args);

//    testDatabase();

  }

  private static void testDatabase() {
//    Preferences prefs = Preferences.systemRoot();
//    System.out.println("prefs.get(\"Ping\", \"No\") = " + prefs.get("Ping", "No"));
//    System.out.println("prefs.absolutePath() = " + prefs.absolutePath());
//
//    try {
//      prefs.clear();
//    } catch (BackingStoreException e) {
//      e.printStackTrace();
//    }
//    prefs.put("Ping", "Pong");
//    System.out.println("prefs.get(\"Ping\", \"No\") = " + prefs.get("Ping", "No"));

  }
}
