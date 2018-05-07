package com.example.rublr.domain;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Blog {

  private String name;
  private Integer totalPostCount;
}
