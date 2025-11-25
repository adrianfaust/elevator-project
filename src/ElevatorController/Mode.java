package ElevatorController;

import Bus.SoftwareBus;
import Message.Message;

/**
 * Mode Component.
 * <p>
 * Manages the operational state of the elevator (Normal, Fire Safety, Controlled).
 * This class acts as a passive state container that subscribes to high-priority system
 * messages (like Fire Alarms) and Supervisor commands to determine the current
 * operating mode.
 * <p>
 * <b>Logic:</b>
 * The state is determined by a strict priority hierarchy:
 * <ol>
 * <li><b>Fire Safety (Highest):</b> Triggered by global fire alarm (Topic 120).</li>
 * <li><b>Controlled:</b> Triggered by supervisor command (Topic 5).</li>
 * <li><b>Normal (Default):</b> Standard operation.</li>
 * </ol>
 */
public class Mode {

    // --- Mode Constants (Defined in SAD) ---
    public static final int NORMAL = 1;
    public static final int FIRE_SAFETY = 2;
    public static final int CONTROLLED = 3;

    private final SoftwareBus bus;
    private final int elevatorID;

    /**
     * The current operating mode.
     * Defaults to {@link #NORMAL} on initialization.
     */
    private int currentMode;

    /**
     * Constructs a new Mode component.
     *
     * @param bus        The SoftwareBus for receiving state-change messages.
     * @param elevatorID The ID of the elevator (used to filter specific command messages).
     */
    public Mode(SoftwareBus bus, int elevatorID) {
        this.bus = bus;
        this.elevatorID = elevatorID;
        this.currentMode = NORMAL; // Default start state

        // --- Subscriptions ---
        // Topic 5: Mode changes initiated by the Command Panel (Supervisor)
        bus.subscribe(5, elevatorID);

        // Topic 120: Global Fire Alarm (Subtopic 0 = Broadcast)
        bus.subscribe(120, 0);

        // Topic 4: Clear Fire Signal (Subtopic 0 = Broadcast)
        bus.subscribe(4, 0);
    }

    /**
     * internal helper method to process pending bus messages and update state.
     * <p>
     * This method enforces the priority logic. It checks the message queue
     * in a specific order to ensure safety signals (Fire) override standard
     * supervisor commands.
     * <p>
     * <b>Note:</b> This is called automatically by {@link #getMode()} to ensure
     * data freshness (Polling pattern).
     */
    private void updateState() {
        Message msg;

        // 1. PRIORITY CHECK: Fire Alarm (Topic 120)
        // If a fire alarm "On" signal (0) is found, it overrides everything.
        msg = bus.get(120, 0);
        if (msg != null) {
            if (msg.getBody() == 0) {
                currentMode = FIRE_SAFETY;
                return; // Immediate return to enforce safety override
            }
        }

        // 2. CHECK: Clear Fire (Topic 4)
        // Resets the system to Normal mode.
        msg = bus.get(4, 0);
        if (msg != null) {
            currentMode = NORMAL;
        }

        // 3. CHECK: Supervisor Mode Commands (Topic 5)
        // Only processed if we are NOT currently in Fire Safety mode.
        msg = bus.get(5, elevatorID);
        if (msg != null && currentMode != FIRE_SAFETY) {
            int body = msg.getBody();
            // Mapping Logic (CSV vs SAD):
            // CSV Body 1 = Centralized -> Maps to SAD "CONTROLLED"
            // CSV Body 2 = Independent -> Maps to SAD "NORMAL"

            if (body == 1) {
                currentMode = CONTROLLED;
            } else if (body == 2) {
                currentMode = NORMAL;
            }
        }
    }

    /**
     * Retrieves the current operational mode.
     * <p>
     * This method triggers an internal bus poll to ensure the returned value
     * reflects the most recent messages from the hardware/supervisor.
     *
     * @return The mode integer (1=Normal, 2=Fire Safety, 3=Controlled).
     */
    public int getMode() {
        updateState(); // Ensure state is up to date
        return currentMode;
    }
}