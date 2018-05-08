package com.example.rublr.domain;

import com.tumblr.jumblr.types.Note;
import com.tumblr.jumblr.types.PhotoPost;
import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.VideoPost;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPost {

  private boolean isPhotoset;
  private Long id;
  private String type;
  private String rebloggedFromName;
  private long noteCount;
  private List<Photo> photos;
  private List<Video> videos;
  private Set<String> notes = new HashSet<>();

  public BlogPost(Post post) {
    this.id = post.getId();
    this.type = post.getType();
    this.rebloggedFromName = post.getRebloggedFromName();
    this.noteCount = post.getNoteCount();
    if (post instanceof PhotoPost) {
      this.photos = mapPhotos(((PhotoPost) post).getPhotos());
      this.isPhotoset = ((PhotoPost) post).isPhotoset();
    } else if (post instanceof VideoPost) {
      this.videos = mapVideos(((VideoPost) post).getVideos());
    }
    List<Note> postNotes = post.getNotes();
    if (postNotes != null && !postNotes.isEmpty()) {
      this.notes.addAll(postNotes.stream().map(Note::getBlogName).collect(Collectors.toSet()));
    }
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
}