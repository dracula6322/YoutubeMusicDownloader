package com.green.square;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ProgramExecutorSpringWrapper {

  @Bean
  public ProgramExecutor programExecutor() {
    return ProgramExecutor.getInstance();
  }
}
