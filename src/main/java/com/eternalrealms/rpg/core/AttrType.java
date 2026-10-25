package com.eternalrealms.rpg.core;

/** Atributos principais distribuíveis. */
public enum AttrType {
    STRENGTH("strength"),
    VITALITY("vitality"),
    RESISTANCE("resistance"),
    AGILITY("agility"),
    INTELLIGENCE("intelligence"),
    WISDOM("wisdom"),
    DEXTERITY("dexterity"),
    LUCK("luck"),
    SPIRIT("spirit"),
    TECHNIQUE("technique");

    public final String id;

    AttrType(String id) {
        this.id = id;
    }
}
