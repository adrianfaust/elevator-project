package Tests;

import ElevatorController.Cabin;
import Message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CabinTest {

    private final int ELEVATOR_ID = 1;
    private StubSoftwareBus stubBus;
    private Cabin cabin;

    @BeforeEach
    void setUp() {
        stubBus = StubSoftwareBus.getInstance();
        stubBus.reset();
        cabin = new Cabin(stubBus, ELEVATOR_ID);
    }

    @AfterEach
    void tearDown() {
        // CRITICAL: Stop the cabin thread so it doesn't pollute subsequent tests
        if (cabin != null) {
            cabin.stop();
        }
    }

    @Test
    void testInitialStatePublishing() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
        }

        boolean hasPosition = false;
        for (Message m : stubBus.publishedMessages) {
            if (m.getTopic() == 202 && m.getBody() == 1) {
                hasPosition = true;
                break;
            }
        }
        assertTrue(hasPosition, "Cabin should broadcast initial floor 1");
    }

    @Test
    void testGoToFloorUpdatesTarget() {
        cabin.goToFloor(5);
        assertEquals(5, cabin.getTargetFloor());
        assertFalse(cabin.arrived());
    }

    @Test
    void testCurrentStatus() {
        int[] status = cabin.currentStatus();
        assertEquals(1, status[0]); // Floor
        assertEquals(2, status[1]); // Direction
    }
}