package com.example.rublr.v4;

import static java.util.stream.Collectors.toList;

import com.example.rublr.api.RecordStore;
import com.example.rublr.domain.Blog;
import com.example.rublr.domain.BlogPost;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
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
      return reader.readValue(file);
    } catch (IOException e) {
      log.error("Could not deserialize file {} because {}", file.getName(), e.getMessage());
    }
    return Collections.emptyList();
  }

  @Override
  public List<BlogPost> readRecords(String name) {
    val blogResource = getJsonFileAtLocation(location, name);
    return load(blogResource);
  }

  @Override
  public int updateRecords(String name, List<BlogPost> delta) {
    val actualRecords = readRecords(name);
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
    return new File(String.format("%s/%s.json", location, fileName));
  }
}
