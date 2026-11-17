package com.fds.warehouseservice.entity.enums;

import com.fds.warehouseservice.exceptions.EnumValueException;

import java.util.Locale;

public enum StorageBinZone {
    AMBIENT, CHILLED, FROZEN;

    public static StorageBinZone fromStringCaseIgnore(String zone) {
        try {
            return StorageBinZone.valueOf(zone.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new EnumValueException("Invalid storage bin zone: " + zone);
        }
    }
}
