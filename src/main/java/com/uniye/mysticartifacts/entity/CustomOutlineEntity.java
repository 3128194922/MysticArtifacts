package com.uniye.mysticartifacts.entity;

/**
 * Common-side contract for entities whose outline is rendered by their client renderer.
 */
public interface CustomOutlineEntity {
    int NO_OUTLINE_COLOR = 0;

    int getCustomOutlineColor();

    default boolean hasCustomOutline() {
        return getCustomOutlineColor() != NO_OUTLINE_COLOR;
    }
}
