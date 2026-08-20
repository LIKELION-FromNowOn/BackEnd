package com.youin.now.item;

import com.youin.now.common.response.ApiResponse;
import com.youin.now.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) { this.itemService = itemService; }

    @GetMapping
    public ApiResponse<List<ItemListRes>> list(@CurrentUser String userId) {
        return ApiResponse.ok(itemService.list(userId));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<ItemDeleteRes> delete(@CurrentUser String userId, @PathVariable String itemId) {
        return ApiResponse.ok(itemService.delete(userId, itemId));
    }

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ItemCustomRes> addCustom(@CurrentUser String userId,
                                                 @Valid @RequestBody ItemCustomReq req) {
        return ApiResponse.ok(itemService.addCustom(userId, req));
    }

    @PutMapping
    public ApiResponse<ItemSaveRes> save(@CurrentUser String userId,
                                         @Valid @RequestBody ItemSaveReq req) {
        return ApiResponse.ok(itemService.save(userId, req));
    }
}
