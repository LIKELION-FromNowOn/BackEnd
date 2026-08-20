package com.youin.now.item;

public record ItemDeleteRes(String itemId, boolean deleted, long remainingCount, boolean needsRejudge) {}
