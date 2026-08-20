package com.youin.now.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youin.now.common.error.ApiException;
import com.youin.now.common.error.ErrorCode;
import com.youin.now.common.id.Ids;
import com.youin.now.common.llm.LlmClient;
import com.youin.now.safety.SafetyPort;
import com.youin.now.subtract.SubtractFrequency;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Set<String> CATEGORIES = Set.of("care", "sleep", "move", "eat", "mind", "life", "med");
    private static final List<String> FREQUENCIES = List.of("weekly_1", "weekly_2", "weekly_3", "weekly_4plus", "daily");
    private static final ObjectMapper JSON = new ObjectMapper();
    private final ItemUserItemRepository items;
    private final SafetyPort safety;
    private final LlmClient llm;
    private final String interpretPrompt;

    public ItemService(ItemUserItemRepository items, SafetyPort safety, LlmClient llm,
                       @Value("classpath:prompts/item-interpret.txt") Resource prompt) throws IOException {
        this.items = items;
        this.safety = safety;
        this.llm = llm;
        this.interpretPrompt = new String(prompt.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public List<ItemListRes> list(String userId) {
        return items.findSelectedRows(userId).stream()
                .map(r -> new ItemListRes(r.getItemId(), r.getName(), r.getCategoryId(),
                        r.getFrequency(), r.getFloorCode(), r.getCustom()))
                .toList();
    }

    @Transactional
    public ItemDeleteRes delete(String userId, String itemId) {
        ItemUserItem target = items.findActiveOwned(userId, itemId).orElse(null);
        if (target == null) {
            if (items.existsActiveByPublicItemId(itemId)) throw new ApiException(ErrorCode.FORBIDDEN);
            throw new ApiException(ErrorCode.ITEM_NOT_FOUND);
        }

        long active = items.countByUserIdAndDeletedAtIsNull(userId);
        if (active <= 3) {
            throw new ApiException(ErrorCode.MIN_ITEMS_REQUIRED, "최소 항목 수 미만이 되어 삭제할 수 없습니다");
        }

        int changed = items.softDeleteById(target.id(), OffsetDateTime.now(KST));
        if (changed != 1) throw new ApiException(ErrorCode.ITEM_NOT_FOUND);
        return new ItemDeleteRes(itemId, true, active - 1, true);
    }

    @Transactional
    public ItemCustomRes addCustom(String userId, ItemCustomReq req) {
        SafetyPort.SafetyResult checked = safety.check(req.text(), SafetyPort.Source.ITEM_CUSTOM);
        if (checked.blocked()) throw new ApiException(ErrorCode.TEXT_REJECTED, checked.message());

        ItemInterpretation parsed = llm.ask(interpretPrompt, interpretationPayload(req.text()), ItemInterpretation.class);
        boolean valid = parsed != null && CATEGORIES.contains(parsed.category())
                && (parsed.frequency() == null || FREQUENCIES.contains(parsed.frequency()));

        String category = valid ? parsed.category() : "life";
        String frequency = valid ? parsed.frequency() : null;
        String interpretedBy = valid ? "llm" : "fallback";
        String id = Ids.of("cu");
        items.save(ItemUserItem.custom(id, userId, req.text(), category, frequency, interpretedBy));

        return new ItemCustomRes(id, req.text(), category, frequency, "optional",
                "llm".equals(interpretedBy) ? "low" : "none", interpretedBy);
    }

    @Transactional
    public ItemSaveRes save(String userId, ItemSaveReq req) {
        if (req.items().size() < 3) throw new ApiException(ErrorCode.MIN_ITEMS_REQUIRED);

        Set<String> ids = new HashSet<>();
        for (ItemSaveReq.Item item : req.items()) ids.add(item.itemId());
        if (ids.size() != req.items().size()) throw new ApiException(ErrorCode.VALIDATION_FAILED);

        Map<String, ItemUserItemRepository.MasterRow> masters = new HashMap<>();
        for (ItemUserItemRepository.MasterRow master : items.findMasterRows(ids)) masters.put(master.getId(), master);
        if (masters.size() != ids.size()) throw new ApiException(ErrorCode.VALIDATION_FAILED, "존재하지 않는 항목이 포함되어 있습니다");

        for (ItemSaveReq.Item item : req.items()) {
            boolean editable = masters.get(item.itemId()).getFrequencyEditable();
            if (editable && (item.frequency() == null || item.frequency().isBlank())) {
                throw new ApiException(ErrorCode.FREQUENCY_REQUIRED);
            }
            if (editable && SubtractFrequency.ofOrNull(item.frequency()) == null)
                throw new ApiException(ErrorCode.VALIDATION_FAILED);
            if (!editable && item.frequency() != null) throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }

        items.softDeleteActive(userId, OffsetDateTime.now(KST));
        items.saveAll(req.items().stream()
                .map(i -> new ItemUserItem(Ids.userItem(), userId, i.itemId(), i.frequency()))
                .toList());
        return new ItemSaveRes(req.items().size(), true);
    }

    private static String interpretationPayload(String text) {
        ObjectNode root = JSON.createObjectNode();
        root.put("text", text);
        ArrayNode categories = root.putArray("categories");
        CATEGORIES.forEach(categories::add);
        ArrayNode frequencies = root.putArray("frequencies");
        FREQUENCIES.forEach(frequencies::add);
        return root.toString();
    }

    private record ItemInterpretation(String category, String frequency) {}
}
