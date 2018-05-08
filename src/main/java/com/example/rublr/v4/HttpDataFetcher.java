package com.example.rublr.v4;

import com.example.rublr.api.DataFetcher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class HttpDataFetcher implements DataFetcher {

  @Override
  public byte[] fetch(String fileURL) {
    HttpURLConnection httpConn = null;
    try {
      val url = new URL(fileURL);
      httpConn = (HttpURLConnection) url.openConnection();
      val responseCode = httpConn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (val inputStream = httpConn.getInputStream();
            val outputStream = new ByteArrayOutputStream()) {
          int bytesRead;
          val buffer = new byte[4096];
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
          }
          log.debug("File downloaded from {}", fileURL);
          return outputStream.toByteArray();
        }
      } else {
        log.error("No file to download. Server replied with code: {} ", responseCode);
      }
    } catch (IOException e) {
      log.error("Error while downloading resource", e);
    } finally {
      if (httpConn != null) {
        httpConn.disconnect();
      }
    }
    return new byte[] {};
  }
}
