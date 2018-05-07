package com.example.rublr.api.service;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.rublr.api.DataFetcher;
import com.example.rublr.api.FileStore;
import com.example.rublr.api.RecordStore;
import com.example.rublr.domain.BlogPost;
import com.example.rublr.domain.Photo;
import com.example.rublr.domain.Video;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.BeforeScenario;
import com.tngtech.jgiven.annotation.Quoted;
import com.tngtech.jgiven.integration.spring.JGivenStage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.val;
import org.assertj.core.util.Lists;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

@JGivenStage
public class ContentSynchronizerStage extends Stage<ContentSynchronizerStage> {

  @Autowired
  RecordStore recordStore;

  @Autowired
  FileStore videoStore;

  private DataFetcher dataFetcher;
  private VideoContentDownloadingService videoDownloadingService;
  private String blogName;

  final String defaultVideoFolderName = "vids";

  @BeforeScenario
  public void setUp() {
    dataFetcher = mock(DataFetcher.class);
    videoDownloadingService = new VideoContentDownloadingService(recordStore, videoStore,
        dataFetcher, defaultVideoFolderName);
  }

  public ContentSynchronizerStage we_have_a_blog_$(@Quoted String givenBlogName) {
    this.blogName = givenBlogName;
    return self();
  }

  public ContentSynchronizerStage it_has_$_downloaded_videos(int count) {
    for (int i = 1; i <= count; i++) {
      videoStore.saveFile(blogName, defaultVideoFolderName, "tumblr_video" + i, new byte[]{0});
    }
    return self();
  }

  public ContentSynchronizerStage the_local_content_folder_has_$_videos(int i) {
    Collection<String> localFiles = videoStore.listFileNames(blogName, defaultVideoFolderName);
    assertThat(localFiles, is(notNullValue()));
    assertThat(localFiles.size(), is(equalTo(i)));
    return self();
  }

  public ContentSynchronizerStage the_remote_resource_has_$_videos(int remoteVideosCount) {
    recordStore.updateRecords(blogName, testData(2));
    Collection<BlogPost> blogPosts = recordStore.readRecords(blogName);
    assertThat(blogPosts, is(notNullValue()));
    assertThat(blogPosts.size(), is(equalTo(remoteVideosCount)));
    return self();
  }

  public ContentSynchronizerStage the_blog_is_synced() {
    Mockito.when(dataFetcher.fetch(anyString())).thenReturn(new byte[]{0});
    videoDownloadingService.download(blogName, 0, 0);
    return self();
  }

  public ContentSynchronizerStage a_total_of_$_fetches_occured(int i) {
    verify(dataFetcher, times(i)).fetch(anyString());
    return self();
  }

  public ContentSynchronizerStage it_has_an_empty_content_folder() {
    return it_has_$_downloaded_videos(0);
  }

  /*
  testing rig
   */

  private List<BlogPost> testData(int count) {
    val videoBlogPost = new BlogPost();
    val videos = Lists.<Video>newArrayList();
    for (int i = 1; i <= count; i++) {
      videos.add(newVideo(i));
    }
    videoBlogPost.setVideos(Collections.unmodifiableList(videos));
    val imageBlogPost = new BlogPost();
    Photo photo = new Photo();
    photo.setCaption("tumblr_photo");
    photo.setSizes(Collections.emptyList());
    imageBlogPost.setPhotos(Collections.singletonList(photo));
    val testData = Lists.newArrayList(videoBlogPost, imageBlogPost);
    return Collections.unmodifiableList(testData);
  }

  private Video newVideo(int i) {
    val video = new Video();
    video.setWidth(i);
    video.setEmbedCode("<video src=\"http://location/tumblr_video" + i + "\" type=video>");
    return video;
  }
}
