package Components;

import Bus.SoftwareBus;
import Message.Message;

/**
 * Cabin Component.
 * <p>
 * This class acts as a <b>Physics Simulator</b> and Hardware Interface for the elevator car.
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 * <li>Simulates physical movement (Motor logic).</li>
 * <li>Tracks position (Floor alignment).</li>
 * <li>Publishes sensor data (Position/Status) to the bus for other components.</li>
 * </ul>
 * <p>
 * Because the system lacks physical alignment sensors, this class runs a dedicated
 * thread (`run`) to approximate travel time between floors.
 */
public class Cabin implements Runnable {

    // --- Internal Direction Constants (Matches CSV Protocol) ---
    private static final int DIR_UP = 0;
    private static final int DIR_DOWN = 1;
    private static final int DIR_NONE = 2;

    // --- Internal Movement Constants (Matches CSV Protocol) ---
    private static final int STATE_IDLE = 0;
    private static final int STATE_MOVING = 1;
    /**
     * Simulated time (in ms) to travel between one floor.
     * Reduced to 100ms for faster testing/simulation.
     */
    private static final int TRAVEL_TIME_MS = 100;
    private final SoftwareBus softwareBus;
    private final int elevatorID;
    // --- State Variables ---
    // Volatile is used here to ensure thread visibility between the
    // Simulation Thread (run) and the Main Logic Thread (goToFloor/getters).
    private volatile int currentFloor;
    private volatile int targetFloor;
    private volatile int currentDirection;
    private volatile boolean isMoving;
    /**
     * Controls the lifecycle of the simulation thread.
     * Set to false to cleanly exit the thread (e.g., during unit tests).
     */
    private volatile boolean running = true;

    /**
     * Constructs a new Cabin component and starts the physics simulation thread.
     *
     * @param softwareBus The bus to publish position/status updates to.
     * @param elevatorID  The ID of this elevator.
     */
    public Cabin(SoftwareBus softwareBus, int elevatorID) {
        this.softwareBus = softwareBus;
        this.elevatorID = elevatorID;

        // Initialize state (Default start at Floor 1)
        this.currentFloor = 1;
        this.targetFloor = 1;
        this.currentDirection = DIR_NONE;
        this.isMoving = false;

        // Broadcast initial state so system knows where we are on startup
        publishPosition();
        publishStatus();

        // Start Physics/Simulation Thread
        Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * The Physics Simulation Loop.
     * <p>
     * This method runs continuously to simulate the elevator motor.
     * logic:
     * <ol>
     * <li>Check if {@code currentFloor != targetFloor}.</li>
     * <li>If different, set direction and state to MOVING.</li>
     * <li>Wait for {@code TRAVEL_TIME_MS}.</li>
     * <li>Update {@code currentFloor}.</li>
     * <li>Publish new position (simulating a floor sensor trigger).</li>
     * <li>Repeat until target is reached.</li>
     * </ol>
     */
    @Override
    public void run() {
        while (running) {
            try {
                if (currentFloor != targetFloor) {
                    // 1. Determine Direction logic
                    int newDirection = (targetFloor > currentFloor) ? DIR_UP : DIR_DOWN;

                    // 2. Start Motor / State Update
                    if (!isMoving || currentDirection != newDirection) {
                        currentDirection = newDirection;
                        isMoving = true;
                        publishStatus(); // Notify bus: Moving + Direction
                    }

                    // 3. Simulate Travel Time (Wait for virtual "Alignment Sensor")
                    Thread.sleep(TRAVEL_TIME_MS);

                    // 4. Update Physical Position
                    if (currentDirection == DIR_UP) {
                        currentFloor++;
                    } else {
                        currentFloor--;
                    }

                    // 5. Publish New Position
                    publishPosition();

                } else {
                    // Target Reached
                    if (isMoving) {
                        isMoving = false;
                        currentDirection = DIR_NONE;
                        publishStatus(); // Notify bus: Idle + None
                    }
                    // Idle sleep to prevent CPU spin
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Stops the simulation thread.
     * <p>
     * <b>Critical for Unit Testing:</b> Prevents thread leaks and port binding
     * conflicts by allowing the test runner to shut down the cabin instance.
     */
    public void stop() {
        this.running = false;
    }

    // --- SAD Interface Methods ---

    /**
     * Sets the destination for the cabin motor.
     * Thread-safe (synchronized) to prevent race conditions with the physics loop.
     *
     * @param floor The target floor number (1-10).
     */
    public synchronized void goToFloor(int floor) {
        if (floor >= 1 && floor <= 10) {
            this.targetFloor = floor;
        }
    }

    /**
     * Retrieves the current physical status.
     *
     * @return int array {@code {currentFloor, currentDirection}}.
     */
    public int[] currentStatus() {
        return new int[]{currentFloor, currentDirection};
    }

    /**
     * Checks if the cabin has physically arrived at the target.
     *
     * @return {@code true} if current floor equals target floor.
     */
    public boolean arrived() {
        return currentFloor == targetFloor;
    }

    /**
     * Gets the current target floor.
     *
     * @return The floor the cabin is currently trying to reach.
     */
    public int getTargetFloor() {
        return targetFloor;
    }

    // --- Internal Bus Helpers ---

    /**
     * Publishes Topic 202 (Car Position).
     * Simulates a floor sensor triggering as the car passes a floor.
     */
    private void publishPosition() {
        softwareBus.publish(new Message(202, elevatorID, currentFloor));
    }

    /**
     * Publishes Topic 207 (Direction) and 208 (Movement State).
     */
    private void publishStatus() {
        softwareBus.publish(new Message(207, elevatorID, currentDirection));
        softwareBus.publish(new Message(208, elevatorID, isMoving ? STATE_MOVING : STATE_IDLE));
    }
}