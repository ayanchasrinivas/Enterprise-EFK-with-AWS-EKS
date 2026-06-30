-- Notification logs table
CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(500) NOT NULL,
    message_content TEXT,
    status VARCHAR(50) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP
);

-- Notification templates table
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL UNIQUE,
    channel VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    body TEXT,
    json_template TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Notification channels table
CREATE TABLE IF NOT EXISTS notification_channels (
    id BIGSERIAL PRIMARY KEY,
    channel_type VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    webhook_url VARCHAR(500) NOT NULL,
    token VARCHAR(100),
    channel_id VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT true,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_notification_logs_incident_id ON notification_logs(incident_id);
CREATE INDEX idx_notification_logs_status ON notification_logs(status);
CREATE INDEX idx_notification_logs_channel ON notification_logs(channel);
CREATE INDEX idx_notification_logs_created_at ON notification_logs(created_at);
CREATE INDEX idx_notification_templates_channel ON notification_templates(channel);
CREATE INDEX idx_notification_templates_active ON notification_templates(active);
CREATE INDEX idx_notification_channels_channel_type ON notification_channels(channel_type);
CREATE INDEX idx_notification_channels_active ON notification_channels(active);

-- Insert default Slack template
INSERT INTO notification_templates (template_name, channel, title, body, json_template, active, created_at, updated_at)
VALUES (
    'incident-alert-slack',
    'slack',
    'Incident Alert',
    'New incident: {{title}}',
    '{"text": "{{title}}", "blocks": []}',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- Insert default Teams template
INSERT INTO notification_templates (template_name, channel, title, body, json_template, active, created_at, updated_at)
VALUES (
    'incident-alert-teams',
    'teams',
    'Incident Alert',
    'New incident: {{title}}',
    '{"@type": "MessageCard", "@context": "https://schema.org/extensions"}',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;
