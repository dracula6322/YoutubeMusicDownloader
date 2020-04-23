package com.green.square;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController extends HttpServlet {

  ExecutorService downloadThread;

  @Autowired
  static public RestFileSystemConfiguration provide;

  public GreetingController(RestFileSystemConfiguration restFileSystemConfiguration) {

    this.provide = restFileSystemConfiguration;
    System.out.println("fileSystemConfiguration = " + this.provide);
    System.out.println("\"We are here\" = " + "We are here");
    downloadThread = Executors.newSingleThreadExecutor();
  }

  @RequestMapping(value = "/files/", method = RequestMethod.GET)
  public String getAllYoutubeFolders() {

    JSONObject result = new JSONObject();
    File[] folders = FileManagerController.getInstance().getFilesByPath(provide.getOutFolder());
    JSONArray foldersArray = new JSONArray();
    for (File folder : folders) {
      foldersArray.put(folder.getName());
    }
    result.put("folders", foldersArray);

    return result.toString();
  }

  @RequestMapping(value = "/files/{filePath}", method = RequestMethod.GET)
  public String getFiles(@PathVariable("filePath") String filePath) {

    System.out.println("filePath = " + filePath);
    JSONObject result = new JSONObject();

    File[] files = FileManagerController.getInstance().getFilesByPath(provide.getOutFolder() + filePath);
    result.put("filePath", filePath);
    JSONArray filesJsonArrays = new JSONArray();
    for (File file : files) {
      JSONObject item = new JSONObject();
      item.put("name", file.getName());
      item.put("base64", getHashNameFile(file.getName()));
      filesJsonArrays.put(item);
    }
    result.put("files", filesJsonArrays);
    return result.toString();
  }

  public static String getHashNameFile(String fileName) {
    return DigestUtils.md5DigestAsHex((fileName).getBytes(StandardCharsets.UTF_8));
  }

  public static String getFileNameWithOutExtension(String fileName) {
    return fileName.replaceFirst("[.][^.]+$", "");
  }

  @RequestMapping(value = "/files/{filePath}/{hashNameFile}", method = RequestMethod.GET)
  public void getFile(@PathVariable("filePath") String filePath, @PathVariable("hashNameFile") String hashNameFile,
      HttpServletResponse response) throws IOException {

    downloadFile(hashNameFile, filePath, response);
  }

  @RequestMapping(value = "/files/all/{filePath}", method = RequestMethod.GET)
  public void downloadAllFiles(@PathVariable("filePath") String filePath, HttpServletResponse response) {

    downloadAllFilesWithZip(filePath, response);

  }

  public static void downloadAllFilesWithZip(String filePath, HttpServletResponse response) {

    File folder = new File(provide.getOutFolder() + filePath + File.separator);
    if (folder.listFiles() == null) {
      System.out.println("\"1\" = " + "1");
      return;
    }

    Objects.requireNonNull(folder.listFiles());

    File file = null;
    try {
      file = zip(Arrays.asList(folder.listFiles()), folder.getAbsolutePath(), "test.zip");
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      System.out.println("\"We start\" = " + "We start");
      InputStream is = new FileInputStream(file);
      String makeString = makeString(file);
      System.out.println("makeString = " + makeString);
      response.addHeader("Content-Disposition", String.format("attachment; filename*=UTF-8''%s", makeString));
      response.setContentLength(is.available());
      ServletOutputStream servletOutputStream = response.getOutputStream();
      IOUtils.copyLarge(is, servletOutputStream);
      response.flushBuffer();
    } catch (IOException ex) {
      System.err.println(String.format("Error writing file to output stream. Filename was '%s'", "test.zip"));
      throw new RuntimeException("IOError writing file to output stream");
    }

  }

  public static File zip(List<File> files, String zipFolderPath, String filename) throws IOException {
    File zipfile = new File(zipFolderPath, filename);
    // Create a buffer for reading the files
    byte[] buf = new byte[1024];
    try {
      // create the ZIP file
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
      // compress the files
      for (int i = 0; i < files.size(); i++) {
        FileInputStream in = new FileInputStream(files.get(i).getAbsolutePath());
        // add ZIP entry to output stream
        out.putNextEntry(new ZipEntry(files.get(i).getName()));
        // transfer bytes from the file to the ZIP file
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
        // complete the entry
        out.closeEntry();
        in.close();
      }
      // complete the ZIP file
      out.close();
      return zipfile;
    } catch (IOException ex) {
      System.err.println(ex.getMessage());
    }
    return null;
  }

  public static void downloadFile(String hashNameFile, String filePath,
      HttpServletResponse response) throws IOException {
    System.out.println("hashNameFile = " + hashNameFile);
    File folder = new File(provide.getOutFolder() + filePath + File.separator);
    if (folder.listFiles() == null) {
      System.out.println("\"1\" = " + "1");
      return;
    }

    Objects.requireNonNull(folder.listFiles());

    for (File file : folder.listFiles()) {
      String localFileHashName = getHashNameFile(file.getName());
      System.out.println("localFileHashName = " + localFileHashName);
      if (localFileHashName.equals(hashNameFile)) {
        //filePath = provide.getOutFolder() + filePath + File.separator + fileName;
        //System.out.println("filePath = " + filePath);
        //File file = new File(filePath);
        String fileName = file.getName();
        if (!file.exists()) {
          response.setStatus(404);
          response.flushBuffer();
          return;
        }
        try {
          System.out.println("\"We start\" = " + "We start");
          InputStream is = new FileInputStream(file);
          String makeString = makeString(file);
          System.out.println("fileName = " + fileName);
          System.out.println("makeString = " + makeString);
          response.addHeader("Content-Disposition", String.format("attachment; filename*=UTF-8''%s", makeString));
          response.setContentLength(is.available());
          ServletOutputStream servletOutputStream = response.getOutputStream();
          IOUtils.copyLarge(is, servletOutputStream);
          response.flushBuffer();
        } catch (IOException ex) {
          System.err.println(String.format("Error writing file to output stream. Filename was '%s'", fileName));
          throw new RuntimeException("IOError writing file to output stream");
        }
      } else {
        System.out.println("file.toString() = " + file.toString());
      }
    }
  }

  public static void downloadFileOnly(File file, HttpServletResponse response) throws IOException {

    String fileName = file.getName();
    if (!file.exists()) {
      response.setStatus(404);
      response.flushBuffer();
      return;
    }
    try {
      System.out.println("\"We start\" = " + "We start");
      InputStream is = new FileInputStream(file);
      String makeString = makeString(file);
      System.out.println("fileName = " + fileName);
      System.out.println("makeString = " + makeString);
      response.addHeader("Content-Disposition", String.format("attachment; filename*=UTF-8''%s", makeString));
      response.setContentLength(is.available());
      ServletOutputStream servletOutputStream = response.getOutputStream();
      IOUtils.copyLarge(is, servletOutputStream);
      response.flushBuffer();
    } catch (IOException ex) {
      System.err.println(String.format("Error writing file to output stream. Filename was '%s'", fileName));
      throw new RuntimeException("IOError writing file to output stream");
    }
  }


  public static String makeString(File file) {
    byte[] bytes = file.getName().getBytes(StandardCharsets.UTF_8);
    StringBuilder sb = new StringBuilder();
    System.out.println("bytes = " + Arrays.toString(bytes));
    for (byte b : bytes) {
      sb.append('%').append(String.format("%02X", b));
    }
    System.out.println(sb.toString());
    return sb.toString();
  }

}
