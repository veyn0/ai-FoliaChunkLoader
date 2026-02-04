package dev.veyno.aiFoliaChunkLoader;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class KeepRegion {
    private final UUID id;
    private final String worldName;
    private final UUID worldUuid;
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final String createdBy;
    private final Instant createdAt;

    public KeepRegion(UUID id, String worldName, UUID worldUuid, int centerX, int centerZ, int radius, String createdBy, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.worldUuid = worldUuid;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getWorldName() {
        return worldName;
    }

    public UUID getWorldUuid() {
        return worldUuid;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public int getRadius() {
        return radius;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean sameKey(KeepRegion other) {
        if (other == null) {
            return false;
        }
        if (worldUuid != null && other.worldUuid != null) {
            if (!worldUuid.equals(other.worldUuid)) {
                return false;
            }
        } else if (!worldName.equalsIgnoreCase(other.worldName)) {
            return false;
        }
        return centerX == other.centerX && centerZ == other.centerZ && radius == other.radius;
    }

    public String shortId() {
        String value = id.toString();
        return value.substring(0, Math.min(8, value.length()));
    }
}
