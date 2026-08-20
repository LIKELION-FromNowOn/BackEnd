package com.youin.now.item;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) { this.itemService = itemService; }

    @PutMapping
    public ApiResponse<ItemSaveRes> save(@CurrentUser String userId,
                                         @Valid @RequestBody ItemSaveReq req) {
        return ApiResponse.ok(itemService.save(userId, req));
    }
}
