package dev.lizarazu.profileapi.sam;

import java.util.List;

public record SamEntityResponse(
    int totalRecords,
    List<SamEntity> entityData
) {
}