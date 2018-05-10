package com.example.rublr.api;

import com.example.rublr.api.domain.Blog;
import com.example.rublr.api.domain.BlogPost;
import java.util.List;

public interface RecordStore {

  List<BlogPost> readRecords(String name);

  long updateRecords(String name, List<BlogPost> localRecords, List<BlogPost> delta);

  List<Blog> listBlogs();
}
