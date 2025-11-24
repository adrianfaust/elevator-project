package Components;

import Bus.SoftwareBus;
import Message.Message;

/**
 * Door Assembly Component.
 * <p>
 * A virtualization of the physical door interfaces and sensors.
 * It serves two primary roles:
 * <ol>
 * <li><b>Actuator:</b> Sends commands to the door motor (Topic 100).</li>
 * <li><b>Sensor Interface:</b> Aggregates data from Obstruction, Position, and Weight sensors.</li>
 * </ol>
 * <p>
 * <b>Design Pattern:</b> Passive Polling.
 * This component does not maintain a background thread. Instead, it polls the
 * {@link SoftwareBus} queue for updates only when its state is requested via
 * an accessor method (e.g., {@link #obstructed()}).
 */
public class DoorAssembly {

    private final SoftwareBus bus;
    private final int elevatorID;

    // --- Internal State Cache ---
    private boolean isObstructed;
    private boolean isFullyOpen;
    private boolean isFullyClosed;
    private boolean isOverloaded;

    /**
     * Constructs a new DoorAssembly.
     *
     * @param bus        The SoftwareBus for communication.
     * @param elevatorID The ID of the elevator.
     */
    public DoorAssembly(SoftwareBus bus, int elevatorID) {
        this.bus = bus;
        this.elevatorID = elevatorID;

        // Initialize default safe state
        this.isFullyClosed = true;
        this.isFullyOpen = false;
        this.isObstructed = false;
        this.isOverloaded = false;

        // --- Subscriptions ---
        // Topic 203: Door sensor (Obstruction)
        bus.subscribe(203, elevatorID);

        // Topic 204: Door status (Open/Closed Position)
        bus.subscribe(204, elevatorID);

        // Topic 205: Cabin load (Overload Sensor)
        bus.subscribe(205, elevatorID);
    }

    /**
     * Internal helper to process pending messages and update cached state.
     * <p>
     * This is called automatically at the start of every accessor method.
     * It iterates through the bus queue to find the most recent sensor readings.
     */
    private void updateState() {
        Message msg;

        // 1. Check Obstruction (Topic 203)
        while ((msg = bus.get(203, elevatorID)) != null) {
            // Body 0 = Obstructed, 1 = Clear
            this.isObstructed = (msg.getBody() == 0);
        }

        // 2. Check Door Status (Topic 204)
        while ((msg = bus.get(204, elevatorID)) != null) {
            // Body 0 = Open, 1 = Closed
            int status = msg.getBody();
            if (status == 0) {
                this.isFullyOpen = true;
                this.isFullyClosed = false;
            } else if (status == 1) {
                this.isFullyOpen = false;
                this.isFullyClosed = true;
            }
        }

        // 3. Check Load (Topic 205)
        while ((msg = bus.get(205, elevatorID)) != null) {
            // Body 0 = Normal, 1 = Overloaded
            this.isOverloaded = (msg.getBody() == 1);
        }
    }

    // --- SAD Interface Methods ---

    /**
     * Commands the door motor to open.
     * Publishes Topic 100 with Body 0.
     */
    public void open() {
        // Topic 100: Door control, Body: 0 = Open
        bus.publish(new Message(100, elevatorID, 0));
    }

    /**
     * Commands the door motor to close.
     * Publishes Topic 100 with Body 1.
     */
    public void close() {
        // Topic 100: Door control, Body: 1 = Close
        bus.publish(new Message(100, elevatorID, 1));
    }

    /**
     * Checks if the door path is obstructed.
     *
     * @return {@code true} if an obstruction is detected.
     */
    public boolean obstructed() {
        updateState(); // Poll for fresh data
        return isObstructed;
    }

    /**
     * Checks if the doors are fully closed and latched.
     *
     * @return {@code true} if fully closed.
     */
    public boolean fullyClosed() {
        updateState(); // Poll for fresh data
        return isFullyClosed;
    }

    /**
     * Checks if the doors are fully open.
     *
     * @return {@code true} if fully open.
     */
    public boolean fullyOpen() {
        updateState(); // Poll for fresh data
        return isFullyOpen;
    }

    /**
     * Checks if the cabin weight exceeds the limit.
     *
     * @return {@code true} if overloaded.
     */
    public boolean overCapacity() {
        updateState(); // Poll for fresh data
        return isOverloaded;
    }
}