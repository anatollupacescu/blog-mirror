package com.example.rublr.api;

import com.example.rublr.domain.BlogPost;
import java.util.List;

public interface BlogPostFetcher {

  int availablePostCount(String name);

  List<BlogPost> fetchPosts(String name, int offset, int step);
}
