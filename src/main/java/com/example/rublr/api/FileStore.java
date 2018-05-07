package com.example.rublr.api;

import java.util.Collection;

public interface FileStore {

  Collection<String> listFileNames(String blogName, String folder);

  boolean saveFile(String blogName, String folder, String fileName, byte[] data);

  boolean init(String blog, String images);
}
