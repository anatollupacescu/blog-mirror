package com.example.rublr.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.example.rublr.api.BlogPostFetcher;
import com.example.rublr.api.RecordStore;
import com.example.rublr.domain.BlogPost;
import com.example.rublr.dummy.InMemoryBlogPostFetcher;
import com.google.common.collect.ImmutableList;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.BeforeStage;
import com.tngtech.jgiven.annotation.Quoted;
import com.tngtech.jgiven.integration.spring.JGivenStage;
import java.util.Collection;
import java.util.List;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;

@JGivenStage
public class BlogPostSynchronizerStage extends Stage<BlogPostSynchronizerStage> {

  @Autowired
  private BlogPostFetcher fetcher;

  @Autowired
  private RecordStore recordStore;

  private BlogPostSynchronizingService service;
  private String blogName;
  private int deltaCount;

  @BeforeStage
  public void setUp() {
    service = new BlogPostSynchronizingService(recordStore, fetcher, 20);
  }

  public BlogPostSynchronizerStage we_have_a_blog(@Quoted String blogName) {
    this.blogName = blogName;
    return self();
  }

  public BlogPostSynchronizerStage it_has_$_local_records(int count) {
    recordStore.updateRecords(blogName, testData(count));
    return self();
  }

  public BlogPostSynchronizerStage it_has_no_local_records() {
    return it_has_$_local_records(0);
  }

  public BlogPostSynchronizerStage the_remote_resource_has_$_posts(int postCount) {
    ((InMemoryBlogPostFetcher) fetcher).configureMockPosts(postCount);
    return self();
  }

  public BlogPostSynchronizerStage the_blog_posts_are_synced() {
    this.deltaCount = service.syncBlog(blogName);
    //act
    return self();
  }

  //assert
  public BlogPostSynchronizerStage a_total_of_$_new_records_downloaded(int numberOfPosts) {
    assertThat(deltaCount, is(equalTo(numberOfPosts)));
    return self();
  }

  public BlogPostSynchronizerStage a_total_of_$_fetches_occured(int count) {
    return self();
  }

  public BlogPostSynchronizerStage the_local_record_store_has_$_posts(int postCount) {
    Collection<BlogPost> localRecords = recordStore.readRecords(blogName);
    assertThat(localRecords, is(notNullValue()));
    assertThat(localRecords.size(), is(equalTo(postCount)));
    return self();
  }

  private List<BlogPost> testData(int count) {
    val posts = ImmutableList.<BlogPost>builder();
    for (int i = 1; i <= count; i++) {
      posts.add(post(i));
    }
    return posts.build();
  }

  private BlogPost post(int i) {
    val post = new BlogPost();
    post.setId((long) i);
    return post;
  }
}
