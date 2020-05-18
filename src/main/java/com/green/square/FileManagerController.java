package com.green.square;

import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileManagerController {

  @Autowired
  public CustomerRepository repository;

  private static FileManagerController ourInstance = new FileManagerController();

  public static FileManagerController getInstance() {
    return ourInstance;
  }

  public FileManagerController() {
  }


  public File[] getFilesByPath(String path){

    File[] result = {};

    File file = new File(path);
    if(file.exists()){
      if(file.isDirectory())
        result = file.listFiles();
    }

    return result;
  }


}
