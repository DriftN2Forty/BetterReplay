package me.justindevb.replay.util;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class ItemStackSerializer {
    private ItemStackSerializer() {}

    public static Map<String, Object> serialize(ItemStack item) {
        return item == null ? null : item.serialize();
    }

    @SuppressWarnings("unchecked")
    public static ItemStack deserialize(Object raw) {
        if (raw == null) return null;
        if (!(raw instanceof Map)) return null;
        Map<String, Object> map = (Map<String, Object>) raw;
        try {
            return ItemStack.deserialize(map);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
