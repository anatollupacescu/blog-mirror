package com.example.rublr.dummy;

import com.example.rublr.api.FileStore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;

public class InMemoryFileStore implements FileStore {

  private final Multimap<String, String> store = ArrayListMultimap.create();

  @Override
  public Collection<String> listFileNames(String blogName, String folder) {
    return store.get(blogName + folder);
  }

  @Override
  public boolean saveFile(String blogName, String folder, String fileName, byte[] data) {
    return store.get(blogName + folder).add(fileName);
  }

  @Override
  public boolean init(String blog, String images) {
    return true;
  }
}
