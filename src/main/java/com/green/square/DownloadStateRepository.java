package com.green.square;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface DownloadStateRepository extends MongoRepository<DownloadState, String> {

  public DownloadState findByVideoId(String videoId);

  public DownloadState findByVideoLink(String videoLink);

}
