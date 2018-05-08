package com.example.rublr.v4;

import static java.util.stream.Collectors.toList;

import com.example.rublr.api.BlogPostFetcher;
import com.example.rublr.domain.BlogPost;
import com.google.common.collect.ImmutableMap;
import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.Blog;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.val;

@AllArgsConstructor
public class TumblrBlogPostFetcher implements BlogPostFetcher {

  private final JumblrClient client;

  @Override
  public int availablePostCount(String name) {
    return getBlogInfo(name).getPostCount();
  }

  public Blog getBlogInfo(String name) {
    Objects.requireNonNull(name);
    return client.blogInfo(name);
  }

  @Override
  public List<BlogPost> fetchPosts(String name, int offset, int step) {
    Objects.requireNonNull(name);
    val options = requestMap(offset, step);
    return client.blogPosts(name, options).stream()
        .map(BlogPost::new)
        .collect(toList());
  }

  private Map<String, Object> requestMap(int offset, int step) {
    return ImmutableMap.of("offset", offset, "limit", step);
  }
}
