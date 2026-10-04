# Orbital Trash CA

A 2D cellular automaton that simulates the accumulation of orbital debris
around Earth. Satellites are launched into orbit, collide with each
other and with existing debris, fragment into new debris, and eventually
decay. The grid is drawn as a donut wrapped around around Earth.

Built in Processing (Java). All values are tuned from a `Settings` file.

## Demos

**Kessler cascade** - a scenario where collisions in low Earth orbit create new space debris that causes more collisions, creating a self-sustaining chain reaction.

https://github.com/user-attachments/assets/edae7cd4-5ade-47d4-ba72-7f86c2f8c5f4

**No Kessler cascade**

https://github.com/user-attachments/assets/8f1e6e0f-e884-4a39-b9ce-c8ae2faf4bf3

## Concept

The grid has two axes:

- **Rows** - altitude orbit bands. Row 0 is closest to Earth. Rows do **not** wrap.
- **Columns** - angular position around the planet. Columns **wrap** on the sides of grid.

Each cell holds at most one object and can be:

- empty       - nothing in this slot                        
- satellite   - spacecraft, has size/speed/direction 
- debris      - fragment, has size/speed/direction/age      



## Rules

Applied in order every step. Each is a small class implementing `Rule`.

| # | Rule | What it does |
|---|------|--------------|
| R1 | `MovementRule` | Objects advance by `speed × direction`; wraps columns. |
| R2 | `CollisionRule` | Chance scales with combined size and speed. On hit, both objects are destroyed and spawn debris. |
| R3 | `LaunchRule` | Occasionally spawns a satellite. Toggleable. |
| R4 | `CleanupRule` | Occasionally removes one random debris object. |
| R5 | `DecayRule` | Innermost debris can burn up in the atmosphere. |
| R6 | `NudgeRule` | On a failed collision roll, the mover slides past the blocker. |
| R7 | `AgingRule` | Every debris has a random lifespan, but larger fragments live longer. |

To keep the amount of fragments in bay, two small debris colliding just annihilate - no new fragments spawn.



## Running

### Using Processing IDE

1. Install [Processing 4](https://processing.org/download).
2. Open `orbital_ca.pde`.
3. Press `Play`.

### Download JAR

1. Have `java` installed.
1. Go to the [**Releases**](../../releases/latest) page of this repository.
2. Download the archive for your platform & extract.
3. Launch the app:
    - **Linux**: make the launcher executable and run it:
        ```
        chmod +x orbital_ca
        ./orbital_ca
        ```
    - **Windows**: run java? // TODO

### Controls

| Key            | Action                        |
|----------------|-------------------------------|
| `Space`        | Run / pause                   |
| `right` or `D` | Step once                     |
| `R`            | Reset with the seeded RNG     |
| `S`            | Toggle satellite launches     |



## Settings

Every simulation parameter lives in `Settings.java`.

Key knobs:

- `LAUNCH_PROB`, `LAUNCHING` - how often new satellites appear.
- `COLLISION_BASE`, `COLLISION_MAX` - how likely encounters are to end in collision.
- `DEBRIS_PER_COLLISION`, `MAX_DEBRIS_PER_COLLISION` - fragment yield.
- `SMALL_DEBRIS_SIZE` - threshold below which two debris annigilate.
- `DEBRIS_AGE_BASE`, `DEBRIS_AGE_PER_SIZE`, `DEBRIS_AGE_JITTER` - lifespan
  distribution.
- `CLEANUP_PROB`, `DECAY_PROB` - active cleanup and atmospheric drag.
- `PASS_BEHAVIOR` - encounter non-collision policy: nudge ahead,
  random nudge, stay put, bounce back.

---

## Extending

Rules are pure functions of `(grid, settings, rng)`.

Add a new rule:

1. Implement `Rule`.
2. Put any new parameters in `Settings`.
3. Append the rule to the list in `orbital_ca.pde`'s `resetSim()`.



## License

MIT