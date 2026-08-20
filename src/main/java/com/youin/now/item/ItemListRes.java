package com.youin.now.item;

/** NOW-ITEM-001의 항목 하나. 엔티티를 그대로 응답에 내보내지 않습니다. */
public record ItemListRes(String itemId, String name, String category,
                          String frequency, String floor, boolean custom) {}
