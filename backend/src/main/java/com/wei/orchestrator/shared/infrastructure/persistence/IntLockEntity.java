package com.wei.orchestrator.shared.infrastructure.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "INT_LOCK")
@IdClass(IntLockEntity.IntLockId.class)
public class IntLockEntity {

    @Id
    @Column(name = "LOCK_KEY", length = 36, nullable = false)
    private String lockKey;

    @Id
    @Column(name = "REGION", length = 100, nullable = false)
    private String region;

    @Column(name = "CLIENT_ID", length = 36)
    private String clientId;

    @Column(name = "CREATED_DATE", nullable = false)
    private LocalDateTime createdDate;

    public static class IntLockId implements Serializable {
        private String lockKey;
        private String region;

        public IntLockId() {}

        public IntLockId(String lockKey, String region) {
            this.lockKey = lockKey;
            this.region = region;
        }

        public String getLockKey() {
            return lockKey;
        }

        public void setLockKey(String lockKey) {
            this.lockKey = lockKey;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntLockId that = (IntLockId) o;
            return lockKey.equals(that.lockKey) && region.equals(that.region);
        }

        @Override
        public int hashCode() {
            return lockKey.hashCode() + region.hashCode();
        }
    }

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
