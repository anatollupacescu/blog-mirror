package com.example.rublr.api;

import com.example.rublr.domain.Blog;
import com.example.rublr.domain.BlogPost;
import java.util.List;

public interface RecordStore {

  List<BlogPost> readRecords(String name);

  int updateRecords(String name, List<BlogPost> delta);

  List<Blog> listBlogs();
}
