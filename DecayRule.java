import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rule R5 — Atmospheric decay.
 *
 * Each step, with probability DECAY_PROB, remove one debris object from the
 * innermost rows [0..DECAY_ROW_MAX] (i.e. the lowest altitude band).
 * Satellites are unaffected — they can boost.
 */
public class DecayRule implements Rule {

    @Override
    public void apply(Grid grid, Settings s, Random rng) {
        if (rng.nextFloat() >= s.DECAY_PROB) return;

        int rowMax = Math.min(grid.rows() - 1, s.DECAY_ROW_MAX);

        List<int[]> candidates = new ArrayList<int[]>();
        for (int r = 0; r <= rowMax; r++) {
            for (int c = 0; c < grid.cols(); c++) {
                if (grid.get(r, c).hasDebris()) {
                    candidates.add(new int[]{ r, c });
                }
            }
        }
        if (candidates.isEmpty()) return;

        int[] pick = candidates.get(rng.nextInt(candidates.size()));
        grid.get(pick[0], pick[1]).clear();
    }
}
