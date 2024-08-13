package org.codenil.comm;

import org.codenil.comm.message.DisconnectReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;

public class PeerReputation implements Comparable<PeerReputation> {
    private static final long USELESS_RESPONSE_WINDOW_IN_MILLIS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
    private static final int DEFAULT_MAX_SCORE = 150;
    private static final int DEFAULT_INITIAL_SCORE = 100;
    private static final Logger LOG = LoggerFactory.getLogger(PeerReputation.class);
    private static final int TIMEOUT_THRESHOLD = 5;
    private static final int USELESS_RESPONSE_THRESHOLD = 5;
    private static final int SMALL_ADJUSTMENT = 1;
    private static final int LARGE_ADJUSTMENT = 10;

    private final ConcurrentMap<Integer, AtomicInteger> timeoutCountByRequestType = new ConcurrentHashMap<>();
    private final Queue<Long> uselessResponseTimes = new ConcurrentLinkedQueue<>();

    private int score;

    private final int maxScore;

    public PeerReputation() {
        this(DEFAULT_INITIAL_SCORE, DEFAULT_MAX_SCORE);
    }

    public PeerReputation(final int initialScore, final int maxScore) {
        checkArgument(
                initialScore <= maxScore, "Initial score must be less than or equal to max score");
        this.maxScore = maxScore;
        this.score = initialScore;
    }

    public Optional<DisconnectReason> recordRequestTimeout(final int requestCode) {
        final int newTimeoutCount = getOrCreateTimeoutCount(requestCode).incrementAndGet();
        if (newTimeoutCount >= TIMEOUT_THRESHOLD) {
            LOG.debug(
                    "Disconnection triggered by {} repeated timeouts for requestCode {}",
                    newTimeoutCount,
                    requestCode);
            score -= LARGE_ADJUSTMENT;
            return Optional.of(DisconnectReason.TIMEOUT);
        } else {
            score -= SMALL_ADJUSTMENT;
            return Optional.empty();
        }
    }

    public void resetTimeoutCount(final int requestCode) {
        timeoutCountByRequestType.remove(requestCode);
    }

    private AtomicInteger getOrCreateTimeoutCount(final int requestCode) {
        return timeoutCountByRequestType.computeIfAbsent(requestCode, code -> new AtomicInteger());
    }

    public Map<Integer, AtomicInteger> timeoutCounts() {
        return timeoutCountByRequestType;
    }

    public Optional<DisconnectReason> recordUselessResponse(final long timestamp) {
        uselessResponseTimes.add(timestamp);
        while (shouldRemove(uselessResponseTimes.peek(), timestamp)) {
            uselessResponseTimes.poll();
        }
        if (uselessResponseTimes.size() >= USELESS_RESPONSE_THRESHOLD) {
            score -= LARGE_ADJUSTMENT;
            LOG.debug("Disconnection triggered by exceeding useless response threshold");
            return Optional.of(DisconnectReason.UNKNOWN);
        } else {
            score -= SMALL_ADJUSTMENT;
            return Optional.empty();
        }
    }

    public void recordUsefulResponse() {
        if (score < maxScore) {
            score = Math.min(maxScore, score + SMALL_ADJUSTMENT);
        }
    }

    private boolean shouldRemove(final Long timestamp, final long currentTimestamp) {
        return timestamp != null && timestamp + USELESS_RESPONSE_WINDOW_IN_MILLIS < currentTimestamp;
    }

    @Override
    public String toString() {
        return String.format(
                "PeerReputation score: %d, timeouts: %s, useless: %s",
                score, timeoutCounts(), uselessResponseTimes.size());
    }

    @Override
    public int compareTo(final @Nonnull PeerReputation otherReputation) {
        return Integer.compare(this.score, otherReputation.score);
    }

    public int getScore() {
        return score;
    }
}