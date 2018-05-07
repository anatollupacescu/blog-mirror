package com.example.rublr.api.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.example.rublr.api.ContentDownloadingService;
import com.example.rublr.api.DataFetcher;
import com.example.rublr.api.FileStore;
import com.example.rublr.api.RecordStore;
import com.example.rublr.domain.BlogPost;
import com.example.rublr.domain.Video;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@AllArgsConstructor
@Slf4j
public class VideoContentDownloadingService implements ContentDownloadingService {

  private RecordStore recordStore;
  private FileStore localFileStore;
  private DataFetcher fetcher;
  private String defaultVideosFolderName;

  @Override
  public long download(String blogName, int minLikes, int minWidth) {
    initFolders(blogName);
    val blogPostStream = getFilteredRecordsStream(blogName, minLikes);
    val videoPostsStream = getFilteredVideosStream(blogPostStream, minWidth);
    val videoUrlStream = toUrlStream(videoPostsStream);
    val downloadedVideos = getDownloadedImagesFileNameList(blogName);
    val fileNameToUrlMap = buildFileNameToUrlMap(videoUrlStream);
    val filteredFileNameToUrlMap = filterAlreadyDownloaded(fileNameToUrlMap, downloadedVideos);
    return downloadVideos(filteredFileNameToUrlMap, blogName);
  }

  private void initFolders(String blogName) {
    localFileStore.init(blogName, defaultVideosFolderName);
  }

  private long downloadVideos(Map<String, String> filteredFileNameToUrlMap, String blogName) {
    if (filteredFileNameToUrlMap.isEmpty()) {
      return 0;
    }
    return filteredFileNameToUrlMap.entrySet().stream()
        .map(e -> safelyDownload(blogName, e.getKey(), e.getValue()))
        .filter(Boolean.TRUE::equals)
        .count();
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

  private Map<String, String> filterAlreadyDownloaded(Map<String, String> fileNameToUrlMap,
      Set<String> downloadedVideos) {
    downloadedVideos.forEach(fileNameToUrlMap::remove);
    return fileNameToUrlMap;
  }

  private Map<String, String> buildFileNameToUrlMap(Stream<String> videoUrlStream) {
    return videoUrlStream.collect(toMap(this::toFileName, identity(), (n, o) -> n));
  }

  private Stream<String> toUrlStream(Stream<Video> videoPostsStream) {
    return videoPostsStream.map(Video::getEmbedCode).map(this::toUrl);
  }

  private Set<String> getDownloadedImagesFileNameList(String blogName) {
    return Sets.newHashSet(localFileStore.listFileNames(blogName, defaultVideosFolderName));
  }

  private Stream<Video> getFilteredVideosStream(Stream<BlogPost> blogPostStream, int minWidth) {
    val videoPostsStream = blogPostStream
        .filter(post -> post.getVideos() != null)
        .flatMap(post -> post.getVideos().stream());
    if (minWidth > 0) {
      return videoPostsStream.filter(video -> video.getWidth() >= minWidth);
    }
    return videoPostsStream;
  }

  private Stream<BlogPost> getFilteredRecordsStream(String blogName, int minLikes) {
    val blogPostStream = recordStore.readRecords(blogName).stream();
    if (minLikes > 0) {
      return blogPostStream.filter(post -> post.getNoteCount() >= minLikes);
    }
    return blogPostStream;
  }

  private String toFileName(String url) {
    if (url == null) {
      return null;
    }
    if (url.contains("/tumblr_")) {
      int start = url.lastIndexOf("/tumblr_") + 1;
      int end = url.lastIndexOf("/");
      if (end <= start) {
        end = url.length();
      }
      return url.substring(start, end);
    }
    return null;
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
