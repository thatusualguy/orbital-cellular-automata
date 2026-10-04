import java.util.Random;

/**
 * Rule R0 — Pre-pass safety net.
 *
 * The movement model (snapshot -> clear -> place) cannot produce a cell
 * with more than one object, and Cell.set() enforces the invariant at the
 * type level. This rule exists to make the intent explicit in the pipeline
 * and as a hook for future rules that might temporarily violate it (e.g.
 * a vertical-drift rule that pushes objects together).
 */
public class ResolveOverlapsRule implements Rule {

    @Override
    public void apply(Grid grid, Settings s, Random rng) {
        // No-op under the current invariants. Kept for pipeline symmetry.
    }
}
