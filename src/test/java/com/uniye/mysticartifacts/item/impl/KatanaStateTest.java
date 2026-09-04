package com.uniye.mysticartifacts.item.impl;

public final class KatanaStateTest {
    private KatanaStateTest() {
    }

    public static void main(String[] args) {
        require(KatanaState.clampEnergy(-4) == 0, "negative energy");
        require(KatanaState.clampEnergy(101) == 100, "energy cap");
        require(KatanaState.canDash(100, false), "full energy can dash");
        require(!KatanaState.canDash(99, false), "partial energy cannot dash");
        require(!KatanaState.canDash(100, true), "open katana cannot dash");
        require(KatanaState.consumeEnergy(100, 100) == 0, "dash consumes energy");
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
