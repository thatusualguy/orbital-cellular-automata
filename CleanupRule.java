import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rule R4 — Active debris removal.
 *
 * Each step, with probability CLEANUP_PROB, remove one random debris object
 * from anywhere in the grid.
 */
public class CleanupRule implements Rule {

    @Override
    public void apply(Grid grid, Settings s, Random rng) {
        if (rng.nextFloat() >= s.CLEANUP_PROB) return;

        List<int[]> debris = new ArrayList<int[]>();
        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) {
                if (grid.get(r, c).hasDebris()) {
                    debris.add(new int[]{ r, c });
                }
            }
        }
        if (debris.isEmpty()) return;

        int[] pick = debris.get(rng.nextInt(debris.size()));
        grid.get(pick[0], pick[1]).clear();
    }
}
