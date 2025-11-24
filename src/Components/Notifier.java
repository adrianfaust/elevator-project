package Components;

import Bus.SoftwareBus;
import Message.Message;

/**
 * Notifier Component.
 * <p>
 * Responsible for handling all visual and auditory feedback mechanisms within the elevator system.
 * It translates logical events (arrival, status updates, errors) into specific hardware commands
 * sent via the {@link SoftwareBus}.
 * <p>
 * <b>Architecture Note:</b> This component is a "Publisher-Only" entity as per the SAD.
 * It does not subscribe to bus messages but serves as the output driver for the elevator's UI.
 */
public class Notifier {

    /**
     * The interface to the system's message bus.
     */
    private final SoftwareBus bus;

    /**
     * The unique identifier for this elevator instance.
     * Used as the subtopic for elevator-specific messages.
     */
    private final int elevatorID;

    /**
     * Constructs a new Notifier.
     *
     * @param bus        The SoftwareBus instance used for publishing hardware commands.
     * @param elevatorID The unique ID of the elevator (1-4).
     */
    public Notifier(SoftwareBus bus, int elevatorID) {
        this.bus = bus;
        this.elevatorID = elevatorID;
    }

    /**
     * Signals that the elevator has arrived at a specific floor.
     * <p>
     * This method triggers two actions:
     * <ol>
     * <li>Updates the visual floor/direction display.</li>
     * <li>Plays the arrival chime audio.</li>
     * </ol>
     *
     * @param floor     The floor number the elevator has arrived at (1-10).
     * @param direction The direction the elevator is currently servicing
     *                  (0 = Up, 1 = Down, 2 = None).
     */
    public void arrivedAtFloor(int floor, int direction) {
        // 1. Update visual indicators
        elevatorStatus(floor, direction);

        // 2. Play Audio: Arrival Chime
        // Topic 116 (Play Sound), Body 0 (Arrival Chime)
        Message msg = new Message(116, elevatorID, 0);
        bus.publish(msg);
    }

    /**
     * Updates the elevator's visual status displays.
     * <p>
     * This publishes two separate messages to the hardware:
     * <ul>
     * <li><b>Topic 111:</b> Updates the 7-segment floor display.</li>
     * <li><b>Topic 112:</b> Updates the direction arrow LEDs.</li>
     * </ul>
     *
     * @param floor     The current floor number (1-10).
     * @param direction The current direction (0 = Up, 1 = Down, 2 = None).
     */
    public void elevatorStatus(int floor, int direction) {
        // Publish Topic 111: Display Floor
        // Body: Floor number
        bus.publish(new Message(111, elevatorID, floor));

        // Publish Topic 112: Display Direction
        // Body: Direction Code (0=Up, 1=Down, 2=None)
        bus.publish(new Message(112, elevatorID, direction));
    }

    /**
     * Triggers the "Over Capacity" audio warning.
     * <p>
     * Used when the DoorAssembly detects a weight limit violation.
     * Sends Topic 116 with Body 1 (Overload Warning).
     */
    public void playCapacityNoise() {
        // Topic 116: Play Sound
        // Body: 1 = Overload Warning
        bus.publish(new Message(116, elevatorID, 1));
    }

    /**
     * Stops the "Over Capacity" audio warning.
     * <p>
     * <b>Note:</b> The current message protocol (CSV) does not define a specific code
     * for stopping audio. This method is retained to satisfy the SAD interface contract
     * but currently performs no operation to avoid undefined hardware behavior.
     */
    public void stopCapacityNoise() {
        // No-Op: Awaiting protocol update for "Stop Sound" command.
    }
}