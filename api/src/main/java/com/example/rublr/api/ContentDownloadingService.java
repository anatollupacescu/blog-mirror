package com.example.rublr.api;

public interface ContentDownloadingService {

  long download(String blogName, int minLikes, int minWidth);

  long getCount(String blogName, int minLikes, int minWidth);
}
