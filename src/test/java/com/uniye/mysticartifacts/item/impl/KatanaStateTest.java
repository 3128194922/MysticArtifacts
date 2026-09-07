package com.uniye.mysticartifacts.item.impl;

import com.uniye.mysticartifacts.Config;

public final class KatanaStateTest {
    private KatanaStateTest() {
    }

    public static void main(String[] args) {
        // 纯 JVM 环境下 Forge 配置未加载，手动指定与断言一致的充能上限
        Config.KatanaMaxCharge = 100;
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
