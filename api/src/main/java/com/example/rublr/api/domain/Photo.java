package com.example.rublr.api.domain;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Photo {

  private String caption;
  private List<Size> sizes;
}
