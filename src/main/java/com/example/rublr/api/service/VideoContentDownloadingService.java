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
import java.util.Objects;
import java.util.Optional;
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
    return Optional.of(getFilteredRecordsStream(blogName, minLikes))
        .map(blogPostStream -> toFilteredVideosStream(blogPostStream, minWidth))
        .map(this::toUrlStream)
        .map(this::buildFileNameToUrlMap)
        .map(fileNameToUrlMap -> {
          val downloadedVideos = getDownloadedImagesFileNameList(blogName);
          return filterAlreadyDownloaded(fileNameToUrlMap, downloadedVideos);
        })
        .map(filteredFileNameToUrlMap -> downloadVideos(filteredFileNameToUrlMap, blogName))
        .get();
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
    return videoUrlStream
        .filter(Objects::nonNull)
        .filter(url -> url.contains("/tumblr_"))
        .collect(toMap(this::toFileName, identity(), (n, o) -> n));
  }

  private String toFileName(String url) {
    int start = url.lastIndexOf("/tumblr_") + 1;
    int end = url.lastIndexOf("/");
    if (end <= start) {
      end = url.length();
    }
    return url.substring(start, end);
  }

  private Stream<String> toUrlStream(Stream<Video> videoPostsStream) {
    return videoPostsStream.map(Video::getEmbedCode).map(this::toUrl);
  }

  private Set<String> getDownloadedImagesFileNameList(String blogName) {
    return Sets.newHashSet(localFileStore.listFileNames(blogName, defaultVideosFolderName));
  }

  private Stream<Video> toFilteredVideosStream(Stream<BlogPost> blogPostStream, int minWidth) {
    val videoPostsStream = blogPostStream
        .filter(post -> post.getVideos() != null && !post.getVideos().isEmpty())
        .flatMap(post -> post.getVideos().stream());
    if (minWidth > 0) {
      return videoPostsStream.filter(video -> video.getWidth() >= minWidth);
    }
    return videoPostsStream;
  }

  private Stream<BlogPost> getFilteredRecordsStream(String blogName, int minLikes) {
    val blogPostStream = getVideoBlogPosts(blogName);
    if (minLikes > 0) {
      return blogPostStream.filter(post -> post.getNoteCount() >= minLikes);
    }
    return blogPostStream;
  }

  private Stream<BlogPost> getVideoBlogPosts(String blogName) {
    return recordStore.readRecords(blogName).stream()
        .filter(post -> "video".equals(post.getType()));
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
