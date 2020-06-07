package com.green.square;

import org.springframework.context.annotation.Bean;

public class ProgramExecutorSpring {

  @Bean
  public ProgramExecutor programExecutor() {
    return ProgramExecutor.getInstance();
  }

}
