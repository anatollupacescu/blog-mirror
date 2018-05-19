package com.example.rublr.v4;

import static java.util.stream.Collectors.toList;

import com.example.rublr.api.BlogPostFetcher;
import com.example.rublr.api.domain.BlogPost;
import com.example.rublr.api.domain.Photo;
import com.example.rublr.api.domain.Size;
import com.example.rublr.api.domain.Video;
import com.google.common.collect.ImmutableMap;
import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.Note;
import com.tumblr.jumblr.types.PhotoPost;
import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.VideoPost;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@AllArgsConstructor
@Slf4j
public class TumblrBlogPostFetcher implements BlogPostFetcher {

  private final JumblrClient client;

  @Override
  public int availablePostCount(String name) {
    Objects.requireNonNull(name);
    val blogInfo = client.blogInfo(name);
    return blogInfo.getPostCount();
  }

  @Override
  public List<BlogPost> fetchPosts(String name, int offset, int step) {
    Objects.requireNonNull(name);
    val options = requestMap(offset, step);
    return fetchFromRemote(name, options);
  }

  private List<BlogPost> fetchFromRemote(String name, Map<String, Object> options) {
    try {
    return client.blogPosts(name, options).stream()
        .map(this::toBlogPost)
        .collect(toList());
    } catch (Exception e) {
      log.error("Could not fetch posts from remote", e);
    }
    return Collections.emptyList();
  }

  private BlogPost toBlogPost(Post post) {
    BlogPost blogPost = new BlogPost();
    blogPost.setId(post.getId());
    blogPost.setType(post.getType());
    blogPost.setRebloggedFromName(post.getRebloggedFromName());
    blogPost.setNoteCount(post.getNoteCount());
    if (post instanceof PhotoPost) {
      blogPost.setPhotos(mapPhotos(((PhotoPost) post).getPhotos()));
      blogPost.setPhotoset(((PhotoPost) post).isPhotoset());
    } else if (post instanceof VideoPost) {
      blogPost.setVideos(mapVideos(((VideoPost) post).getVideos()));
    }
    List<Note> postNotes = post.getNotes();
    if (postNotes != null && !postNotes.isEmpty()) {
      blogPost.setNotes(postNotes.stream().map(Note::getBlogName).collect(Collectors.toSet()));
    }
    return blogPost;
  }

  private List<Video> mapVideos(List<com.tumblr.jumblr.types.Video> videos) {
    return videos.stream().map(v -> {
      Video video = new Video();
      video.setWidth(v.getWidth());
      video.setEmbedCode(v.getEmbedCode());
      return video;
    }).collect(Collectors.toList());
  }

  private List<Photo> mapPhotos(List<com.tumblr.jumblr.types.Photo> photos) {
    List<Photo> photoList = new ArrayList<>(photos.size());
    photos.forEach(p -> photoList.add(mapSizes(p)));
    return photoList;
  }

  private Photo mapSizes(com.tumblr.jumblr.types.Photo p) {
    List<Size> sizes = p.getSizes().stream().map(s -> {
      Size size = new Size();
      size.setHeight(s.getHeight());
      size.setWidth(s.getWidth());
      size.setUrl(s.getUrl());
      return size;
    }).collect(Collectors.toList());
    Photo photo = new Photo();
    photo.setCaption(p.getCaption());
    photo.setSizes(sizes);
    return photo;
  }

  private Map<String, Object> requestMap(int offset, int step) {
    return ImmutableMap.of("offset", offset, "limit", step);
  }
}
