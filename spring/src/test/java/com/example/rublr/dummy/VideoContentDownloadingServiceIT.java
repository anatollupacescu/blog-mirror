package com.example.rublr.dummy;

import com.tngtech.jgiven.integration.spring.SimpleSpringScenarioTest;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class VideoContentDownloadingServiceIT extends
    SimpleSpringScenarioTest<VideoContentDownloadingServiceStage> {

  @Test
  public void whenLocalStoreIsEmptyPendingCountIsEqualToTotalCount() {
    given()
        .we_have_a_blog_$("emptyBlog1").and()
        .it_has_an_empty_content_folder().and()
        .the_remote_resource_has_$_videos(2);

    when()
        .the_pending_count_is_inquired(0, 0);

    then()
        .the_local_content_folder_has_$_videos(0).and()
        .the_pending_count_is_$(2);
  }

  @Test
  public void whenLocalStoreContainsPartialContentCountEqualsDelta() {
    val localVideoCount = 1;
    given()
        .we_have_a_blog_$("oneLocalVideoBlogForCount").and()
        .it_has_$_downloaded_videos(localVideoCount).and()
        .the_remote_resource_has_$_videos(2);

    when()
        .the_pending_count_is_inquired(0, 0);

    then()
        .the_local_content_folder_has_$_videos(localVideoCount).and()
        .the_pending_count_is_$(1);
  }

  @Test
  public void thereAreNoLocalFilesAllContentIsDownloaded() {
    given()
        .we_have_a_blog_$("emptyBlog2").and()
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
