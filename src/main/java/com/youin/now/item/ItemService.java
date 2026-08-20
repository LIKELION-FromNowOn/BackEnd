package com.youin.now.item;

import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.id.Ids;
import com.youin.now.subtract.SubtractFrequency;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final ItemUserItemRepository items;

    public ItemService(ItemUserItemRepository items) { this.items = items; }

    @Transactional
    public ItemSaveRes save(String userId, ItemSaveReq req) {
        Set<String> ids = new HashSet<>();
        for (ItemSaveReq.Item item : req.items()) ids.add(item.itemId());
        if (ids.size() != req.items().size()) throw new ApiException(ErrorCode.VALIDATION_FAILED);

        Map<String, ItemUserItemRepository.MasterRow> masters = new HashMap<>();
        for (ItemUserItemRepository.MasterRow master : items.findMasterRows(ids)) masters.put(master.getId(), master);
        if (masters.size() != ids.size()) throw new ApiException(ErrorCode.VALIDATION_FAILED, "존재하지 않는 항목이 포함되어 있습니다");

        for (ItemSaveReq.Item item : req.items()) {
            boolean editable = masters.get(item.itemId()).getFrequencyEditable();
            if (editable && SubtractFrequency.ofOrNull(item.frequency()) == null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "빈도를 선택해 주세요");
            }
            if (!editable && item.frequency() != null) throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }

        items.softDeleteActive(userId, OffsetDateTime.now(KST));
        items.saveAll(req.items().stream()
                .map(i -> new ItemUserItem(Ids.userItem(), userId, i.itemId(), i.frequency()))
                .toList());
        return new ItemSaveRes(req.items().size(), true);
    }
}
