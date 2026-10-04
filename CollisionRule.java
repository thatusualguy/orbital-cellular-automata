import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Rule R2 — Collision.
 *
 * Does NOT run on its own (apply is a no-op). MovementRule holds an
 * instance and calls roll(...) / spawnDebris(...) when a mover lands on
 * an occupied target cell.
 */
public class CollisionRule implements Rule {

    @Override
    public void apply(Grid grid, Settings s, Random rng) {
        // no-op: invoked by MovementRule
    }

    /**
     * @return true if the two objects collide this encounter.
     *         Probability scales with combined size and combined speed.
     */
    public boolean roll(OrbitalObject a, OrbitalObject b, Settings s, Random rng) {
        float chance = collisionChance(a, b, s);
        return rng.nextFloat() < chance;
    }

    /** Exposed so tests can assert the formula without randomness. */
    public float collisionChance(OrbitalObject a, OrbitalObject b, Settings s) {
        float chance = s.COLLISION_BASE
                     * (a.getSize() + b.getSize())
                     * (a.getSpeed() + b.getSpeed());
        return Math.min(chance, s.COLLISION_MAX);
    }

    /**
     * Destroy both objects (already removed from the grid by the caller)
     * and scatter the resulting debris at the crash site and its neighbors.
     */
    public void spawnDebris(Grid grid, int row, int col,
                            OrbitalObject a, OrbitalObject b,
                            Settings s, Random rng) {
        int n = debrisCount(a, b, s);

        // Candidate cells: the crash cell plus its 8 neighbors, shuffled.
        List<int[]> spots = new ArrayList<int[]>();
        spots.add(new int[]{ row, col });
        spots.addAll(grid.neighbors8(row, col));
        Collections.shuffle(spots, rng);

        int placed = 0;
        for (int[] spot : spots) {
            if (placed >= n) break;
            Cell cell = grid.get(spot[0], spot[1]);
            if (!cell.isEmpty()) continue;
            OrbitalObject d = OrbitalObject.collisionDebris(
                s, rng,
                a.getSize(), b.getSize(),
                a.getSpeed(), b.getSpeed());
            cell.set(d);
            placed++;
        }
    }

    /** How many debris pieces this crash produces (capped). */
    public int debrisCount(OrbitalObject a, OrbitalObject b, Settings s) {
        float raw = s.DEBRIS_PER_COLLISION
                  * (a.getSize() + b.getSize())
                  * (a.getSpeed() + b.getSpeed())
                  * s.COLLISION_YIELD;
        int n = (int) Math.floor(raw);
        return Math.min(n, s.MAX_DEBRIS_PER_COLLISION);
    }
    
    /**
     * True when both objects are debris at or below SMALL_DEBRIS_SIZE.
     * In that case the collision removes both without spawning anything —
     * tiny fragments grinding together just vaporise.
     */
    public boolean isSmallDebrisMerge(OrbitalObject a, OrbitalObject b, Settings s) {
        return a.isDebris()
            && b.isDebris()
            && a.getSize() <= s.SMALL_DEBRIS_SIZE
            && b.getSize() <= s.SMALL_DEBRIS_SIZE;
    }
}
