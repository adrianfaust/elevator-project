package Tests;

import Bus.SoftwareBus;
import Message.Message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A manual mock of the SoftwareBus for Unit Testing.
 * Implements Singleton pattern to avoid re-binding the server port multiple times.
 */
public class StubSoftwareBus extends SoftwareBus {

    private static StubSoftwareBus instance;

    // Stores messages sent BY the component (for us to verify)
    public final List<Message> publishedMessages = new ArrayList<>();

    // Stores messages waiting TO BE READ by the component (for us to simulate inputs)
    private final LinkedList<Message> incomingMessages = new LinkedList<>();

    /**
     * Private constructor prevents direct instantiation.
     */
    private StubSoftwareBus() {
        super(true); // Binds port 9999
    }

    /**
     * Returns the single instance of the StubBus.
     */
    public static synchronized StubSoftwareBus getInstance() {
        if (instance == null) {
            instance = new StubSoftwareBus();
        }
        return instance;
    }

    /**
     * Clears all messages. Must be called in @BeforeEach.
     */
    public void reset() {
        publishedMessages.clear();
        incomingMessages.clear();
    }

    @Override
    public void publish(Message message) {
        publishedMessages.add(message);
    }

    @Override
    public void subscribe(int topic, int subtopic) {
        // No-op
    }

    @Override
    public Message get(int topic, int subtopic) {
        Iterator<Message> it = incomingMessages.iterator();
        while (it.hasNext()) {
            Message m = it.next();
            if (m.getTopic() == topic && (subtopic == 0 || m.getSubTopic() == subtopic)) {
                it.remove();
                return m;
            }
        }
        return null;
    }

    public void addIncoming(Message msg) {
        incomingMessages.add(msg);
    }
}