package Tests;

import ElevatorController.DoorAssembly;
import Message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoorAssemblyTest {

    private final int ELEVATOR_ID = 1;
    private StubSoftwareBus stubBus;
    private DoorAssembly doorAssembly;

    @BeforeEach
    void setUp() {
        stubBus = StubSoftwareBus.getInstance();
        stubBus.reset();
        doorAssembly = new DoorAssembly(stubBus, ELEVATOR_ID);
    }

    @Test
    void testOpenCommand() {
        doorAssembly.open();
        assertEquals(1, stubBus.publishedMessages.size());
        assertEquals(100, stubBus.publishedMessages.get(0).getTopic());
        assertEquals(0, stubBus.publishedMessages.get(0).getBody());
    }

    @Test
    void testObstructedStateUpdate() {
        assertFalse(doorAssembly.obstructed());
        stubBus.addIncoming(new Message(203, ELEVATOR_ID, 0));
        assertTrue(doorAssembly.obstructed());
    }

    @Test
    void testFullyOpenStateUpdate() {
        stubBus.addIncoming(new Message(204, ELEVATOR_ID, 0));
        assertTrue(doorAssembly.fullyOpen());
    }
}