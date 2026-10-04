import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rule R1 — Movement (with collision and nudge resolved inline).
 *
 * Algorithm (snapshot -> clear -> place):
 *   1. Snapshot every object with speed > 0 and record its (row, col).
 *   2. Clear those origin cells.
 *   3. For each snapshot entry in order, compute the wrapped target column
 *      and try to place:
 *        - target empty          -> place
 *        - target occupied       -> roll collision; on hit destroy both and
 *                                   spawn debris; on miss, nudge.
 *
 * Stationary objects are never cleared, so a mover can collide with them
 * naturally. Two movers whose paths cross simply swap — no collision, which
 * matches the "different altitudes" interpretation from the spec.
 */
public class MovementRule implements Rule {

    private final CollisionRule collision;
    private final NudgeRule     nudge;

    public MovementRule() {
        this(new CollisionRule(), new NudgeRule());
    }

    public MovementRule(CollisionRule collision, NudgeRule nudge) {
        this.collision = collision;
        this.nudge     = nudge;
    }

    @Override
    public void apply(Grid grid, Settings s, Random rng) {
        // 1. Snapshot movers.
        List<int[]>          origins = new ArrayList<int[]>();
        List<OrbitalObject>  movers  = new ArrayList<OrbitalObject>();

        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) {
                Cell cell = grid.get(r, c);
                if (cell.isEmpty()) continue;
                OrbitalObject o = cell.getObject();
                if (o.getSpeed() == 0) continue;
                origins.add(new int[]{ r, c });
                movers.add(o);
            }
        }

        // 2. Clear their origin cells.
        for (int[] p : origins) {
            grid.get(p[0], p[1]).clear();
        }

        // 3. Place each mover.
        for (int i = 0; i < movers.size(); i++) {
            int fromRow = origins.get(i)[0];
            int fromCol = origins.get(i)[1];
            OrbitalObject o = movers.get(i);

            int targetCol = grid.wrapCol(fromCol + o.signedSpeed());
            Cell target   = grid.get(fromRow, targetCol);

            if (target.isEmpty()) {
                target.set(o);
                continue;
            }

            // Target occupied: collision or nudge.
            OrbitalObject other = target.getObject();
            if (collision.roll(o, other, s, rng)) {
                target.clear();                          // remove B; A is not placed
                if (collision.isSmallDebrisMerge(o, other, s)) {
                    // silent annihilation — no debris spawned
                } else {
                    collision.spawnDebris(grid, fromRow, targetCol, o, other, s, rng);
                }
            } else {
                nudge.resolve(grid,
                        fromRow, fromCol,
                        fromRow, targetCol,
                        o, target,
                        s, rng);
            }
        }
    }

    // Expose the helpers so other rules or tests can reach them.
    public CollisionRule collision() { return collision; }
    public NudgeRule     nudge()     { return nudge; }
}
