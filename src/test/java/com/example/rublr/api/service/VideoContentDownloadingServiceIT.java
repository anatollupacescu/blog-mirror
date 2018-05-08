package com.example.rublr.api.service;

import com.tngtech.jgiven.integration.spring.SimpleSpringScenarioTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class VideoContentDownloadingServiceIT extends
    SimpleSpringScenarioTest<VideoContentDownloadingServiceStage> {

  @Test
  public void thereAreNoLocalFilesAllContentIsDownloaded() {
    given()
        .we_have_a_blog_$("emptyBlog").and()
        .it_has_an_empty_content_folder().and()
        .the_remote_resource_has_$_videos(2);

    when()
        .the_blog_is_synced();

    then()
        .the_local_content_folder_has_$_videos(2).and()
        .a_total_of_$_fetches_occured(2);
  }

  @Test
  public void thereIsOneLocalFileOnlyNewContentIsDownloaded() {
    given()
        .we_have_a_blog_$("oneLocalVideoBlog").and()
        .it_has_$_downloaded_videos(1).and()
        .the_remote_resource_has_$_videos(2);

    when()
        .the_blog_is_synced();

    then()
        .the_local_content_folder_has_$_videos(2).and()
        .a_total_of_$_fetches_occured(1);
  }

  @Test
  public void theLocalFolderHasAllRemoteContentNoDownloadsAreMade() {
    given()
        .we_have_a_blog_$("twoLocalVideoBlog").and()
        .it_has_$_downloaded_videos(2).and()
        .the_remote_resource_has_$_videos(2);

    when()
        .the_blog_is_synced();

    then()
        .the_local_content_folder_has_$_videos(2).and()
        .a_total_of_$_fetches_occured(0);
  }
}
