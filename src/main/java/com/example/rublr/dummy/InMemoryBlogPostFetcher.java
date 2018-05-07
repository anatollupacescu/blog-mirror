package com.example.rublr.dummy;

import com.example.rublr.api.BlogPostFetcher;
import com.example.rublr.domain.BlogPost;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.val;

public class InMemoryBlogPostFetcher implements BlogPostFetcher {

  private final List<BlogPost> posts = Lists.newArrayList();

  @Override
  public int availablePostCount(String name) {
    return posts.size();
  }

  @Override
  public List<BlogPost> fetchPosts(String name, int offset, int step) {
    return posts.stream().skip(offset).limit(step).collect(Collectors.toList());
  }

  public void configureMockPosts(int postCount) {
    posts.clear();
    IntStream.range(0, postCount)
        .mapToObj(this::dummyPost)
        .forEach(posts::add);
  }

  private BlogPost dummyPost(int l) {
    val blogPost = new BlogPost();
    blogPost.setId((long) l);
    return blogPost;
  }
}
