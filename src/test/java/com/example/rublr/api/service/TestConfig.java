package com.example.rublr.api.service;

import com.example.rublr.api.BlogPostFetcher;
import com.example.rublr.api.FileStore;
import com.example.rublr.api.RecordStore;
import com.example.rublr.dummy.InMemoryBlogPostFetcher;
import com.example.rublr.dummy.InMemoryFileStore;
import com.example.rublr.dummy.InMemoryRecordStore;
import com.tngtech.jgiven.integration.spring.EnableJGiven;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableJGiven
@ComponentScan(basePackages = "com.example.rublr.api.service")
public class TestConfig {

  @Bean
  public BlogPostFetcher fetcher() {
    return new InMemoryBlogPostFetcher();
  }

  @Bean
  public RecordStore recordStore() {
    return new InMemoryRecordStore();
  }

  @Bean
  public FileStore videoStore() {
    return new InMemoryFileStore();
  }
}
