package com.example.rublr.api.service;

import com.example.rublr.api.ContentDownloadingService;
import com.example.rublr.api.DataFetcher;
import com.example.rublr.api.FileStore;
import com.example.rublr.api.RecordStore;
import com.example.rublr.domain.BlogPost;
import com.example.rublr.domain.Size;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import rx.Observable;
import rx.Scheduler;

@Slf4j
@AllArgsConstructor
public class ImageContentDownloadingService implements ContentDownloadingService {

  private Scheduler computation;
  private FileStore imageStore;
  private RecordStore recordStore;
  private DataFetcher dataFetcher;
  private String defaultImagesFolderName;

  @Override
  public long download(String blogName, int minLikes, int minWidth) {
	  imageStore.init(blogName, defaultImagesFolderName);
    val blogPosts = getBlogPosts(blogName);
    return Optional.of(blogPosts)
		.map(posts -> toFilteredImageUrlCollection(posts, minLikes, minWidth))
		.map(this::buildPendingImagesMap)
		.map(fileNameToUrl -> {
			val alreadyDownloaded = getAlreadyDownloadedFileNames(blogName);
			return filterAlreadyDownloaded(fileNameToUrl, alreadyDownloaded);	
		})
		.map(filteredFileNamesMap -> downloadImages(filteredFileNamesMap, blogName))
		.get();
  }

  private int downloadImages(Map<String, String> filteredFileNamesMap, String blogName) {
    if (filteredFileNamesMap.isEmpty()) {
      return 0;
    }
    val size = filteredFileNamesMap.size();
    val latch = new CountDownLatch(size);
    Observable.from(filteredFileNamesMap.entrySet())
        .flatMap(s -> Observable.just(s)
            .subscribeOn(computation)
            .map(e -> saveImageFromUrl(blogName, e.getKey(), e.getValue())))
        .subscribe(isSuccess -> {
          if (isSuccess) {
            latch.countDown();
            log.info("Images downloaded: {}/{}", size - latch.getCount(), size);
          }
        });

    waitAllDownloads(latch);
    return size;
  }

  private void waitAllDownloads(CountDownLatch latch) {
    try {
      val remaining = latch.getCount();
      latch.await(remaining, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, String> filterAlreadyDownloaded(Map<String, String> fileNameToUrl,
      Collection<String> alreadyDownloaded) {
    alreadyDownloaded.forEach(fileNameToUrl::remove);
    return fileNameToUrl;
  }

  private Map<String, String> buildPendingImagesMap(Collection<String> imagesToDownload) {
    return imagesToDownload.stream()
        .collect(Collectors.toMap(this::filenameOnly, Function.identity(), (old, nev) -> old));
  }

  private Collection<String> getAlreadyDownloadedFileNames(String blogName) {
    return Collections
        .unmodifiableCollection(imageStore.listFileNames(blogName, defaultImagesFolderName));
  }

  private List<BlogPost> getBlogPosts(String blogName) {
    return Collections.unmodifiableList(recordStore.readRecords(blogName));
  }

  private boolean saveImageFromUrl(String blogName, String fileName, String url) {
    val contents = dataFetcher.fetch(url);
    return imageStore.saveFile(blogName, defaultImagesFolderName, fileName, contents);
  }

  private Collection<String> toFilteredImageUrlCollection(List<BlogPost> blogPosts, int minLikes,
      int minWidth) {
    val images = blogPosts.stream()
        .filter(post -> "photo".equals(post.getType()))
        .filter(o -> o.getNoteCount() >= minLikes)
        .flatMap(post -> post.getPhotos().stream())
        .flatMap(photo -> photo.getSizes().stream())
        .filter(o -> o.getWidth() >= minWidth);
    return images.map(Size::getUrl).collect(Collectors.toSet());
  }

  private String filenameOnly(String url) {
    int start = url.lastIndexOf("/") + 1;
    return url.substring(start);
  }
}
