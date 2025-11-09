package ua.panic.pnctgreward.db;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {
    void init();
    void close();

    CompletableFuture<Boolean> link(long tgId, UUID uuid, String name);
    CompletableFuture<Optional<UUID>> getUuidByTg(long tgId);
    CompletableFuture<Optional<Long>> getTgByUuid(UUID uuid);

    CompletableFuture<Long> getLastClaimEpoch(long tgId, String campaignId);
    CompletableFuture<Void> recordClaim(long tgId, String campaignId, long epochSeconds);
    CompletableFuture<Boolean> hasAnyClaim(long tgId, String campaignId);

    CompletableFuture<Void> upsertPending(UUID uuid, String name, String campaignId);
    CompletableFuture<Optional<String>> popPending(UUID uuid);
}
