package com.youin.now.item;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ItemPortAdapter implements ItemPort {
    private final ItemUserItemRepository items;
    public ItemPortAdapter(ItemUserItemRepository items) { this.items = items; }

    @Override
    public List<SelectedItem> selected(String userId) {
        return items.findSelectedRows(userId).stream()
                .map(r -> new SelectedItem(r.getUserItemId(), r.getItemId(), r.getName(), r.getCategoryId(),
                        r.getCore(), r.getBase(), r.getFrequency(), r.getFloor(), r.getEvidenceLevel()))
                .toList();
    }
}
