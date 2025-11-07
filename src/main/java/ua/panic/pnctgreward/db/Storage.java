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
    CompletableFuture<Boolean> markClaim(long tgId, String campaignId);
    CompletableFuture<Boolean> hasClaim(long tgId, String campaignId);
}