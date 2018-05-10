package com.example.rublr.api.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.example.rublr.api.ContentDownloadingService;
import com.example.rublr.api.DataFetcher;
import com.example.rublr.api.FileStore;
import com.example.rublr.api.RecordStore;
import com.example.rublr.api.domain.BlogPost;
import com.example.rublr.api.domain.Video;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@AllArgsConstructor
public class VideoContentDownloadingService implements ContentDownloadingService {

  private RecordStore recordStore;
  private FileStore localFileStore;
  private DataFetcher fetcher;
  private String defaultVideosFolderName;

  @Override
  public long download(String blogName, int minLikes, int minWidth) {
    initFolders(blogName);
    val videoUrlMap = buildPendingForDownloadFiles(blogName, minLikes, minWidth);
    return downloadVideos(blogName, videoUrlMap);
  }

  private Map<String,String> buildPendingForDownloadFiles(String blogName, int minLikes, int minWidth) {
    val blogPostStream = getVideoBlogPosts(blogName);
    val filteredVideosStream = applyFilters(blogPostStream, minLikes, minWidth);
    val urlStream = extractUrls(filteredVideosStream);
    val fileNameUrlMap = buildFileNameToUrlMap(urlStream);
    val downloadedVideos = getDownloadedFiles(blogName);
    return removeAlreadyDownloaded(fileNameUrlMap, downloadedVideos);
  }

  private Stream<Video> applyFilters(Stream<BlogPost> blogPostStream, int minLikes, int minWidth) {
    val filteredBlogPostStream = applyMinLikesFilter(blogPostStream, minLikes);
    val videoStream = toVideoStream(filteredBlogPostStream);
    return applyMinWidthFilter(videoStream, minWidth);
  }

  @Override
  public long getCount(String blogName, int minLikes, int minWidth) {
    val filteredFileNameToUrlMap = buildPendingForDownloadFiles(blogName, minLikes, minWidth);
    return filteredFileNameToUrlMap.size();
  }

  private void initFolders(String blogName) {
    localFileStore.initializeStore(blogName, defaultVideosFolderName);
  }

  private long downloadVideos(String blogName, Map<String, String> filteredFileNameToUrlMap) {
    if (filteredFileNameToUrlMap.isEmpty()) {
      return 0;
    }
    val size = new AtomicInteger(filteredFileNameToUrlMap.size());
    log.info("Downloading {} videos", size.get());
    val counter = new AtomicInteger(0);
    return filteredFileNameToUrlMap.entrySet().stream()
        .map(e -> downloadVideoAndUpdateCounters(blogName, e, counter, size))
        .filter(Boolean.TRUE::equals)
        .count();
  }

  private boolean downloadVideoAndUpdateCounters(String blogName, Entry<String, String> e,
      AtomicInteger counter, AtomicInteger size) {
    val result = safelyDownload(blogName, e.getKey(), e.getValue());
    if (result) {
      val downloaded = counter.incrementAndGet();
      log.info("Videos downloaded: {}/{}", downloaded, size.get());
    } else {
      size.decrementAndGet();
    }
    return result;
  }

  private boolean safelyDownload(String blogName, String fileName, String url) {
    try {
      return download(url, blogName, fileName);
    } catch (Exception e) {
      log.error("Could not download video at '{}' because: ", fileName, e);
    }
    return false;
  }

  private boolean download(String url, String blogName, String fileName) {
    val videoData = fetcher.fetch(url);
    return localFileStore.saveFile(blogName, defaultVideosFolderName, fileName, videoData);
  }

  private Map<String, String> removeAlreadyDownloaded(Map<String, String> fileNameToUrlMap,
      Set<String> downloadedVideos) {
    downloadedVideos.forEach(fileName -> {
      if (fileNameToUrlMap.remove(fileName) != null) {
        log.info("Skipping already downloaded file {}", fileName);
      }
    });
    return fileNameToUrlMap;
  }

  private Map<String, String> buildFileNameToUrlMap(Stream<String> videoUrlStream) {
    return videoUrlStream
        .filter(Objects::nonNull)
        .filter(url -> url.contains("/tumblr_"))
        .collect(toMap(this::toFileName, identity(), (n, o) -> n));
  }

  private String toFileName(String url) {
    int start = url.lastIndexOf("/tumblr_") + 1;
    int end = url.lastIndexOf('/');
    if (end <= start) {
      end = url.length();
    }
    return url.substring(start, end);
  }

  private Stream<String> extractUrls(Stream<Video> videoPostsStream) {
    return videoPostsStream.map(Video::getEmbedCode).map(this::toUrl);
  }

  private Set<String> getDownloadedFiles(String blogName) {
    return Sets.newHashSet(localFileStore.listFileNames(blogName, defaultVideosFolderName));
  }

  private Stream<Video> toVideoStream(Stream<BlogPost> blogPostStream) {
    return blogPostStream
        .filter(post -> post.getVideos() != null && !post.getVideos().isEmpty())
        .flatMap(post -> post.getVideos().stream());
  }

  private Stream<Video> applyMinWidthFilter(Stream<Video> videoPostSteam, int minWidth) {
    if (minWidth > 0) {
      return videoPostSteam.filter(video -> video.getWidth() >= minWidth);
    }
    return videoPostSteam;
  }

  private Stream<BlogPost> getVideoBlogPosts(String blogName) {
    return recordStore.readRecords(blogName).stream()
        .filter(post -> "video".equals(post.getType()));
  }

  private Stream<BlogPost> applyMinLikesFilter(Stream<BlogPost> blogPostStream, int minLikes) {
    if (minLikes > 0) {
      return blogPostStream.filter(post -> post.getNoteCount() >= minLikes);
    }
    return blogPostStream;
  }

  private String toUrl(String s) {
    if (s == null) {
      return null;
    }
    if (s.contains("<video")) {
      String start = "src=\"";
      int from = s.indexOf(start) + start.length();
      int to = s.indexOf("\" type=", from);
      return s.substring(from, to);
    }
    return null;
  }
}
