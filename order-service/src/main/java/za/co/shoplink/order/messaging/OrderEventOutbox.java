package za.co.shoplink.order.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.JmsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Process-local pending publications; intentionally shares the lifetime of in-memory orders. */
@Component
public class OrderEventOutbox {
    private static final Logger LOG = LoggerFactory.getLogger(OrderEventOutbox.class);
    private final Map<String, Runnable> pending = new LinkedHashMap<>();

    public synchronized void enqueue(String key, Runnable publication) {
        pending.putIfAbsent(key, publication);
    }

    @Scheduled(fixedDelayString = "${shoplink.events.retry-delay-ms:2000}")
    public synchronized void retryPending() {
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            try {
                entry.getValue().run();
                iterator.remove();
            } catch (JmsException ex) {
                LOG.warn("Publication {} remains pending: {}", entry.getKey(), ex.getMessage());
            }
        }
    }

    public synchronized int pendingCount() { return pending.size(); }
}
