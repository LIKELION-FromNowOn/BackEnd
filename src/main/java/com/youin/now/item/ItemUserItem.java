package com.youin.now.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "user_items")
public class ItemUserItem {
    @Id @Column(nullable = false) private String id;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(name = "care_item_id") private String careItemId;
    @Column(name = "custom_name") private String customName;
    @Column(name = "is_custom", nullable = false) private boolean custom;
    @Column(name = "frequency") private String frequency;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP(6)") private OffsetDateTime createdAt;
    @Column(name = "deleted_at") private OffsetDateTime deletedAt;

    protected ItemUserItem() {}

    public ItemUserItem(String id, String userId, String careItemId, String frequency) {
        this.id = id;
        this.userId = userId;
        this.careItemId = careItemId;
        this.frequency = frequency;
        this.custom = false;
    }
}
