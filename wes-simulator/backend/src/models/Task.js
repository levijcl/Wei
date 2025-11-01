const db = require('../config/database');
const { v4: uuidv4 } = require('uuid');

class Task {
  /**
   * Create a new WES task (picking or putaway)
   * Tasks automatically progress: PENDING -> IN_PROGRESS (after 10s) -> COMPLETED (after 1 min total)
   */
  static async create(taskData) {
    let connection;
    try {
      connection = await db.getPool().getConnection();

      const taskId = uuidv4();
      const sql = `
        INSERT INTO wes_tasks (
          task_id,
          task_type,
          order_id,
          warehouse_id,
          priority,
          status,
          created_at,
          updated_at,
          estimated_completion_at
        ) VALUES (
          :taskId,
          :taskType,
          :orderId,
          :warehouseId,
          :priority,
          :status,
          SYSTIMESTAMP,
          SYSTIMESTAMP,
          SYSTIMESTAMP + INTERVAL '1' MINUTE
        )
      `;

      const binds = {
        taskId,
        taskType: taskData.task_type || 'PICKING',
        orderId: taskData.order_id,
        warehouseId: taskData.warehouse_id || 'WH001',
        priority: taskData.priority || 5,
        status: 'PENDING'
      };

      await connection.execute(sql, binds, { autoCommit: false });

      // Insert task items
      for (const item of taskData.items) {
        const itemSql = `
          INSERT INTO wes_task_items (
            task_item_id,
            task_id,
            sku,
            product_name,
            quantity,
            location,
            created_at
          ) VALUES (
            :taskItemId,
            :taskId,
            :sku,
            :productName,
            :quantity,
            :location,
            SYSTIMESTAMP
          )
        `;

        const itemBinds = {
          taskItemId: uuidv4(),
          taskId,
          sku: item.sku,
          productName: item.product_name || item.sku,
          quantity: item.quantity,
          location: item.location || null
        };

        await connection.execute(itemSql, itemBinds, { autoCommit: false });
      }

      await connection.commit();

      return taskId;
    } catch (err) {
      if (connection) {
        try {
          await connection.rollback();
        } catch (rollbackErr) {
          console.error('Error rolling back transaction:', rollbackErr);
        }
      }
      throw err;
    } finally {
      if (connection) {
        try {
          await connection.close();
        } catch (closeErr) {
          console.error('Error closing connection:', closeErr);
        }
      }
    }
  }

  static async findAll(filters = {}) {
    let sql = `
      SELECT
        task_id,
        task_type,
        order_id,
        warehouse_id,
        priority,
        status,
        created_at,
        updated_at,
        started_at,
        estimated_completion_at,
        completed_at
      FROM wes_tasks
      WHERE 1=1
    `;

    const binds = {};

    if (filters.status) {
      sql += ' AND status = :status';
      binds.status = filters.status;
    }

    if (filters.task_type) {
      sql += ' AND task_type = :taskType';
      binds.taskType = filters.task_type;
    }

    if (filters.warehouse_id) {
      sql += ' AND warehouse_id = :warehouseId';
      binds.warehouseId = filters.warehouse_id;
    }

    sql += ' ORDER BY priority DESC, created_at ASC';

    const result = await db.execute(sql, binds, {
      outFormat: db.oracledb.OUT_FORMAT_OBJECT
    });

    return result.rows;
  }

  static async findById(taskId) {
    const taskSql = `
      SELECT
        task_id,
        task_type,
        order_id,
        warehouse_id,
        priority,
        status,
        created_at,
        updated_at,
        started_at,
        estimated_completion_at,
        completed_at
      FROM wes_tasks
      WHERE task_id = :taskId
    `;

    const taskResult = await db.execute(taskSql, { taskId }, {
      outFormat: db.oracledb.OUT_FORMAT_OBJECT
    });

    if (taskResult.rows.length === 0) {
      return null;
    }

    const task = taskResult.rows[0];

    const itemsSql = `
      SELECT
        task_item_id,
        sku,
        product_name,
        quantity,
        location,
        created_at
      FROM wes_task_items
      WHERE task_id = :taskId
      ORDER BY created_at
    `;

    const itemsResult = await db.execute(itemsSql, { taskId }, {
      outFormat: db.oracledb.OUT_FORMAT_OBJECT
    });

    task.items = itemsResult.rows;

    return task;
  }

  static async updateStatus(taskId, newStatus) {
    const sql = `
      UPDATE wes_tasks
      SET status = :status,
          updated_at = SYSTIMESTAMP,
          completed_at = CASE WHEN :status = 'COMPLETED' THEN SYSTIMESTAMP ELSE completed_at END
      WHERE task_id = :taskId
    `;

    const result = await db.execute(sql, { status: newStatus, taskId }, {
      autoCommit: true
    });

    return result.rowsAffected > 0;
  }

  static async updatePriority(taskId, newPriority) {
    const sql = `
      UPDATE wes_tasks
      SET priority = :priority,
          updated_at = SYSTIMESTAMP
      WHERE task_id = :taskId
    `;

    const result = await db.execute(sql, { priority: newPriority, taskId }, {
      autoCommit: true
    });

    return result.rowsAffected > 0;
  }

  static async cancelTask(taskId) {
    const sql = `
      UPDATE wes_tasks
      SET status = 'CANCELLED',
          updated_at = SYSTIMESTAMP
      WHERE task_id = :taskId
        AND status NOT IN ('COMPLETED', 'CANCELLED')
    `;

    const result = await db.execute(sql, { taskId }, {
      autoCommit: true
    });

    return result.rowsAffected > 0;
  }

  /**
   * Auto-progress tasks based on elapsed time
   * Only ONE task can be IN_PROGRESS at a time
   * PENDING -> IN_PROGRESS (selected by priority DESC, created_at ASC)
   * IN_PROGRESS -> COMPLETED (after 1 minute total from creation)
   */
  static async autoProgressTasks() {
    let connection;
    try {
      connection = await db.getPool().getConnection();

      // Step 1: Check if there's already a task IN_PROGRESS
      const checkInProgressSql = `
        SELECT COUNT(*) as count
        FROM wes_tasks
        WHERE status = 'IN_PROGRESS'
      `;

      const checkResult = await connection.execute(
        checkInProgressSql,
        {},
        { outFormat: db.oracledb.OUT_FORMAT_OBJECT }
      );

      const hasInProgress = checkResult.rows[0].COUNT > 0;

      // Step 2: Complete any IN_PROGRESS tasks that have exceeded 1 minute from started_at
      const completeSql = `
        UPDATE wes_tasks
        SET status = 'COMPLETED',
            updated_at = SYSTIMESTAMP,
            completed_at = SYSTIMESTAMP
        WHERE status = 'IN_PROGRESS'
          AND started_at IS NOT NULL
          AND started_at <= SYSTIMESTAMP - INTERVAL '1' MINUTE
      `;

      const completeResult = await connection.execute(completeSql, {}, { autoCommit: true });
      const tasksCompleted = completeResult.rowsAffected || 0;

      // Step 3: If no task is IN_PROGRESS (or we just completed one), start the next PENDING task
      if (!hasInProgress || tasksCompleted > 0) {
        // Find the next PENDING task (by priority DESC, then created_at ASC)
        const findNextTaskSql = `
          SELECT task_id
          FROM wes_tasks
          WHERE status = 'PENDING'
          ORDER BY priority DESC, created_at ASC
          FETCH FIRST 1 ROWS ONLY
        `;

        const nextTaskResult = await connection.execute(
          findNextTaskSql,
          {},
          { outFormat: db.oracledb.OUT_FORMAT_OBJECT }
        );

        if (nextTaskResult.rows.length > 0) {
          const nextTaskId = nextTaskResult.rows[0].TASK_ID;

          // Start this task (move to IN_PROGRESS and set started_at)
          const startTaskSql = `
            UPDATE wes_tasks
            SET status = 'IN_PROGRESS',
                started_at = SYSTIMESTAMP,
                updated_at = SYSTIMESTAMP
            WHERE task_id = :taskId
              AND status = 'PENDING'
          `;

          await connection.execute(
            startTaskSql,
            { taskId: nextTaskId },
            { autoCommit: true }
          );
        }
      }

    } catch (err) {
      console.error('Error in autoProgressTasks:', err);
      throw err;
    } finally {
      if (connection) {
        try {
          await connection.close();
        } catch (closeErr) {
          console.error('Error closing connection:', closeErr);
        }
      }
    }
  }
}

module.exports = Task;
