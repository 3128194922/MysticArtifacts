package com.uniye.mysticartifacts.entity;

import java.util.Comparator;
import java.util.List;

/**
 * Shared, side-independent decision rules for the artifact entities.
 * Minecraft entity queries are kept outside this class so the rules remain cheap to test.
 */
public final class ArtifactAiLogic {
    private ArtifactAiLogic() {
    }

    public enum Role {
        SPIRIT,
        DEMONIC
    }

    public enum State {
        FOLLOW,
        SEARCH,
        ATTACK,
        COOLDOWN,
        RETURN
    }

    public record Candidate(int id, double distanceSqr, boolean alive, boolean spectator,
                            boolean sameTeam, boolean lineOfSight, boolean activeTarget,
                            boolean attackedOwner, boolean targetsOwner, boolean reservedByOther) {
        public boolean valid() {
            return alive && !spectator && !sameTeam && lineOfSight;
        }
    }

    /**
     * Chooses a deterministic target. The demonic entity avoids the spirit's active target
     * when another valid target exists, then falls back to it rather than becoming idle.
     */
    public static int chooseTarget(List<Candidate> candidates, int activeTargetId,
                                   int attackerId, Role role, boolean dualWorn) {
        Candidate selected = chooseTargetPass(candidates, activeTargetId, attackerId, role,
                dualWorn && role == Role.DEMONIC, true);
        if (selected == null) {
            selected = chooseTargetPass(candidates, activeTargetId, attackerId, role, false, false);
        }
        return selected == null ? -1 : selected.id();
    }

    private static Candidate chooseTargetPass(List<Candidate> candidates, int activeTargetId,
                                              int attackerId, Role role, boolean avoidShared,
                                              boolean avoidReserved) {
        return candidates.stream()
                .filter(Candidate::valid)
                .filter(candidate -> !avoidShared || candidate.id() != activeTargetId)
                .filter(candidate -> !avoidReserved || !candidate.reservedByOther())
                .min(Comparator
                        .comparingInt((Candidate candidate) -> priority(candidate, activeTargetId,
                                attackerId, role))
                        .thenComparingDouble(Candidate::distanceSqr)
                        .thenComparingInt(Candidate::id))
                .orElse(null);
    }

    private static int priority(Candidate candidate, int activeTargetId, int attackerId,
                                Role role) {
        if (role == Role.SPIRIT) {
            if (candidate.id() == activeTargetId || candidate.activeTarget()) return 0;
            if (candidate.id() == attackerId || candidate.attackedOwner()) return 1;
            if (candidate.targetsOwner()) return 2;
        } else {
            if (candidate.targetsOwner()) return 0;
            if (candidate.id() == attackerId || candidate.attackedOwner()) return 1;
            if (candidate.id() == activeTargetId || candidate.activeTarget()) return 2;
        }
        return 3;
    }

    public static State nextState(State current, boolean hasTarget, boolean targetValid,
                                  boolean attackReady, boolean closeToFollowPoint) {
        boolean usableTarget = hasTarget && targetValid;
        return switch (current) {
            case FOLLOW -> usableTarget ? State.ATTACK : State.SEARCH;
            case SEARCH -> usableTarget ? State.ATTACK
                    : (closeToFollowPoint ? State.FOLLOW : State.SEARCH);
            case ATTACK -> !usableTarget ? State.RETURN
                    : (attackReady ? State.ATTACK : State.COOLDOWN);
            case COOLDOWN -> !usableTarget ? State.RETURN
                    : (attackReady ? State.ATTACK : State.COOLDOWN);
            case RETURN -> closeToFollowPoint
                    ? (usableTarget ? State.ATTACK : State.FOLLOW)
                    : State.RETURN;
        };
    }
}
