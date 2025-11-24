package Tests;

import Components.Notifier;
import Message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotifierTest {

    private final int ELEVATOR_ID = 1;
    private StubSoftwareBus stubBus;
    private Notifier notifier;

    @BeforeEach
    void setUp() {
        // Use Singleton instance and reset it
        stubBus = StubSoftwareBus.getInstance();
        stubBus.reset();

        notifier = new Notifier(stubBus, ELEVATOR_ID);
    }

    @Test
    void testArrivedAtFloor() {
        notifier.arrivedAtFloor(5, 0);

        assertEquals(3, stubBus.publishedMessages.size());

        Message floorMsg = stubBus.publishedMessages.get(0);
        assertEquals(111, floorMsg.getTopic());
        assertEquals(5, floorMsg.getBody());

        Message dirMsg = stubBus.publishedMessages.get(1);
        assertEquals(112, dirMsg.getTopic());
        assertEquals(0, dirMsg.getBody());

        Message soundMsg = stubBus.publishedMessages.get(2);
        assertEquals(116, soundMsg.getTopic());
        assertEquals(0, soundMsg.getBody());
    }

    @Test
    void testElevatorStatus() {
        notifier.elevatorStatus(3, 1);

        assertEquals(2, stubBus.publishedMessages.size());
        assertEquals(111, stubBus.publishedMessages.get(0).getTopic());
        assertEquals(112, stubBus.publishedMessages.get(1).getTopic());
    }

    @Test
    void testPlayCapacityNoise() {
        notifier.playCapacityNoise();
        assertEquals(1, stubBus.publishedMessages.size());
        assertEquals(116, stubBus.publishedMessages.get(0).getTopic());
        assertEquals(1, stubBus.publishedMessages.get(0).getBody());
    }
}