package com.example.rublr.dummy;

import com.example.rublr.api.RecordStore;
import com.example.rublr.api.domain.Blog;
import com.example.rublr.api.domain.BlogPost;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryRecordStore implements RecordStore {

  private final Multimap<String, BlogPost> posts = ArrayListMultimap.create();

  @Override
  public List<BlogPost> readRecords(String name) {
    Preconditions.checkNotNull(name);
    return Collections.unmodifiableList(Lists.newArrayList(posts.get(name)));
  }

  @Override
  public long updateRecords(String name, List<BlogPost> localRecords, List<BlogPost> delta) {
    Preconditions.checkNotNull(name);
    if (posts.putAll(name, delta)) {
      return delta.size();
    }
    return 0;
  }

  @Override
  public List<Blog> listBlogs() {
    return posts.asMap().entrySet().stream()
        .map(entry -> new Blog(entry.getKey(), entry.getValue().size()))
        .collect(Collectors.toList());
  }
}
