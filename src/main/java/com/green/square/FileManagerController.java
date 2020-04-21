package com.green.square;

import java.io.File;

public class FileManagerController {

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
