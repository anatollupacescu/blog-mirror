package com.example.rublr.v4;

import static java.util.stream.Collectors.toList;

import com.example.rublr.api.RecordStore;
import com.example.rublr.api.domain.Blog;
import com.example.rublr.api.domain.BlogPost;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;
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
    val dir = Paths.get(location);
    try (val stream = Files.newDirectoryStream(dir, "*.json")) {
        return StreamSupport.stream(stream.spliterator(), false)
            .map(path -> new Blog(path.getFileName().toString(), getRecordCount(path)))
            .collect(toList());
    } catch (IOException e) {
      log.error("Could not open file", e);
    }
      return Collections.emptyList();
  }

  private Integer getRecordCount(Path path) {
    return load(path).size();
  }

  private List<BlogPost> load(Path path) {
    try {
      val body = Files.readAllBytes(path);
      val list = reader.<List<BlogPost>>readValue(body);
      return Collections.unmodifiableList(list);
    } catch (IOException e) {
      log.error("Could not deserialize file", e);
    }
    return Collections.emptyList();
  }

  @Override
  public List<BlogPost> readRecords(String name) {
    val blogResource = getJsonFileAtLocation(location, name);
    if (!Files.exists(blogResource)) {
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
      mapper.writeValue(blogResource.toFile(), items);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Path getJsonFileAtLocation(String location, String fileName) {
    return Paths.get(location, fileName + ".json");
  }
}
