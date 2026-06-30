const { Pool } = require('pg');
const winston = require('winston');

// Logger setup
const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.json(),
  defaultMeta: { service: 'notification-worker' },
  transports: [
    new winston.transports.Console({
      format: winston.format.combine(
        winston.format.colorize(),
        winston.format.simple()
      )
    })
  ]
});

// Database connection
const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'tasks_db',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || 'postgres'
});

// Initialize notifications table
async function initDB() {
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS notifications (
        id SERIAL PRIMARY KEY,
        task_id UUID NOT NULL,
        message VARCHAR(255),
        sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    logger.info('Notification database initialized');
  } catch (err) {
    logger.error('Database init failed:', err);
  }
}

// Send notifications for status changes
async function processNotifications() {
  try {
    const result = await pool.query(`
      SELECT * FROM tasks
      WHERE status != 'PENDING'
      AND updated_at > NOW() - INTERVAL '5 minutes'
    `);

    for (const task of result.rows) {
      // Simulate sending notification (in real app: email, webhook, etc.)
      logger.info(`Sending notification for task: ${task.id} with status: ${task.status}`);

      // Store in notifications table
      await pool.query(
        `INSERT INTO notifications (task_id, message) VALUES ($1, $2)`,
        [task.id, `Task "${task.title}" status changed to ${task.status}`]
      );
    }

    if (result.rows.length > 0) {
      logger.info(`Processed ${result.rows.length} notifications`);
    }
  } catch (err) {
    logger.error('Error processing notifications:', err);
  }
}

// Main worker loop
async function startWorker() {
  await initDB();
  logger.info('Notification worker started');

  // Process notifications every 30 seconds
  setInterval(processNotifications, 30000);

  // Initial run
  await processNotifications();
}

startWorker();

// Graceful shutdown
process.on('SIGTERM', async () => {
  logger.info('Shutting down notification worker');
  await pool.end();
  process.exit(0);
});
