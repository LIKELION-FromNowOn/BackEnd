package com.youin.now.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemCustomReq(@NotBlank @Size(min = 2, max = 60) String text) {}
