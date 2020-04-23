package com.green.square;

import org.apache.commons.lang3.SystemUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringMainClass {

  public static void main(String[] args) {
    SpringApplication.run(SpringMainClass.class, args);
  }


  @Bean
  public RestFileSystemConfiguration provide() {
    if (SystemUtils.IS_OS_LINUX) {
      return new RestFileSystemConfiguration("/home/andrey/youtubeNew/", "/usr/local/bin/youtube-dl",
          "/usr/bin/ffmpeg");
    } else {
      return new RestFileSystemConfiguration("C:\\youtubeNew\\",
          "C:\\Users\\Andrey\\Downloads\\youtube\\youtube-dl.exe", "E:\\Programs\\ffmpeg\\bin\\ffmpeg.exe");
    }
  }

//  @Bean
//  public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
//    return args -> {
//
//      System.out.println("Let's inspect the beans provided by Spring Boot:");
//
//      String[] beanNames = ctx.getBeanDefinitionNames();
//      Arrays.sort(beanNames);
//      for (String beanName : beanNames) {
//        System.out.println(beanName);
//      }
//
//    };
//  }

}
