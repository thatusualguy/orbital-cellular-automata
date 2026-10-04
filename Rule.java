/**
 * A single simulation step. Rules are applied in the order registered in
 * the main sketch (see OrbitalCA.pde).
 *
 * Rules mutate the grid and the objects inside it. They never touch
 * rendering or input — that's the sketch's job.
 */
public interface Rule {
    void apply(Grid grid, Settings s, java.util.Random rng);
}
