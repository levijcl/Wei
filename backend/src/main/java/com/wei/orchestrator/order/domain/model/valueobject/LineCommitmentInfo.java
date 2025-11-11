package com.wei.orchestrator.order.domain.model.valueobject;

import java.time.LocalDateTime;
import java.util.Objects;

public final class LineCommitmentInfo {
    private final CommitmentStatus status;
    private final String wesTransactionId;
    private final String failureReason;
    private final LocalDateTime committedAt;

    private LineCommitmentInfo(
            CommitmentStatus status,
            String wesTransactionId,
            String failureReason,
            LocalDateTime committedAt) {
        this.status = status;
        this.wesTransactionId = wesTransactionId;
        this.failureReason = failureReason;
        this.committedAt = committedAt;
    }

    public static LineCommitmentInfo pending() {
        return new LineCommitmentInfo(CommitmentStatus.PENDING, null, null, null);
    }

    public static LineCommitmentInfo inProgress(String pickingTaskId) {
        if (pickingTaskId == null || pickingTaskId.isBlank()) {
            throw new IllegalArgumentException("Picking task ID cannot be null or blank");
        }
        return new LineCommitmentInfo(
                CommitmentStatus.IN_PROGRESS, pickingTaskId, null, LocalDateTime.now());
    }

    public static LineCommitmentInfo committed(String wesTransactionId) {
        if (wesTransactionId == null || wesTransactionId.isBlank()) {
            throw new IllegalArgumentException("WES transaction ID cannot be null or blank");
        }
        return new LineCommitmentInfo(
                CommitmentStatus.COMMITTED, wesTransactionId, null, LocalDateTime.now());
    }

    public static LineCommitmentInfo failed(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Failure reason cannot be null or blank");
        }
        return new LineCommitmentInfo(CommitmentStatus.FAILED, null, reason, LocalDateTime.now());
    }

    public boolean isCommitted() {
        return status == CommitmentStatus.COMMITTED;
    }

    public boolean isFailed() {
        return status == CommitmentStatus.FAILED;
    }

    public boolean isPending() {
        return status == CommitmentStatus.PENDING;
    }

    public CommitmentStatus getStatus() {
        return status;
    }

    public String getWesTransactionId() {
        return wesTransactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCommittedAt() {
        return committedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineCommitmentInfo that = (LineCommitmentInfo) o;
        return status == that.status
                && Objects.equals(wesTransactionId, that.wesTransactionId)
                && Objects.equals(failureReason, that.failureReason)
                && Objects.equals(committedAt, that.committedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, wesTransactionId, failureReason, committedAt);
    }

    @Override
    public String toString() {
        return String.format(
                "LineCommitmentInfo{status=%s, wesTransactionId='%s', failureReason='%s',"
                        + " committedAt=%s}",
                status, wesTransactionId, failureReason, committedAt);
    }
}
