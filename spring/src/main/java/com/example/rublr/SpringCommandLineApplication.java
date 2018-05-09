package com.example.rublr;

import com.example.rublr.api.BlogPostFetcher;
import com.example.rublr.api.DataFetcher;
import com.example.rublr.api.FileStore;
import com.example.rublr.api.RecordStore;
import com.example.rublr.api.domain.Blog;
import com.example.rublr.api.service.BlogPostSynchronizingService;
import com.example.rublr.api.service.ImageContentDownloadingService;
import com.example.rublr.api.service.VideoContentDownloadingService;
import com.example.rublr.v4.HttpDataFetcher;
import com.example.rublr.v4.LocalFileStore;
import com.example.rublr.v4.LocalRecordStore;
import com.example.rublr.v4.TumblrBlogPostFetcher;
import com.tumblr.jumblr.JumblrClient;
import java.util.List;
import java.util.concurrent.Executors;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import rx.Scheduler;
import rx.schedulers.Schedulers;

@SpringBootApplication
public class SpringCommandLineApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpringCommandLineApplication.class, args);
  }

  @Bean
  public PromptProvider customPromptProvider() {
    return () -> new AttributedString("blog-mirror:>",
        AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
  }

  @Bean
  public RecordStore recordStore(@Value("${record.store.folder:./}") String rootFolder) {
    return new LocalRecordStore(rootFolder);
  }

  @Bean
  public BlogPostSynchronizingService service(BlogPostFetcher tumblrPostFetcher,
      RecordStore recordStore,
      @Value("${download.posts.step:20}") int step) {
    return new BlogPostSynchronizingService(recordStore, tumblrPostFetcher, step);
  }

  @Bean
  public BlogPostFetcher tumblrPostFetcher(JumblrClient client) {
    return new TumblrBlogPostFetcher(client);
  }

  @Bean
  public DataFetcher httpDataFetcher() {
    return new HttpDataFetcher();
  }

  @Bean
  public FileStore localFileStore(@Value("${local.files.root.folder:./}") String rootFolder) {
    return new LocalFileStore(rootFolder);
  }

  @Bean
  public VideoContentDownloadingService videoDownloader(RecordStore recordStore,
      FileStore localFileStore,
      @Value("${video.downloader.default.folder:videos}") String defaultFolderName) {
    return new VideoContentDownloadingService(recordStore, localFileStore, httpDataFetcher(),
        defaultFolderName);
  }

  @Bean
  public ImageContentDownloadingService imageDownloader(FileStore localFileStore,
      RecordStore recordStore,
      @Value("${image.downloader.thread.count:5}") int nThreads,
      @Value("${image.downloader.default.folder:images}") String defaultFolderName) {
    Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(nThreads));
    return new ImageContentDownloadingService(scheduler, localFileStore, recordStore,
        httpDataFetcher(), defaultFolderName);
  }

  @Bean
  public JumblrClient client(@Value("${tumblr.client.key}") String clientKey,
      @Value("${tumblr.client.secret}") String clientSecret,
      @Value("${tumblr.token}") String token,
      @Value("${tumblr.token.secret}") String tokenSecret) {
    JumblrClient client = new JumblrClient(clientKey, clientSecret);
    client.setToken(token, tokenSecret);
    return client;
  }

  @ShellComponent
  public class BlogMirrorCommands {

    @Autowired
    private RecordStore recordStore;

    @Autowired
    private BlogPostSynchronizingService service;

    @Autowired
    private VideoContentDownloadingService videoDownloadingService;

    @Autowired
    private ImageContentDownloadingService imageDownloadingService;

    @ShellMethod("List mirrored blogs along with their record count")
    public List<Blog> list() {
      return recordStore.listBlogs();
    }

    @ShellMethod("Fetches records for a given blog")
    public String syncBlog(@ShellOption String blogName) {
      long count = service.syncBlog(blogName);
      return String.format("Done fetching %d record(s)", count);
    }

    @ShellMethod("Fetches images for a given blog")
    public String fetchImages(@ShellOption String blogName,
        @ShellOption(defaultValue = "0") int minLikes,
        @ShellOption(defaultValue = "0") int minWidth) {
      long count = imageDownloadingService.download(blogName, minLikes, minWidth);
      return String.format("Done fetching %d image(s)", count);
    }

    @ShellMethod("Fetches videos for a given blog")
    public String fetchVideos(@ShellOption String blogName,
        @ShellOption(defaultValue = "0") int minLikes,
        @ShellOption(defaultValue = "0") int minWidth) {
      long count = videoDownloadingService.download(blogName, minLikes, minWidth);
      return String.format("Done fetching %d video(s)", count);
    }
  }
}