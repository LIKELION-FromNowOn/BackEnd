package com.youin.now.item;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemUserItemRepository extends JpaRepository<ItemUserItem, String> {
    @Modifying
    @Query("update ItemUserItem i set i.deletedAt = :deletedAt where i.userId = :userId and i.deletedAt is null")
    void softDeleteActive(@Param("userId") String userId, @Param("deletedAt") OffsetDateTime deletedAt);

    @Query("""
            select i from ItemUserItem i
             where i.userId = :userId and i.deletedAt is null
               and (i.id = :itemId or i.careItemId = :itemId)
            """)
    Optional<ItemUserItem> findActiveOwned(@Param("userId") String userId, @Param("itemId") String itemId);

    @Query("""
            select (count(i) > 0) from ItemUserItem i
             where i.deletedAt is null and (i.id = :itemId or i.careItemId = :itemId)
            """)
    boolean existsActiveByPublicItemId(@Param("itemId") String itemId);

    long countByUserIdAndDeletedAtIsNull(String userId);

    @Modifying
    @Query("update ItemUserItem i set i.deletedAt = :deletedAt where i.id = :id and i.deletedAt is null")
    int softDeleteById(@Param("id") String id, @Param("deletedAt") OffsetDateTime deletedAt);

    @Query(value = "select id as id, frequency_editable as frequencyEditable from care_items where id in (:ids)", nativeQuery = true)
    List<MasterRow> findMasterRows(@Param("ids") Collection<String> ids);

    @Query(value = """
            select ui.id as userItemId, coalesce(ui.care_item_id, ui.id) as itemId,
                   coalesce(ci.name, ui.custom_name) as name, coalesce(ci.category_id, ui.custom_category, 'life') as categoryId,
                   coalesce(ci.core, 0) as core, coalesce(ci.base, 0) as base, ui.frequency as frequency,
                   case ci.floor when 'essential' then 2 when 'recommended' then 1 when 'excluded' then -1 else 0 end as floor,
                   coalesce(ci.floor, 'optional') as floorCode, ui.is_custom as custom,
                   case ci.evidence_level when 'high' then 1 when 'medium' then 2 when 'low' then 3 else 0 end as evidenceLevel
              from user_items ui left join care_items ci on ci.id = ui.care_item_id
             where ui.user_id = :userId and ui.deleted_at is null
            """, nativeQuery = true)
    List<SelectedRow> findSelectedRows(@Param("userId") String userId);

    interface MasterRow { String getId(); boolean getFrequencyEditable(); }
    interface SelectedRow {
        String getUserItemId(); String getItemId(); String getName(); String getCategoryId();
        double getCore(); double getBase(); String getFrequency(); int getFloor(); String getFloorCode();
        boolean getCustom(); int getEvidenceLevel();
    }
}
