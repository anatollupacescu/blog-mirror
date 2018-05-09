package com.example.rublr.v4;

import static java.util.stream.Collectors.toSet;

import com.example.rublr.api.FileStore;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.logging.log4j.util.Strings;

@Slf4j
public class LocalFileStore implements FileStore {

  private final String rootFolder;

  public LocalFileStore(String rootFolder) {
    this.rootFolder = rootFolder;
  }

  @Override
  public boolean init(String blogName, String folder) {
    val folderPath = getFolderPath(blogName, folder);
    try {
      Files.createDirectories(folderPath);
      return true;
    } catch (IOException e) {
      log.error("Could not create directories", e);
    }
    return false;
  }

  @Override
  public Set<String> listFileNames(String blogName, String folder) {
    val directory = getFolderPath(blogName, folder);
    ensureDirectoryExists(directory);
    return fileList(directory);
  }

  private void ensureDirectoryExists(Path directory) {
    if (Files.notExists(directory)) {
      throw new IllegalStateException("File store not initialized");
    }
  }

  public static Set<String> fileList(Path directory) {
    try (Stream<Path> directoryStream = Files.list(directory)) {
      return directoryStream.map(Path::getFileName).map(Path::toString).collect(toSet());
    } catch (IOException ex) {
      log.error("Could not list directory {} because: {}", directory, ex.getMessage());
    }
    return Collections.emptySet();
  }

  private Path getFolderPath(String blogName, String folder) {
    assertAllNotEmpty(blogName, folder);
    return Paths.get(rootFolder, blogName, folder);
  }

  private Path getFilePath(String blogName, String folder, String fileName) {
    assertAllNotEmpty(fileName);
    return Paths.get(getFolderPath(blogName, folder).toString(), fileName);
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
      val file = new File(destination.toUri());
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
