package ElevatorController;

import Bus.SoftwareBus;
import Message.Message;

/**
 * Buttons Component.
 * <p>
 * Manages the logic for scheduling elevator destinations. It tracks two types of inputs:
 * <ol>
 * <li><b>Cabin Requests:</b> Buttons pressed inside the elevator (Topic 201).</li>
 * <li><b>Hall Calls:</b> Buttons pressed on floors (Topic 200).</li>
 * </ol>
 * <p>
 * This component implements the primary dispatch algorithm (LOOK/SCAN) to determine
 * the next target floor based on the current direction and pending requests.
 */
public class Buttons {

    private final SoftwareBus bus;
    private final int elevatorID;

    // --- State Storage ---
    // Arrays index 1-10 represent floors. Index 0 is unused.
    private final boolean[] cabinRequests = new boolean[11];
    private final boolean[] upHallCalls = new boolean[11];
    private final boolean[] downHallCalls = new boolean[11];

    /**
     * Constructs a new Buttons component.
     *
     * @param bus        The SoftwareBus for receiving button presses and sending resets.
     * @param elevatorID The ID of the elevator.
     */
    public Buttons(SoftwareBus bus, int elevatorID) {
        this.bus = bus;
        this.elevatorID = elevatorID;

        // Subscribe to Hall Calls (Topic 200)
        // Subtopic 0 acts as a wildcard to receive calls from all floors
        bus.subscribe(200, 0);

        // Subscribe to Cabin Requests (Topic 201)
        // Subtopic is the elevator ID (listen only for this car's buttons)
        bus.subscribe(201, elevatorID);
    }

    /**
     * Internal helper to drain the message queue and update internal boolean arrays.
     * <p>
     * This uses a `while` loop to process <i>all</i> pending messages in the buffer
     * to ensure the dispatch logic considers every button press that occurred
     * since the last cycle.
     */
    private void updateState() {
        Message msg;

        // 1. Process Hall Calls (Topic 200)
        while ((msg = bus.get(200, 0)) != null) {
            int floor = msg.getSubTopic();
            int direction = msg.getBody(); // 0 = Up, 1 = Down

            // Validate floor range before storing
            if (floor >= 1 && floor <= 10) {
                if (direction == 0) {
                    upHallCalls[floor] = true;
                } else if (direction == 1) {
                    downHallCalls[floor] = true;
                }
            }
        }

        // 2. Process Cabin Requests (Topic 201)
        while ((msg = bus.get(201, elevatorID)) != null) {
            int floor = msg.getBody();
            if (floor >= 1 && floor <= 10) {
                cabinRequests[floor] = true;
            }
        }
    }

    /**
     * Calculates the next destination floor.
     * <p>
     * Implements the <b>LOOK/SCAN Algorithm</b>:
     * <ul>
     * <li>If moving, continue in the current direction if there are requests ahead.</li>
     * <li>If no requests ahead, check for "reversal calls" (furthest call in opposite direction).</li>
     * <li>If idle, scan for the nearest request.</li>
     * </ul>
     *
     * @param currentFloor     The elevator's current location.
     * @param currentDirection The elevator's current status (0=Up, 1=Down, 2=Idle).
     * @return int array containing {@code {TargetFloor, DirectionToTravel}}.
     * Returns {@code {-1, 2}} if no pending requests exist.
     */
    public int[] nextService(int currentFloor, int currentDirection) {
        updateState(); // Poll bus for fresh input data

        // CASE 1: Elevator is Idle (2)
        // Heuristic: Scan from bottom up for any request.
        if (currentDirection == 2) {
            for (int f = 1; f <= 10; f++) {
                if (cabinRequests[f] || upHallCalls[f] || downHallCalls[f]) {
                    // Determine direction needed to reach the request
                    int dir = (f > currentFloor) ? 0 : (f < currentFloor ? 1 : 2);
                    return new int[]{f, dir};
                }
            }
            return new int[]{-1, 2}; // Remain Idle
        }

        // CASE 2: Moving UP (0)
        if (currentDirection == 0) {
            // A. Check for requests above current floor
            for (int f = currentFloor + 1; f <= 10; f++) {
                // Stop if: User wants this floor (Cabin) OR Someone wants to go Up (Hall)
                if (cabinRequests[f] || upHallCalls[f]) {
                    return new int[]{f, 0};
                }
            }
            // B. Look for Reversal: Highest pending Down call
            for (int f = 10; f > currentFloor; f--) {
                if (downHallCalls[f]) {
                    return new int[]{f, 0};
                }
            }
            // If no requests above, fall through to re-evaluate (usually triggers idle/down logic)
            return nextService(currentFloor, 2);
        }

        // CASE 3: Moving DOWN (1)
        if (currentDirection == 1) {
            // A. Check for requests below current floor
            for (int f = currentFloor - 1; f >= 1; f--) {
                // Stop if: User wants this floor (Cabin) OR Someone wants to go Down (Hall)
                if (cabinRequests[f] || downHallCalls[f]) {
                    return new int[]{f, 1};
                }
            }
            // B. Look for Reversal: Lowest pending Up call
            for (int f = 1; f < currentFloor; f++) {
                if (upHallCalls[f]) {
                    return new int[]{f, 1};
                }
            }
            // If no requests below, re-evaluate
            return nextService(currentFloor, 2);
        }

        return new int[]{-1, 2};
    }

    /**
     * Resets a Hall Call after it has been serviced.
     * <p>
     * Sends a message (Topic 110) to the hardware to turn off the button light.
     *
     * @param floor     The floor number.
     * @param direction The button type (0=Up, 1=Down).
     */
    public void callReset(int floor, int direction) {
        // Clear internal state immediately
        if (direction == 0) upHallCalls[floor] = false;
        if (direction == 1) downHallCalls[floor] = false;

        // Publish Reset Command
        bus.publish(new Message(110, floor, direction));
    }

    /**
     * Resets a Cabin Request after arrival.
     * <p>
     * Sends a message (Topic 109) to the hardware to turn off the button light inside the car.
     *
     * @param floor The floor number.
     */
    public void requestReset(int floor) {
        // Clear internal state immediately
        if (floor >= 1 && floor <= 10) {
            cabinRequests[floor] = false;
        }

        // Publish Reset Command
        bus.publish(new Message(109, elevatorID, floor));
    }

    // --- Control Enable/Disable Methods (Topics 113-115) ---

    /**
     * Enables all hall call buttons.
     */
    public void enableCalls() {
        // Topic 113: Calls Enabled, Body 1 = True
        bus.publish(new Message(113, 0, 1));
    }

    /**
     * Disables all hall call buttons (e.g., during maintenance or fire mode).
     */
    public void disableCalls() {
        // Topic 113: Calls Enabled, Body 0 = False
        bus.publish(new Message(113, 0, 0));
    }

    /**
     * Enables standard multi-selection mode for cabin buttons.
     */
    public void enableAllRequests() {
        // Topic 114: Selections Enabled
        bus.publish(new Message(114, 0, 1));
        // Topic 115: Selection Type (1 = Multiple/Normal)
        bus.publish(new Message(115, 0, 1));
    }

    /**
     * Enables single-selection mode (e.g., for specific Fire/Emergency phases).
     */
    public void enableSingleRequest() {
        // Topic 114: Selections Enabled
        bus.publish(new Message(114, 0, 1));
        // Topic 115: Selection Type (0 = Single)
        bus.publish(new Message(115, 0, 0));
    }
}