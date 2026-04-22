package io.github.mystagogy.insuranceinterface.domain.stat.entity;

public enum GenderType {
    MALE("남자"),
    FEMALE("여자"),
    OTHER("기타"),
    ALL("전체"),
    UNKNOWN("미상");

    private final String displayName;

    GenderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static GenderType fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return UNKNOWN;
        }
        return switch (label.trim()) {
            case "남", "남자", "남성", "M", "MALE" -> MALE;
            case "여", "여자", "여성", "F", "FEMALE" -> FEMALE;
            case "기타", "OTHER" -> OTHER;
            case "전체", "합계", "ALL" -> ALL;
            default -> UNKNOWN;
        };
    }
}
