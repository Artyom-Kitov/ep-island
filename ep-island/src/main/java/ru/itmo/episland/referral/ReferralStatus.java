package ru.itmo.episland.referral;

public enum ReferralStatus {
    CREATED,
    HANDED_TO_CONVOY,
    CANCELLED;

    public boolean isEditable() {
        return this == CREATED;
    }
}
