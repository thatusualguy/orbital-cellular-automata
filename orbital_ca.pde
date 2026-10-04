import java.util.*;

// OrbitalCA.pde
// Cellular-automaton simulation of orbital debris.
// Spec references: §7 (visualization), §8 (interaction), §9 (simulate).

Settings        cfg;
Grid            grid;
ArrayList<Rule> rules;
Random          rng;
boolean         running   = false;
int             stepCount = 0;
float           cx, cy;

static final int HIST_LEN = 1000;
int[] histSat  = new int[HIST_LEN];
int[] histDeb  = new int[HIST_LEN];
int   histHead = 0;      // next write index
int   histSize = 0;      // how many valid samples (caps at HIST_LEN)
int   histMax  = 1;      // running max for auto-scaling the y-axis

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------

void setup() {
    size(1200, 900);
    smooth();
    cx = width  / 2f;
    cy = height / 2f;
    cfg = new Settings();
    resetSim();
}

void resetSim() {
    rng   = new Random(cfg.RANDOM_SEED);
    grid  = new Grid(cfg.ROWS, cfg.COLS);
    rules = new ArrayList<Rule>();
    rules.add(new MovementRule());   // owns CollisionRule + NudgeRule internally
    rules.add(new LaunchRule());
    rules.add(new CleanupRule());
    rules.add(new DecayRule());
    rules.add(new AgingRule());
    stepCount = 0;
    histHead = 0;
    histSize = 0;
    histMax  = 1;
    for (int i = 0; i < HIST_LEN; i++) { histSat[i] = 0; histDeb[i] = 0; }
    running   = false;
}

void draw() {
    if (running) simulate();
    render();
}

void simulate() {
    for (Rule r : rules) r.apply(grid, cfg, rng);
    grid.tickFlashes();
    stepCount++;
    recordHistory();
}

// ---------------------------------------------------------------------------
// Input
// ---------------------------------------------------------------------------

void keyPressed() {
    if (key == ' ') {
        running = !running;
    } else if (keyCode == RIGHT || key == 'd' || key == 'D') {
        simulate();
    } else if (key == 'r' || key == 'R') {
        resetSim();
    } else if (key == 's' || key == 'S'){
        cfg.LAUNCHING = !cfg.LAUNCHING;
    }
}

// ---------------------------------------------------------------------------
// Rendering
// ---------------------------------------------------------------------------

void render() {
    background(8, 10, 20);

    drawOrbitRings();
    drawEarth();
    drawObjects();
    drawFlashes();
    drawHUD();
    drawHistoryGraph();
}

void recordHistory() {
    histSat[histHead] = grid.count(OrbitalObject.Type.SATELLITE);
    histDeb[histHead] = grid.count(OrbitalObject.Type.DEBRIS);
    histHead = (histHead + 1) % HIST_LEN;
    if (histSize < HIST_LEN) histSize++;

    // auto-scale: max over the current window
    histMax = 1;
    for (int i = 0; i < histSize; i++) {
        int idx = (histHead - 1 - i + HIST_LEN * 2) % HIST_LEN;
        histMax = Math.max(histMax, histSat[idx]);
        histMax = Math.max(histMax, histDeb[idx]);
    }
}

void drawHistoryGraph() {
    int gw = 300;
    int gh = 120;
    int gx = width - gw - 16;
    int gy = 16;

    // panel background
    noStroke();
    fill(0, 0, 0, 150);
    rect(gx, gy, gw, gh, 6);

    // title
    fill(180);
    textAlign(LEFT, TOP);
    textSize(11);
    text("Last " + HIST_LEN + " steps   (max " + histMax + ")", gx + 8, gy + 6);

    // plot area
    float px = gx + 8;
    float py = gy + 24;
    float pw = gw - 16;
    float ph = gh - 32;

    // faint frame
    noFill();
    stroke(60, 70, 90);
    strokeWeight(1);
    rect(px, py, pw, ph);

    if (histSize < 2) return;

    // helper: draw one series
    drawSeries(histSat, px, py, pw, ph, color( 80, 220, 255), 1.2f);
    drawSeries(histDeb, px, py, pw, ph, color(255, 140,  60), 1.2f);

    // legend
    noStroke();
    textSize(10);

    fill( 80, 220, 255);
    rect(px + 4, py + 4, 8, 8);
    fill(220);
    text("sat", px + 16, py + 3);

    fill(255, 140, 60);
    rect(px + 44, py + 4, 8, 8);
    fill(220);
    text("debris", px + 56, py + 3);
}

/**
 * Draw one history series into the plot rect.
 * Oldest sample is drawn at the left; newest at the right.
 */
void drawSeries(int[] data, float px, float py, float pw, float ph, int col, float weight) {
    noFill();
    stroke(col);
    strokeWeight(weight);

    float denom = Math.max(1, histSize - 1);
    float yScale = ph / (float) histMax;

    beginShape();
    for (int i = 0; i < histSize; i++) {
        // i = 0 is oldest; walk backwards from histHead-1
        int idx  = (histHead - 1 - i + HIST_LEN * 2) % HIST_LEN;
        float x  = px + pw * (i / denom);
        float y  = py + ph - (data[idx] * yScale);
        vertex(x, y);
    }
    endShape();
}

void drawOrbitRings() {
    noFill();
    strokeWeight(1);
    for (int r = 0; r < grid.rows(); r++) {
        float t = (grid.rows() <= 1) ? 0f : (float) r / (grid.rows() - 1);
        // brighter near Earth, fainter near the outer edge
        stroke(40, 70, 110, 60 + 60 * (1f - t));
        float rad = radiusForRow(r);
        ellipse(cx, cy, rad * 2f, rad * 2f);
    }
}

void drawEarth() {
    noStroke();
    // ocean
    fill(30, 90, 180);
    ellipse(cx, cy, cfg.EARTH_RADIUS * 2f, cfg.EARTH_RADIUS * 2f);
    // landmass hints (simple — spec says "simple shape of Earth")
    fill(60, 140, 90);
    ellipse(cx - 18, cy - 10, 34, 22);
    ellipse(cx + 12, cy + 14, 26, 20);
    ellipse(cx + 26, cy -  8, 16, 20);
    ellipse(cx - 10, cy + 26, 20, 14);
    // subtle atmosphere halo
    noFill();
    stroke(120, 180, 255, 60);
    strokeWeight(6);
    ellipse(cx, cy, cfg.EARTH_RADIUS * 2f + 12, cfg.EARTH_RADIUS * 2f + 12);
}

void drawObjects() {
    noStroke();
    for (int r = 0; r < grid.rows(); r++) {
        for (int c = 0; c < grid.cols(); c++) {
            Cell cell = grid.get(r, c);
            if (cell.isEmpty()) continue;

            OrbitalObject o = cell.getObject();
            float px = projectX(r, c);
            float py = projectY(r, c);
            float d  = pixelDiameter(o.getSize());

            if (o.isSatellite()) {
                // glow
                fill( 80, 220, 255, 70);
                ellipse(px, py, d * 3f, d * 3f);
                // core
                fill(210, 250, 255);
                ellipse(px, py, d, d);
            } else {
                fill(255, 90, 40, 70);
                ellipse(px, py, d * 3f, d * 3f);
                fill(255, 165, 70);
                ellipse(px, py, d, d);
            }
        }
    }
}

void drawFlashes() {
    if (!cfg.FLASH_NEAR_MISS) return;
    noFill();
    strokeWeight(1.5f);
    for (int r = 0; r < grid.rows(); r++) {
        for (int c = 0; c < grid.cols(); c++) {
            Cell cell = grid.get(r, c);
            if (!cell.isFlashing()) continue;

            float a = cell.flashAlpha(cfg.NEAR_MISS_FLASH_FRAMES); // 1 -> 0
            float t = 1f - a;                                      // 0 -> 1
            float ringR = 4f + t * 16f;                            // expands
            stroke(255, 245, 160, a * 200f);
            float px = projectX(r, c);
            float py = projectY(r, c);
            ellipse(px, py, ringR * 2f, ringR * 2f);
        }
    }
}

void drawHUD() {
    int sats = grid.count(OrbitalObject.Type.SATELLITE);
    int debs = grid.count(OrbitalObject.Type.DEBRIS);

    // translucent plate
    noStroke();
    fill(0, 0, 0, 140);
    rect(8, 8, 320, 88, 6);

    fill(220);
    textAlign(LEFT, TOP);
    textSize(13);
    text("Step: " + stepCount + "     " + (running ? "RUNNING" : "PAUSED"),
         16, 14);

    // colour key + counts
    fill( 80, 220, 255);
    ellipse(22, 44, 8, 8);
    fill(220);
    text("Satellites: " + sats, 34, 37);

    fill(255, 165, 70);
    ellipse(22, 64, 8, 8);
    fill(220);
    text("Debris: " + debs, 34, 57);

    fill(160);
    textSize(11);
    text("Space: run/pause  Right: sim step   R: reset  S: stop/launch satellites", 16, 80);
}

// ---------------------------------------------------------------------------
// Geometry helpers
// ---------------------------------------------------------------------------

float radiusForRow(int row) {
    float t = (grid.rows() <= 1) ? 0f : (float) row / (grid.rows() - 1);
    return cfg.EARTH_RADIUS + cfg.ALT_MIN
         + t * (cfg.ALT_MAX - cfg.ALT_MIN);
}

float projectX(int row, int col) {
    float angle = TWO_PI * col / grid.cols();
    return cx + radiusForRow(row) * cos(angle);
}

float projectY(int row, int col) {
    float angle = TWO_PI * col / grid.cols();
    return cy + radiusForRow(row) * sin(angle);
}

float pixelDiameter(float objSize) {
    // size range roughly 0.1 .. 5.0 in default settings
    return 2f + objSize * 1.4f;
}
