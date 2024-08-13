package org.codenil.comm.connections;

import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class Subscribers<T> {
    private static final Subscribers<?> NONE = new EmptySubscribers<>();

    private final AtomicLong subscriberId = new AtomicLong();
    private final Map<Long, T> subscribers = new ConcurrentHashMap<>();

    private final boolean suppressCallbackExceptions;

    private Subscribers(final boolean suppressCallbackExceptions) {
        this.suppressCallbackExceptions = suppressCallbackExceptions;
    }

    @SuppressWarnings("unchecked")
    public static <T> Subscribers<T> none() {
        return (Subscribers<T>) NONE;
    }

    public static <T> Subscribers<T> create() {
        return new Subscribers<T>(false);
    }


    public static <T> Subscribers<T> create(final boolean catchCallbackExceptions) {
        return new Subscribers<T>(catchCallbackExceptions);
    }

    public long subscribe(final T subscriber) {
        final long id = subscriberId.getAndIncrement();
        subscribers.put(id, subscriber);
        return id;
    }

    public boolean unsubscribe(final long subscriberId) {
        return subscribers.remove(subscriberId) != null;
    }

    public void forEach(final Consumer<T> action) {
        ImmutableSet.copyOf(subscribers.values())
                .forEach(subscriber -> {
                    try {
                        action.accept(subscriber);
                    } catch (final Exception e) {
                        if (suppressCallbackExceptions) {
//                                    LOG.debug("Error in callback: {}", e);
                        } else {
                            throw e;
                        }
                    }
                });
    }

    public int getSubscriberCount() {
        return subscribers.size();
    }

    private static class EmptySubscribers<T> extends Subscribers<T> {

        private EmptySubscribers() {
            super(false);
        }

        @Override
        public long subscribe(final T subscriber) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unsubscribe(final long subscriberId) {
            return false;
        }

        @Override
        public void forEach(final Consumer<T> action) {}

        @Override
        public int getSubscriberCount() {
            return 0;
        }
    }
}
