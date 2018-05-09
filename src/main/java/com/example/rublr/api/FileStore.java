package com.example.rublr.api;

import java.util.Collection;

public interface FileStore {

  Collection<String> listFileNames(String blogName, String contentFolder);

  boolean saveFile(String blogName, String contentFolder, String fileName, byte[] data);

  boolean initializeStore(String blogName, String contentFolder);
}
