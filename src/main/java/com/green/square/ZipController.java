package com.green.square;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class ZipController {

  public File convertFilesToZip(List<File> files, String zipFolderPath, String filename, Logger logger) {
    File zipFile = new File(zipFolderPath, filename);
    zipFile.deleteOnExit();

    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));) {
      for (File file : files) {
        final ZipEntry zipEntry = new ZipEntry(file.getName());
        out.putNextEntry(zipEntry);
        byte[] bytesFromFile = Files.readAllBytes(file.toPath());
        out.write(bytesFromFile);
        out.closeEntry();
      }
      out.close();
      return zipFile;
    } catch (IOException ex) {
      logger.error(ex.getMessage(), ex);
      ex.printStackTrace();
    }

    return null;
  }

  public void sendFileToHttpResponse(File file, HttpServletResponse response, Logger logger) {

    try {
      logger.info("SendFileToHttpResponse " + file.getAbsolutePath());
      try (InputStream is = new FileInputStream(file)) {
        String makeString = makeString(file, logger);
        logger.info("makeString = " + makeString);
        response.addHeader("Content-Disposition", String.format("attachment; filename*=UTF-8''%s", makeString));
        response.setContentLength(is.available());
        try (ServletOutputStream servletOutputStream = response.getOutputStream()) {
          IOUtils.copyLarge(is, servletOutputStream);
        }
      }
      response.flushBuffer();
    } catch (IOException ex) {
      logger.error(String.format("Error writing file to output stream. Filename was '%s'", file.getAbsolutePath()),
          ex);
    }
  }

  private String makeString(File file, Logger logger) {
    byte[] bytes = file.getName().getBytes(StandardCharsets.UTF_8);
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append('%').append(String.format("%02X", b));
    }
    logger.info(sb.toString());
    return sb.toString();
  }

}
