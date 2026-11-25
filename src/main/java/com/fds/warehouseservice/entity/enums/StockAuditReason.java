package com.fds.warehouseservice.entity.enums;

import com.fds.warehouseservice.exceptions.EnumValueException;

import java.util.Locale;

public enum StockAuditReason {
    RELOCATION, WRITE_OFF, DAMAGE;

    public static StockAuditReason fromStringCaseIgnore(String reason) {
        try {
            return StockAuditReason.valueOf(reason.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new EnumValueException("Invalid audit reason: " + reason);
        }
    }
}
