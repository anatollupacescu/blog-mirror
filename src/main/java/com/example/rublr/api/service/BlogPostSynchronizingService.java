package com.example.rublr.api.service;

import com.example.rublr.api.BlogPostFetcher;
import com.example.rublr.api.RecordStore;
import com.example.rublr.domain.BlogPost;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.Assert;

@Slf4j
public class BlogPostSynchronizingService {

  final RecordStore recordStore;
  final BlogPostFetcher client;

  final int stepSize;

  public BlogPostSynchronizingService(RecordStore recordStore, BlogPostFetcher client, int stepSize) {
    this.recordStore = recordStore;
    this.client = client;
    this.stepSize = stepSize;
  }

  public int syncBlog(String name) {
    Assert.notNull(name, "Blog name expected");
    val localRecords = recordStore.readRecords(name);
    val currentRecordCount = localRecords.size();
    val availableRecordCount = client.availablePostCount(name);
    log.info("Will download {} records...", availableRecordCount - currentRecordCount);
    int updated = 0;
    if (currentRecordCount < availableRecordCount) {
      val remoteRecords = fetchPosts(name, currentRecordCount, availableRecordCount);
      log.info("Saving {} new records...", remoteRecords.size());
      updated = recordStore.updateRecords(name, remoteRecords);
    } else {
      log.info("No new records found");
    }
    return updated;
  }

  public List<BlogPost> fetchPosts(String name, int downloaded, int size) {
    int step = stepSize;
    val result = ImmutableList.<BlogPost>builder();
    for (int i = size - downloaded; i > 0; i -= 20) {
      int offset = i - 20;
      if (offset < 0) {
        step = step + offset;
        offset = 0;
      }
      Collection<BlogPost> fetched = client.fetchPosts(name, offset, step);
      result.addAll(fetched);
    }
    return result.build();
  }
}
