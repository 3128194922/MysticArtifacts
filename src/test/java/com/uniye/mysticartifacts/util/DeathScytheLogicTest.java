package com.uniye.mysticartifacts.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DeathScytheLogicTest {
    private DeathScytheLogicTest() {
    }

    public static void main(String[] args) throws IOException {
        requireEquals(100, DeathScytheLogic.ENERGY_TICKS, "five seconds uses 100 ticks");
        requireEquals(100, DeathScytheLogic.remainingEnergyTicks(200, 100), "fresh energy is full");
        requireEquals(50, DeathScytheLogic.remainingEnergyTicks(200, 150), "energy counts down by game time");
        requireEquals(0, DeathScytheLogic.remainingEnergyTicks(200, 250), "expired energy clamps to zero");
        requireEquals(13, DeathScytheLogic.barWidth(100), "full energy fills the bar");
        requireEquals(6, DeathScytheLogic.barWidth(50), "half energy maps to six pixels");
        requireEquals(0, DeathScytheLogic.barWidth(0), "empty energy maps to zero");
        requireFalse(DeathScytheLogic.hasEnergy(200, 200), "energy expires at its timestamp");
        requireFalse(DeathScytheLogic.hasEnergy(200, 201), "expired energy cannot trigger right-click");
        requireEquals(20, DeathScytheLogic.RIGHT_CLICK_COOLDOWN_TICKS, "right-click cooldown is one second");
        requireEquals(1, DeathScytheLogic.nextSequence(0), "first slash sequence is one");

        String itemSource = Files.readString(Path.of(
                "src/main/java/com/uniye/mysticartifacts/item/impl/DeathScytheItem.java"));
        requireContains(itemSource, "stack.getOrCreateTag().putUUID(TAG_TARGET_UUID, targetUUID);",
                "a later target replaces the previous UUID in the same tag");
        requireContains(itemSource, "setTargetUUID(stack, target.getUUID());",
                "each successful melee hit records its target");
        requireContains(itemSource, "if (!DeathScytheLogic.hasEnergy(getEnergyUntil(stack), level.getGameTime()))",
                "an expired timestamp blocks right-click");
        requireNotContains(itemSource, "FMLEnvironment", "common item must not inspect physical client side");
        requireNotContains(itemSource, "@OnlyIn", "common item must not contain client-only members");
        requireNotContains(itemSource, "net.minecraft.client", "common item must not reference client classes");
        requireNotContains(itemSource, "ClientBarTime", "common item must not reference client time helper");
        requireNotContains(itemSource, "hurtAndBreak", "energy bar must not consume durability");

        String clearSource = section(itemSource, "public static void clearTarget(",
                "public static long getEnergyUntil(");
        requireContains(clearSource, "tag.remove(TAG_TARGET_UUID);", "invalid target clears UUID");
        requireContains(clearSource, "tag.remove(TAG_ENERGY_UNTIL);", "invalid target clears expiry");
        requireContains(clearSource, "tag.remove(TAG_SLASH_SEQUENCE);", "invalid target clears sequence");
        requireContains(clearSource, "tag.remove(TAG_ENERGY_DISPLAY_TICKS);", "invalid target clears display cache");

        String tickSource = section(itemSource, "public void inventoryTick(",
                "public InteractionResultHolder<ItemStack> use(");
        requireContains(tickSource, "if (level.isClientSide)", "client display is updated in inventory tick");
        requireContains(tickSource, "getRemainingEnergyTicks(stack, level.getGameTime())",
                "client display uses supplied level time");
        requireContains(tickSource, "TAG_ENERGY_DISPLAY_TICKS", "client display has a separate NBT key");
        String clientTickSource = section(tickSource, "if (level.isClientSide)",
                "if (!(level instanceof ServerLevel serverLevel))");
        requireContains(clientTickSource, "putInt(TAG_ENERGY_DISPLAY_TICKS, remaining)",
                "client writes only the display cache");
        requireContains(clientTickSource, "tag.remove(TAG_ENERGY_DISPLAY_TICKS);",
                "client clears expired display cache");
        requireNotContains(clientTickSource, "setTargetUUID(", "client cannot choose a target");
        requireNotContains(clientTickSource, "setEnergyUntil(", "client cannot extend energy");
        requireNotContains(clientTickSource, "target.hurt(", "client cannot apply damage");
        String barSource = section(itemSource, "public boolean isBarVisible(",
                "public boolean hurtEnemy(");
        requireContains(barSource, "getDisplayEnergyTicks(stack)", "bar reads the display cache");
        requireContains(barSource, "Math.max(0, Math.min(13, 13 * getDisplayEnergyTicks(stack) / duration))",
                "display width is floored and clamped to 0..13");
        requireNotContains(barSource, "getEnergyUntil(stack)", "bar must not read authority timestamp directly");

        String useSource = section(itemSource, "public InteractionResultHolder<ItemStack> use(", "\n}");
        requireContains(useSource, "player.getAttributeValue(Attributes.ATTACK_DAMAGE)",
                "right-click uses current player attack damage");
        requireContains(useSource.replaceAll("\\s+", " "),
                "if (!damaged) { return InteractionResultHolder.fail(stack); }",
                "failed damage exits before effects");
        requireInOrder(useSource, "if (!damaged)",
                "addCooldown(this, DeathScytheLogic.RIGHT_CLICK_COOLDOWN_TICKS)",
                "triggerSlash(serverPlayer, stack)", "new DeathScytheSlashPacket(");

        System.out.println("PASS: Death Scythe logic rules");
    }

    private static void requireEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void requireFalse(boolean value, String message) {
        if (value) {
            throw new AssertionError(message);
        }
    }

    private static void requireContains(String source, String expected, String message) {
        if (!source.contains(expected)) {
            throw new AssertionError(message + ": missing " + expected);
        }
    }

    private static void requireNotContains(String source, String forbidden, String message) {
        if (source.contains(forbidden)) {
            throw new AssertionError(message + ": found " + forbidden);
        }
    }

    private static String section(String source, String start, String end) {
        int first = source.indexOf(start);
        int last = first < 0 ? -1 : source.indexOf(end, first + start.length());
        if (last < 0) {
            throw new AssertionError("missing source section: " + start + " to " + end);
        }
        return source.substring(first, last);
    }

    private static void requireInOrder(String source, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int position = source.indexOf(marker, previous + 1);
            if (position < 0) {
                throw new AssertionError("missing or misplaced successful attack step: " + marker);
            }
            previous = position;
        }
    }
}
