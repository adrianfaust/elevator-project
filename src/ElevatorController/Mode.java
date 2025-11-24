package ElevatorController;

import Bus.SoftwareBus;
import Message.Message;

/**
 * Mode Component (from SAD)
 * Handles mode changes such as Normal, Fire-Safety, and Controlled.
 * * Note: Uses internal polling (updateState) to ensure the returned mode
 * is always up to date with the latest bus messages.
 */
public class Mode {

    // Modes defined in SAD
    public static final int NORMAL = 1;
    public static final int FIRE_SAFETY = 2;
    public static final int CONTROLLED = 3;

    private final SoftwareBus bus;
    private final int elevatorID;
    private int currentMode;

    /**
     * @param bus        Instance of the SoftwareBus.
     * @param elevatorID The ID of the elevator this mode tracker belongs to.
     */
    public Mode(SoftwareBus bus, int elevatorID) {
        this.bus = bus;
        this.elevatorID = elevatorID;
        this.currentMode = NORMAL; // Default start state

        // Subscribe to relevant topics
        bus.subscribe(5, elevatorID); // Mode changes
        bus.subscribe(120, 0);        // Fire Alarm
        bus.subscribe(4, 0);          // Clear Fire
    }

    /**
     * Internal helper to check the bus for mode-changing messages.
     * Called automatically by getMode().
     */
    private void updateState() {
        Message msg;

        // 1. Check for Fire Alarm (Topic 120) - Highest Priority
        msg = bus.get(120, 0);
        if (msg != null) {
            if (msg.getBody() == 0) {
                currentMode = FIRE_SAFETY;
                return; // Fire overrides other mode messages
            }
        }

        // 2. Check for Clear Fire (Topic 4)
        msg = bus.get(4, 0);
        if (msg != null) {
            currentMode = NORMAL;
        }

        // 3. Check for System Mode Commands (Topic 5)
        // Sent by Command Panel
        msg = bus.get(5, elevatorID);
        if (msg != null && currentMode != FIRE_SAFETY) {
            int body = msg.getBody();
            // Mapping based on CSV/Logic:
            // 1 = Centralized (Controlled)
            // 2 = Independent (Normal)

            if (body == 1) {
                currentMode = CONTROLLED;
            } else if (body == 2) {
                currentMode = NORMAL;
            }
        }
    }

    /**
     * Returns the current operation mode.
     * Automatically updates internal state from the bus before returning.
     *
     * @return 1=NORMAL, 2=FIRE_SAFETY, 3=CONTROLLED
     */
    public int getMode() {
        updateState(); // Ensure state is up to date
        return currentMode;
    }
}