import java.util.Random;

/**
 * Rule R3 — Satellite launch.
 *
 * Each step, with probability LAUNCH_PROB, spawn one satellite in a random
 * empty cell within the launch altitude band [LAUNCH_ROW_MIN..LAUNCH_ROW_MAX].
 */
public class LaunchRule implements Rule {

    private static final int MAX_ATTEMPTS = 20;

    @Override
    public void apply(Grid grid, Settings s, Random rng) {
        if (!s.LAUNCHING) return;
        if (rng.nextFloat() >= s.LAUNCH_PROB) return;

        int rowMin = Math.max(0, s.LAUNCH_ROW_MIN);
        int rowMax = Math.min(grid.rows() - 1, s.LAUNCH_ROW_MAX);
        if (rowMax < rowMin) return;

        int span = rowMax - rowMin + 1;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int r = rowMin + rng.nextInt(span);
            int c = rng.nextInt(grid.cols());
            if (grid.isEmpty(r, c)) {
                grid.get(r, c).set(OrbitalObject.randomSatellite(s, rng));
                return;
            }
        }
        // Launch band full this step — skip.
    }
}
