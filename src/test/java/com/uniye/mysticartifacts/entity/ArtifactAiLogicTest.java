package com.uniye.mysticartifacts.entity;

import java.util.List;

public final class ArtifactAiLogicTest {
    private ArtifactAiLogicTest() {
    }

    public static void main(String[] args) {
        testSpiritPrefersPlayerAttackTarget();
        testDemonicAvoidsSpiritTargetWhenBothAreWorn();
        testInvalidCandidatesAreSkipped();
        testStateTransitionsDoNotRetargetDuringCooldown();
        System.out.println("PASS: Artifact AI logic rules");
    }

    private static void testSpiritPrefersPlayerAttackTarget() {
        int selected = ArtifactAiLogic.chooseTarget(
                List.of(
                        candidate(2, 2.0, false, false, false, false, false, true),
                        candidate(3, 9.0, false, false, false, true, true, false)
                ),
                3,
                2,
                ArtifactAiLogic.Role.SPIRIT,
                true
        );
        requireEquals(3, selected, "spirit keeps the player's active target");
    }

    private static void testDemonicAvoidsSpiritTargetWhenBothAreWorn() {
        int selected = ArtifactAiLogic.chooseTarget(
                List.of(
                        candidate(3, 4.0, false, false, false, true, true, false),
                        candidate(4, 9.0, false, false, false, false, false, true)
                ),
                3,
                -1,
                ArtifactAiLogic.Role.DEMONIC,
                true
        );
        requireEquals(4, selected, "demonic chooses a different target when spirit owns the active target");
    }

    private static void testInvalidCandidatesAreSkipped() {
        int selected = ArtifactAiLogic.chooseTarget(
                List.of(
                        candidate(2, 1.0, true, false, false, true, false, true),
                        candidate(5, 4.0, false, false, false, true, false, true),
                        candidate(6, 2.0, false, false, false, true, false, true)
                ),
                -1,
                -1,
                ArtifactAiLogic.Role.DEMONIC,
                false
        );
        requireEquals(6, selected, "invalid candidates are ignored");
    }

    private static void testStateTransitionsDoNotRetargetDuringCooldown() {
        requireEquals(ArtifactAiLogic.State.COOLDOWN,
                ArtifactAiLogic.nextState(ArtifactAiLogic.State.ATTACK, true, true, false, false),
                "an attack enters cooldown when it is not ready");
        requireEquals(ArtifactAiLogic.State.ATTACK,
                ArtifactAiLogic.nextState(ArtifactAiLogic.State.COOLDOWN, true, true, true, false),
                "cooldown returns to attack without reacquiring a target");
        requireEquals(ArtifactAiLogic.State.RETURN,
                ArtifactAiLogic.nextState(ArtifactAiLogic.State.ATTACK, true, false, true, false),
                "lost targets force a return state");
    }

    private static ArtifactAiLogic.Candidate candidate(int id, double distanceSqr,
                                                        boolean spectator, boolean sameTeam,
                                                        boolean noLineOfSight, boolean activeTarget,
                                                        boolean targetsOwner, boolean attackedOwner) {
        return new ArtifactAiLogic.Candidate(id, distanceSqr, true, spectator, sameTeam,
                !noLineOfSight, activeTarget, attackedOwner, targetsOwner, false);
    }

    private static void requireEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void requireEquals(ArtifactAiLogic.State expected,
                                      ArtifactAiLogic.State actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
