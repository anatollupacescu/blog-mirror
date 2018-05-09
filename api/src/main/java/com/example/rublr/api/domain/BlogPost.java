package com.example.rublr.api.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

}