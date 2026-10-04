import java.util.Random;

/**
 * Rule R7 — Debris aging.
 *
 * Every step, each debris object's age increments. When age reaches its
 * randomly-assigned maxAge (which scales with size), the debris is removed.
 *
 * Satellites are unaffected (maxAge == -1 -> ages() == false).
 *
 * Runs after Movement so a piece of debris gets one last move on the step
 * it expires.
 */
public class AgingRule implements Rule {

    @Override
    public void apply(Grid grid, Settings s, Random rng) {
        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) {
                Cell cell = grid.get(r, c);
                if (cell.isEmpty()) continue;

                OrbitalObject o = cell.getObject();
                if (!o.ages()) continue;

                o.incrementAge();
                if (o.isExpired()) {
                    cell.clear();
                }
            }
        }
    }
}
