package com.example.rublr.v4;

import com.example.rublr.api.FileStore;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.logging.log4j.util.Strings;

@Slf4j
public class LocalFileStore implements FileStore {

  private final String slash = "/";
  private final String rootFolder;

  public LocalFileStore(String s) {
    this.rootFolder = s.endsWith(slash) ? s : s + slash;
  }

  @Override
  public boolean init(String blogName, String folder) {
    val folderPath = getFolderPath(blogName, folder);
    return new File(folderPath).mkdirs();
  }

  @Override
  public Set<String> listFileNames(String blogName, String folder) {
    val destination = getFolderPath(blogName, folder);
    File file = new File(destination);
    Preconditions.checkArgument(file.exists(), "Destination folder not found");
    File[] files = file.listFiles();
    if (files == null) {
      return Collections.emptySet();
    }
    return Arrays.stream(files)
        .map(File::getName)
        .collect(Collectors.toSet());
  }

  private String getFolderPath(String blogName, String folder) {
    assertAllNotEmpty(blogName, folder);
    return String.format("%s%s/%s/", rootFolder, blogName, folder);
  }

  private String getFilePath(String blogName, String folder, String fileName) {
    assertAllNotEmpty(fileName);
    return String.format("%s/%s", getFolderPath(blogName, folder), fileName);
  }

  private void assertAllNotEmpty(String... strings) {
    Arrays.stream(strings).forEach(s -> Preconditions.checkArgument(Strings.isNotEmpty(s)));
  }

  @Override
  public boolean saveFile(String blogName, String folder, String fileName, byte[] data) {
    if (data.length == 0) {
      log.warn("Empty body, will not save");
    } else {
      val destination = getFilePath(blogName, folder, fileName);
      val file = new File(destination);
      try (OutputStream out = new FileOutputStream(file)) {
        out.write(data);
        return true;
      } catch (IOException e) {
        log.error("Could not save file {} to location {} because: ", fileName, folder,
            e.getMessage());
      }
    }
    return false;
  }
}
