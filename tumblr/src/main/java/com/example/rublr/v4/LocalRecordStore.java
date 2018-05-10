package com.example.rublr.v4;

import static java.util.stream.Collectors.toList;

import com.example.rublr.api.RecordStore;
import com.example.rublr.api.domain.Blog;
import com.example.rublr.api.domain.BlogPost;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class LocalRecordStore implements RecordStore {

  private final String location;
  private final ObjectReader reader;
  private final ObjectMapper mapper;

  public LocalRecordStore(String location) {
    this.location = location;
    this.mapper = new ObjectMapper();
    val type = mapper.getTypeFactory().constructCollectionType(List.class, BlogPost.class);
    this.reader = mapper.readerFor(type).without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @Override
  public List<Blog> listBlogs() {
    val localFolder = new File(location);
    val files = localFolder.listFiles();
    if (files == null) {
      return Collections.emptyList();
    }
    return Arrays.stream(files)
        .filter(file -> file.getName().endsWith(".json"))
        .map(file -> new Blog(file.getName(), getRecordCount(file)))
        .collect(toList());
  }

  private Integer getRecordCount(File file) {
    return load(file).size();
  }

  private List<BlogPost> load(File file) {
    try {
      return Collections.unmodifiableList(reader.readValue(file));
    } catch (IOException e) {
      log.error("Could not deserialize file {} because {}", file.getName(), e.getMessage());
    }
    return Collections.emptyList();
  }

  @Override
  public List<BlogPost> readRecords(String name) {
    val blogResource = getJsonFileAtLocation(location, name);
    if (!blogResource.exists()) {
      log.info("Blog {} was not found locally", name);
      return Collections.emptyList();
    }
    return load(blogResource);
  }

  @Override
  public long updateRecords(String name, List<BlogPost> actualRecords, List<BlogPost> delta) {
    val newList = ImmutableList.<BlogPost>builder();
    newList.addAll(actualRecords);
    newList.addAll(delta);
    save(name, newList.build());
    return delta.size();
  }

  private void save(String name, List<BlogPost> items) {
    try {
      val blogResource = getJsonFileAtLocation(location, name);
      mapper.writeValue(blogResource, items);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private File getJsonFileAtLocation(String location, String fileName) {
    val path = Paths.get(location, fileName + ".json");
    return path.toFile();
  }
}
