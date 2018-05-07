package com.example.rublr.v4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import lombok.val;
import org.junit.Before;
import org.junit.Test;

public class LocalFileStoreTest {

  private LocalFileStore store;

  @Before
  public void setUp() {
    store = new LocalFileStore("target/");
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenUninitializedThrowsException() {
    store.listFileNames("blog", "images");
  }

  @Test
  public void whenCreatedIsEmpty() {
    val blogName = "blog";
    val initialized = store.init(blogName, "images");
    assertThat(initialized, is(equalTo(true)));
    val files = store.listFileNames("blog", "images");
    assertThat(files, is(notNullValue()));
    assertThat(files.isEmpty(), is(equalTo(true)));
  }

  @Test
  public void canSaveFile() {
    val blogName = "blog2";
    store.init(blogName, "images");
    val saved = store.saveFile(blogName, "images", "file", new byte[]{1});
    assertThat(saved, is(equalTo(true)));
    val files = store.listFileNames(blogName, "images");
    assertThat(files, is(notNullValue()));
    files.forEach(System.out::println);
    assertThat(files.isEmpty(), is(equalTo(false)));
  }
}