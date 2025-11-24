package Tests;

import ElevatorController.Buttons;
import Message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ButtonsTest {

    private final int ELEVATOR_ID = 1;
    private StubSoftwareBus stubBus;
    private Buttons buttons;

    @BeforeEach
    void setUp() {
        stubBus = StubSoftwareBus.getInstance();
        stubBus.reset();
        buttons = new Buttons(stubBus, ELEVATOR_ID);
    }

    @Test
    void testNextService_Idle_RequestUp() {
        stubBus.addIncoming(new Message(200, 3, 0));
        int[] result = buttons.nextService(1, 2);
        assertArrayEquals(new int[]{3, 0}, result);
    }

    @Test
    void testNextService_MovingUp_StopOnWay() {
        stubBus.addIncoming(new Message(200, 5, 0));
        int[] result = buttons.nextService(2, 0);
        assertArrayEquals(new int[]{5, 0}, result);
    }

    @Test
    void testCallReset() {
        buttons.callReset(4, 0);
        assertEquals(1, stubBus.publishedMessages.size());
        Message msg = stubBus.publishedMessages.get(0);
        assertEquals(110, msg.getTopic());
        assertEquals(4, msg.getSubTopic());
    }

    @Test
    void testEnableCalls() {
        buttons.enableCalls();
        assertEquals(1, stubBus.publishedMessages.size());
        assertEquals(113, stubBus.publishedMessages.get(0).getTopic());
    }
}