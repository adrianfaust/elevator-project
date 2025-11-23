package Components;

import Bus.SoftwareBus;
import Message.Message;

/**
 * Notifier Component (from SAD)
 * Responsible for communicating visual and audio information to the hardware
 * via the SoftwareBus.
 */
public class Notifier {

    private final SoftwareBus bus;
    private final int elevatorID;

    /**
     * @param bus        Instance of the SoftwareBus to publish messages to.
     * @param elevatorID The ID of the elevator this notifier belongs to (used as subtopic).
     */
    public Notifier(SoftwareBus bus, int elevatorID) {
        this.bus = bus;
        this.elevatorID = elevatorID;
    }

    /**
     * Updates the displays and plays the arrival chime.
     * Maps to CSV Topics 111, 112, and 116.
     *
     * @param floor     The floor the elevator has arrived at.
     * @param direction The direction the elevator is heading (0=Up, 1=Down, 2=None).
     */
    public void arrivedAtFloor(int floor, int direction) {
        // Update visuals first
        elevatorStatus(floor, direction);

        // Topic 116: Play sound
        // Body: 0 = Arrival Chime
        Message msg = new Message(116, elevatorID, 0);
        bus.publish(msg);
    }

    /**
     * Updates the floor and direction display.
     * Maps to CSV Topics 111 and 112.
     *
     * @param floor     The current floor.
     * @param direction The current direction (0=Up, 1=Down, 2=None).
     */
    public void elevatorStatus(int floor, int direction) {
        // Topic 111: Display floor
        // Body: floor number
        bus.publish(new Message(111, elevatorID, floor));

        // Topic 112: Display direction
        // Body: direction code (0=Up, 1=Down, 2=None)
        bus.publish(new Message(112, elevatorID, direction));
    }

    /**
     * Plays the capacity/overload warning noise.
     * Maps to CSV Topic 116.
     */
    public void playCapacityNoise() {
        // Topic 116: Play sound
        // Body: 1 = Overload Warning
        bus.publish(new Message(116, elevatorID, 1));
    }

    /**
     * Stops the capacity/overload warning noise.
     * Note: The CSV (Topic 116) currently only specifies codes for triggering sounds (0/1).
     * This method is included to satisfy the SAD structure.
     */
    public void stopCapacityNoise() {
        // Placeholder: If a specific "Stop Sound" code is added to the protocol later,
        // it would be implemented here.
    }
}