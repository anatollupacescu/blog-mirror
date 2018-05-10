package com.example.rublr.dummy;

import com.tngtech.jgiven.integration.spring.SimpleSpringScenarioTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class BlogPostSynchronizingServiceIT extends SimpleSpringScenarioTest<BlogPostSynchronizerStage> {

  @Test
  public void forNewBlogMirrorWillDownloadAllBlogPosts() {

    int postCount = 21;

    given()
        .we_have_a_blog("blogName1").and()
        .it_has_no_local_records().and()
        .the_remote_resource_has_$_posts(postCount);

    when()
        .the_blog_posts_are_synced();

    then()
        .a_total_of_$_fetches_occured(2).and()
        .a_total_of_$_new_records_downloaded(postCount).and()
        .the_local_record_store_has_$_posts(postCount);
  }

  @Test
  public void noFetchesOccurWhenBlogMirrorIsCompletelySynced() {
    int postCount = 3;

    given()
        .we_have_a_blog("blogName2").and()
        .it_has_$_local_records(postCount).and()
        .the_remote_resource_has_$_posts(postCount);

    when()
        .the_blog_posts_are_synced();

    then()
        .a_total_of_$_fetches_occured(0).and()
        .a_total_of_$_new_records_downloaded(0).and()
        .the_local_record_store_has_$_posts(postCount);
  }

  @Test
  public void postsAreDownloadedInBatches() {
    given()
        .we_have_a_blog("blogName3").and()
        .it_has_no_local_records().and()
        .the_remote_resource_has_$_posts(21);

    when()
        .the_blog_posts_are_synced();

    then()
        .a_total_of_$_fetches_occured(2).and()
        .a_total_of_$_new_records_downloaded(21).and()
        .the_local_record_store_has_$_posts(21);
  }

  @Test
  public void onlyDeltasAreDownloaded() {
    given()
        .we_have_a_blog("blogName4").and()
        .it_has_$_local_records(15).and()
        .the_remote_resource_has_$_posts(20);

    when()
        .the_blog_posts_are_synced();

    then()
        .a_total_of_$_fetches_occured(1).and()
        .a_total_of_$_new_records_downloaded(5).and()
        .the_local_record_store_has_$_posts(20);
  }
}
