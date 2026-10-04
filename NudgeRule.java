import java.util.Random;

/**
 * Rule R6 — Nudge (non-colliding co-occupancy).
 *
 * Does NOT run on its own (apply is a no-op). MovementRule calls
 * resolve(...) when a mover's target cell is occupied but the collision
 * roll failed.
 *
 * Implements spec §4.6.
 */
public class NudgeRule implements Rule {

    @Override
    public void apply(Grid grid, Settings s, Random rng) {
        // no-op: invoked by MovementRule
    }

    /**
     * Move A around B without destroying either.
     *
     * @param fromRow   A's origin row (its cell has already been cleared)
     * @param fromCol   A's origin column
     * @param targetRow row of the blocked cell (same as fromRow in v1)
     * @param targetCol column of the blocked cell
     * @param mover     the object trying to move (A)
     * @param target    the blocked Cell (holds B)
     */
    public void resolve(Grid grid,
                        int fromRow, int fromCol,
                        int targetRow, int targetCol,
                        OrbitalObject mover, Cell target,
                        Settings s, Random rng) {

        int[] free = grid.findFreeNeighbor(
                targetRow, targetCol,
                mover.getDirection(),
                s.PASS_BEHAVIOR,
                rng);

        if (free != null) {
            // Nudge succeeded: A slips into the free neighbor.
            grid.get(free[0], free[1]).set(mover);
            if (s.FLASH_NEAR_MISS) {
                target.flashNearMiss(s.NEAR_MISS_FLASH_FRAMES);
            }
            return;
        }

        // No free cell in the row (or policy returned null by design).
        if (s.PASS_BEHAVIOR == Settings.PassBehavior.BOUNCE_BACK) {
            mover.flipDirection();
        }
        // STAY_PUT, BOUNCE_BACK-after-flip, or packed row: put A back where
        // it started, if that cell is still free.
        placeBackAtOrigin(grid, fromRow, fromCol, mover);
    }

    /**
     * Fallback: return A to its origin cell. Origin was cleared by
     * MovementRule before placements began, so it's usually free. In dense
     * traffic it may have been claimed by another mover; in that case the
     * object is dropped from the simulation (rare; logged for debugging).
     */
    private void placeBackAtOrigin(Grid grid, int row, int col, OrbitalObject mover) {
        Cell origin = grid.get(row, col);
        if (origin.isEmpty()) {
            origin.set(mover);
        } else {
            // Rare: A had nowhere to go. Drop it.
            System.err.println("NudgeRule: dropped " + mover
                    + " at (" + row + "," + col + ") — origin occupied");
        }
    }
}
