package com.example.rublr.api.service;

import static java.util.stream.Collectors.toList;

import com.example.rublr.api.ContentDownloadingService;
import com.example.rublr.api.DataFetcher;
import com.example.rublr.api.FileStore;
import com.example.rublr.api.RecordStore;
import com.example.rublr.api.domain.BlogPost;
import com.example.rublr.api.domain.Size;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
    initializeFolders(blogName);
    val remainingImages = buildPendingImageMap(blogName, minLikes, minWidth);
    return downloadImages(remainingImages, blogName);
  }

  private void initializeFolders(String blogName) {
    imageStore.initializeStore(blogName, defaultImagesFolderName);
  }

  private Map<String, String> buildPendingImageMap(String blogName, int minLikes, int minWidth) {
    val blogPosts = getBlogPosts(blogName);
    val urls = toFilteredImageUrlCollection(blogPosts, minLikes, minWidth);
    val urlMap = toUrlMap(urls);
    val alreadyDownloaded = getAlreadyDownloadedFileNames(blogName);
    return removeAlreadyDownloaded(urlMap, alreadyDownloaded);
  }

  @Override
  public long getCount(String blogName, int minLikes, int minWidth) {
    initializeFolders(blogName);
    return buildPendingImageMap(blogName, minLikes, minWidth).size();
  }

  private int downloadImages(Map<String, String> filteredFileNamesMap, String blogName) {
    if (filteredFileNamesMap.isEmpty()) {
      return 0;
    }
    val size = new AtomicInteger(filteredFileNamesMap.size());
    val downloaded = new AtomicInteger(0);
    Observable.from(filteredFileNamesMap.entrySet())
        .flatMap(s -> Observable.just(s)
            .subscribeOn(computation)
            .map(e -> saveImageFromUrl(blogName, e.getKey(), e.getValue())))
        .toBlocking()
        .subscribe(isSuccess -> {
          if (isSuccess) {
            val totalDownloaded = downloaded.incrementAndGet();
            log.info("Images downloaded: {}/{}", totalDownloaded, size.get());
          } else {
            size.decrementAndGet();
          }
        });
    return downloaded.get();
  }

  private Map<String, String> removeAlreadyDownloaded(Map<String, String> fileNameToUrl, Collection<String> alreadyDownloaded) {
    alreadyDownloaded.forEach(fileNameToUrl::remove);
    return fileNameToUrl;
  }

  private Map<String, String> toUrlMap(Collection<String> imagesToDownload) {
    return imagesToDownload.stream()
        .collect(Collectors.toMap(this::filenameOnly, Function.identity(), (old, nev) -> old));
  }

  private Collection<String> getAlreadyDownloadedFileNames(String blogName) {
    val fileNames = imageStore.listFileNames(blogName, defaultImagesFolderName);
    return Collections.unmodifiableCollection(fileNames);
  }

  private List<BlogPost> getBlogPosts(String blogName) {
    val fileNames = recordStore.readRecords(blogName);
    return Collections.unmodifiableList(fileNames);
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
    return images.map(Size::getUrl).collect(toList());
  }

  private String filenameOnly(String url) {
    int start = url.lastIndexOf('/') + 1;
    return url.substring(start);
  }
}
