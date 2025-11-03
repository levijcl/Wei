package com.wei.orchestrator.unit.wes.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import org.junit.jupiter.api.Test;

class TaskStatusTest {

    @Test
    void shouldReturnFalseForPendingIsTerminal() {
        assertFalse(TaskStatus.PENDING.isTerminal());
    }

    @Test
    void shouldReturnFalseForSubmittedIsTerminal() {
        assertFalse(TaskStatus.SUBMITTED.isTerminal());
    }

    @Test
    void shouldReturnFalseForInProgressIsTerminal() {
        assertFalse(TaskStatus.IN_PROGRESS.isTerminal());
    }

    @Test
    void shouldReturnTrueForCompletedIsTerminal() {
        assertTrue(TaskStatus.COMPLETED.isTerminal());
    }

    @Test
    void shouldReturnTrueForFailedIsTerminal() {
        assertTrue(TaskStatus.FAILED.isTerminal());
    }

    @Test
    void shouldReturnTrueForCanceledIsTerminal() {
        assertTrue(TaskStatus.CANCELED.isTerminal());
    }

    @Test
    void shouldReturnTrueForPendingCanSubmit() {
        assertTrue(TaskStatus.PENDING.canSubmit());
    }

    @Test
    void shouldReturnFalseForSubmittedCanSubmit() {
        assertFalse(TaskStatus.SUBMITTED.canSubmit());
    }

    @Test
    void shouldReturnFalseForInProgressCanSubmit() {
        assertFalse(TaskStatus.IN_PROGRESS.canSubmit());
    }

    @Test
    void shouldReturnFalseForCompletedCanSubmit() {
        assertFalse(TaskStatus.COMPLETED.canSubmit());
    }

    @Test
    void shouldReturnFalseForFailedCanSubmit() {
        assertFalse(TaskStatus.FAILED.canSubmit());
    }

    @Test
    void shouldReturnFalseForCanceledCanSubmit() {
        assertFalse(TaskStatus.CANCELED.canSubmit());
    }

    @Test
    void shouldReturnFalseForPendingCanUpdateFromWes() {
        assertFalse(TaskStatus.PENDING.canUpdateFromWes());
    }

    @Test
    void shouldReturnTrueForSubmittedCanUpdateFromWes() {
        assertTrue(TaskStatus.SUBMITTED.canUpdateFromWes());
    }

    @Test
    void shouldReturnTrueForInProgressCanUpdateFromWes() {
        assertTrue(TaskStatus.IN_PROGRESS.canUpdateFromWes());
    }

    @Test
    void shouldReturnFalseForCompletedCanUpdateFromWes() {
        assertFalse(TaskStatus.COMPLETED.canUpdateFromWes());
    }

    @Test
    void shouldReturnFalseForFailedCanUpdateFromWes() {
        assertFalse(TaskStatus.FAILED.canUpdateFromWes());
    }

    @Test
    void shouldReturnFalseForCanceledCanUpdateFromWes() {
        assertFalse(TaskStatus.CANCELED.canUpdateFromWes());
    }

    @Test
    void shouldReturnTrueForPendingCanCancel() {
        assertTrue(TaskStatus.PENDING.canCancel());
    }

    @Test
    void shouldReturnTrueForSubmittedCanCancel() {
        assertTrue(TaskStatus.SUBMITTED.canCancel());
    }

    @Test
    void shouldReturnTrueForInProgressCanCancel() {
        assertTrue(TaskStatus.IN_PROGRESS.canCancel());
    }

    @Test
    void shouldReturnFalseForCompletedCanCancel() {
        assertFalse(TaskStatus.COMPLETED.canCancel());
    }

    @Test
    void shouldReturnFalseForFailedCanCancel() {
        assertFalse(TaskStatus.FAILED.canCancel());
    }

    @Test
    void shouldReturnFalseForCanceledCanCancel() {
        assertFalse(TaskStatus.CANCELED.canCancel());
    }
}
