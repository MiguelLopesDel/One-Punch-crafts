package com.onepunchcrafts.constant;

public enum NbtBooleanValues {
    isSaitama("issaitama");

    private String value;

    NbtBooleanValues(String isSaitama) {
        this.value = isSaitama;
    }

    public String getValue() {
        return value;
    }
}
