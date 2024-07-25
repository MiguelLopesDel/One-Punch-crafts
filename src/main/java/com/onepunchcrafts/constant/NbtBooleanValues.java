package com.onepunchcrafts.constant;

public enum NbtBooleanValues {
    isSaitama("issaitama"), seriousFartActive("seriousfart"), superSpeed("superspeedsaitama"), breakBlocksQuickly("breakblocksquickly"), specialSkillActive("seriousPunchSkillActive");

    private String value;

    NbtBooleanValues(String isSaitama) {
        this.value = isSaitama;
    }

    public String getValue() {
        return value;
    }
}
