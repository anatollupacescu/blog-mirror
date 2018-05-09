package com.example.rublr.v4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.val;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalFileStoreTest {

  final String root = "target";
  final List<Path> paths = Lists.newArrayList();

  private LocalFileStore store;

  @Before
  public void setUp() {
    store = new LocalFileStore(root);
  }

  @After
  public void tearDown() {
    paths.forEach(p -> {
      try {
        Files.deleteIfExists(p);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  @Test(expected = IllegalStateException.class)
  public void whenUninitializedThrowsException() {
    store.listFileNames("blog", "images");
  }

  @Test
  public void whenCreatedIsEmpty() {
    val blogName = "blog";
    String images = "images";
    val initialized = store.init(blogName, images);
    assertThat(initialized, is(equalTo(true)));
    val files = store.listFileNames("blog", images);
    assertThat(files, is(notNullValue()));
    assertThat(files.isEmpty(), is(equalTo(true)));
    //cleanup
    paths.add(Paths.get(root, blogName, images));
    paths.add(Paths.get(root, blogName));
  }

  @Test
  public void canSaveFile() {
    val blogName = "blog2";
    val images = "images";
    val file = "file";
    store.init(blogName, images);
    val saved = store.saveFile(blogName, images, file, new byte[]{1});
    assertThat(saved, is(equalTo(true)));
    val files = store.listFileNames(blogName, images);
    assertThat(files, is(notNullValue()));
    assertThat(files.isEmpty(), is(equalTo(false)));
    //cleanup
    paths.add(Paths.get(root, blogName, images, file));
    paths.add(Paths.get(root, blogName, images));
    paths.add(Paths.get(root, blogName));
  }
}