# Cebu: Rising Waters

A small Java Swing campaign game based on the supplied game flow and mechanics diagrams. It has no external dependencies.

## Run

Install a Java Development Kit (JDK) 17 or newer, open this folder in a terminal, then run:

```text
javac FloodRescue.java
java FloodRescue
```

On Windows, `run.bat` performs those steps for you.

## Controls

- **WASD** or **arrow keys**: move the green player box in 2D
- **Shift**: temporary morale speed boost
- **G**: use a garbage clearer for the mission
- **R**: restore one health point for the mission
- **Esc**: pause or resume a mission
- **Enter**: start, launch, or advance

## Campaign

Four missions progress from early rainfall in Talamban to the final storm in Guadalupe. Level 1 teaches movement, the single current lane, obstacle navigation, survivor pickup, and safe-zone delivery through on-screen prompts and a moving waypoint. Faster acceleration and friction make movement more responsive, while obstacle hits now bounce the player clear and briefly protect against repeated damage. Moving water ripples, floating survivor markers, a pulsing safe zone, pickup and delivery rings, and boost glow add game feel. Move the green box in two dimensions around solid obstacles, collect survivors (up to the player's capacity), and deliver them to the marked safe zone. Currents push only inside the horizontal arrow-marked lanes; outside them, movement is player-controlled. Later missions add stronger currents and landslide hazards. Completing a mission awards supplies for health, rescue capacity, and movement speed upgrades.

This is a playable prototype: the maps and art are drawn in Java, and the levels use authored obstacle layouts with procedural survivor placement.

