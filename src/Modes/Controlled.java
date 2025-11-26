package Modes;

import Bus.SoftwareBus;
import ElevatorController.Buttons;
import ElevatorController.Cabin;
import ElevatorController.DoorAssembly;
import ElevatorController.Notifier;
import Message.Message;

/**
 * Controlled mode behavior.
 *
 * In this mode the elevator motion is driven by commands coming from the
 * control room. The control room / command panel talks using the same
 * message codes that the Elevator Multiplexer already understands.
 *
 * This class generally only manages the use of two topics:
 *   - Topic 102: Car dispatch (body = 0 up, 1 down)
 *   - Topic 103: Car stop    (body = 0, stop car)
 *   Helper methods in other device classes manage other message sending needed
 *   for operation.
 */
public class Controlled {
    
    // Message topics used:
    private static final int TOPIC_CAR_DISPATCH = 102; // body: 0 = up, 1 = down
    private static final int TOPIC_CAR_STOP = 103; // body: 0 = stop car
    
    // Direction encoding (Topic 102 Message body)
    private static final int DIR_UP = 0;
    private static final int DIR_DOWN = 1;
    private static final int DIR_NONE = 2; // used for Notifier / direction display
    
    // Floors range
    private static final int MIN_FLOOR = 1;
    private static final int MAX_FLOOR = 10;
    
    private final SoftwareBus bus;
    private final int elevatorID;
    
    // References to the rest of the Elevator Controller
    private final Buttons buttons;
    private final Cabin cabin;
    private final DoorAssembly doors;
    private final Notifier notifier;
    
    /**
     * Create a Controlled-mode controller for one elevator.
     *
     * @param bus        Shared SoftwareBus
     * @param elevatorID Elevator ID (1–4, used as subtopic)
     * @param buttons    Buttons component for this elevator
     * @param cabin      Cabin component for this elevator
     * @param doors      DoorAssembly component for this elevator
     * @param notifier   Notifier component for this elevator
     */
    public Controlled(SoftwareBus bus,
                      int elevatorID,
                      Buttons buttons,
                      Cabin cabin,
                      DoorAssembly doors,
                      Notifier notifier) {
        
        this.bus = bus;
        this.elevatorID = elevatorID;
        this.buttons = buttons;
        this.cabin = cabin;
        this.doors = doors;
        this.notifier = notifier;
        
        if (buttons != null) {
            buttons.disableCalls();
            buttons.enableSingleRequest();
        }
    }
    
    /**
     * Main entry point while Mode == CONTROLLED.
     * <p>
     * Call this once per main loop iteration. It consumes dispatcher
     * commands (start/stop in a direction) that use the same topics
     * the Elevator Multiplexer already uses.
     */
    public void step() {
        
        // Highest priority: an explicit STOP from the control room.
        Message stopCmd = bus.get(TOPIC_CAR_STOP, elevatorID);
        if (stopCmd != null) {
            handleStopCommand();
            return;
        }
        
        // Otherwise, see if the control room has dispatched the car
        // in a given direction.
        Message dispatchCmd = bus.get(TOPIC_CAR_DISPATCH, elevatorID);
        if (dispatchCmd != null) {
            handleDispatchCommand(dispatchCmd);
        }
    }
    
    /**
     * Handle Topic 103 "Car stop" command.
     */
    private void handleStopCommand() {
        
        // 1. Tell the multiplexer to stop the car (Topic 103, body=0).
        bus.publish(new Message(TOPIC_CAR_STOP, elevatorID, 0));
        
        // 2. Ask Cabin to halt any current trip
        cabin.stop();
        
        // 3. Open the doors where the car currently is, using the existing
        //    DoorAssembly logic
        doors.open();
        
        // 4. Notify displays / sounds via Notifier using existing topics
        int[] status = cabin.currentStatus(); // [floor, direction]
        int currentFloor = status[0];
        notifier.elevatorStatus(currentFloor, DIR_NONE);
    }
    
    /**
     * Handle Topic 102 "Car dispatch" command.
     * Body = 0 means "go up", body = 1 means "go down".
     */
    private void handleDispatchCommand(Message dispatchCmd) {
        int direction = dispatchCmd.getBody();
        
        int targetFloor;
        switch (direction) {
            case DIR_UP:
                targetFloor = MAX_FLOOR;
                break;
            case DIR_DOWN:
                targetFloor = MIN_FLOOR;
                break;
            default:
                // Unknown direction – ignore.
                return;
        }
        
        // Close doors before motion
        doors.close();
        
        // Ask Cabin to perform the trip. Cabin controls motion
        cabin.goToFloor(targetFloor);
        
        // Update indicators using Notifier, which already publishes:
        notifier.elevatorStatus(targetFloor, direction);
    }
    
}