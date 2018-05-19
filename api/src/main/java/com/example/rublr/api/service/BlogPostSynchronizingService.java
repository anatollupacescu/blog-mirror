package com.example.rublr.api.service;

import com.example.rublr.api.BlogPostFetcher;
import com.example.rublr.api.RecordStore;
import com.example.rublr.api.domain.BlogPost;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@AllArgsConstructor
public class BlogPostSynchronizingService {

  private RecordStore recordStore;
  private BlogPostFetcher client;
  private int stepSize;

  public long syncBlog(String name) {
    Objects.requireNonNull(emptyToNull(name), "Blog name expected");
    val localRecords = recordStore.readRecords(name);
    val currentRecordCount = localRecords.size();
    val availableRecordCount = client.availablePostCount(name);
    long updated = 0;
    if (currentRecordCount < availableRecordCount) {
      log.info("Downloading {} records...", availableRecordCount - currentRecordCount);
      val remoteRecords = fetchPosts(name, currentRecordCount, availableRecordCount);
      log.info("Saving {} new records...", remoteRecords.size());
      updated = recordStore.updateRecords(name, localRecords, remoteRecords);
    } else {
      log.info("No new records found");
    }
    return updated;
  }

  private String emptyToNull(String name) {
    if ("".equals(name)) {
      return null;
    }
    return name;
  }

  private List<BlogPost> fetchPosts(String name, int downloaded, int size) {
    int step = stepSize;
    val result = ImmutableList.<BlogPost>builder();
    int count = 0;
    int remaining = size - downloaded;
    for (int i = remaining; i > 0; i -= 20) {
      int offset = i - 20;
      if (offset < 0) {
        step = step + offset;
        offset = 0;
      }
      Collection<BlogPost> fetched = client.fetchPosts(name, offset, step);
      if (fetched.isEmpty()) {
        break;
      }
      count += fetched.size();
      log.info("Records downloaded {}/{}", count, remaining);
      result.addAll(fetched);
    }
    return result.build();
  }
}
