package Modes;

import ElevatorController.Buttons;
import ElevatorController.Cabin;
import ElevatorController.DoorAssembly;
import ElevatorController.Notifier;

/**
 * Fire mode behavior.
 *
 * In Fire Safety mode the elevator ignores normal hall calls and
 * immediately recalls to a designated recall floor (floor 1).
 * Once recalled, the doors are opened and kept open until fire mode
 * is cleared. This class is intended to be used by the main controller
 * whenever Mode == FIRE_SAFETY.
 *
 * Design:
 *  - On entry: disable hall calls and enable single cabin selection.
 *  - While not yet at floor 1: close doors (if safe) and command Cabin
 *    to travel to floor 1.
 *  - Once at floor 1: open doors and hold there, ignoring normal calls.
 *  - Overload always has priority: doors remain open and an alarm plays.
 */
public class Fire {

    // Direction codes
    private static final int DIR_NONE = 2;

    private static final int RECALL_FLOOR = 1;

    // References to the rest of the Elevator Controller
    private final Buttons buttons;
    private final Cabin cabin;
    private final DoorAssembly doors;
    private final Notifier notifier;

    /**
     * True once the car has successfully recalled to the recall floor
     * and opened its doors.
     */
    private boolean recallComplete;

    /**
     * Create a Fire-mode controller for one elevator.
     *
     * @param buttons  Buttons component for this elevator.
     * @param cabin    Cabin component for this elevator.
     * @param doors    DoorAssembly component for this elevator.
     * @param notifier Notifier component for this elevator.
     */
    public Fire(Buttons buttons,
                Cabin cabin,
                DoorAssembly doors,
                Notifier notifier) {

        this.buttons = buttons;
        this.cabin = cabin;
        this.doors = doors;
        this.notifier = notifier;


        if (buttons != null) {
            buttons.disableCalls();
            buttons.enableSingleRequest();
        }

        this.recallComplete = false;
    }

    /**
     * Main entry point while Mode == FIRE_SAFETY.
     * Call this once per main controller loop iteration.
     */
    public void step() {

        // 1. Overload always has highest priority.
        if (doors.overCapacity()) {
            doors.open();
            notifier.playCapacityNoise();
            return;
        } else {
            notifier.stopCapacityNoise();
        }

        // 2. If recall not finished yet, perform recall sequence.
        if (!recallComplete) {
            handleRecallToFloor();
        } else {
            // 3. Once recalled: keep doors open at the recall floor.
            maintainRecallState();
        }
    }

    /**
     * Handles the recall sequence from the current floor to RECALL_FLOOR.
     */
    private void handleRecallToFloor() {
        int[] status = cabin.currentStatus();
        int currentFloor = status[0];

        // If we are already at the recall floor, mark recall complete and open doors.
        if (currentFloor == RECALL_FLOOR) {
            recallComplete = true;
            doors.open();
            notifier.arrivedAtFloor(RECALL_FLOOR, DIR_NONE);
            return;
        }

        // If doors are obstructed, keep them open until obstruction clears.
        if (doors.obstructed()) {
            doors.open();
            return;
        }

        // If doors are not fully closed, close them before moving.
        if (!doors.fullyClosed()) {
            doors.close();
            return;
        }

        // Doors are closed and clear, command the cabin to travel to recall floor.
        cabin.goToFloor(RECALL_FLOOR);
    }

    /**
     * Maintains the car in an idle state at the recall floor (set to floor 1) with doors open.
     * If the car goes away from the recall floor, the recall sequence
     * will be reinitiated.
     */
    private void maintainRecallState() {
        int[] status = cabin.currentStatus();
        int currentFloor = status[0];

        // If we are no longer at the recall floor re-initiate recall.
        if (currentFloor != RECALL_FLOOR) {
            recallComplete = false;
            return;
        }

        // Ensure doors remain open
        if (!doors.fullyOpen()) {
            doors.open();
        }

        // Car is at recall floor, no direction.
        notifier.elevatorStatus(RECALL_FLOOR, DIR_NONE);
    }
}