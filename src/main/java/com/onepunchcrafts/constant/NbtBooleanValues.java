package com.onepunchcrafts.constant;

public enum NbtBooleanValues {
    seriousFartActive("seriousfart"), superSpeed("superspeedsaitama"), breakBlocksQuickly("breakblocksquickly"),
    specialSkillActive("seriousPunchSkillActive"), extremeSpeed("extremeSpeed"), extremeJump("extremejump");

    private String value;

    NbtBooleanValues(String string) {
        this.value = string;
    }

    public String getValue() {
        return value;
    }
}
