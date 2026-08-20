package com.youin.now.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ItemSaveReq(
        @NotNull List<@Valid Item> items
) {
    public record Item(@NotBlank String itemId, String frequency) {}
}
