-- Incidents table
CREATE TABLE IF NOT EXISTS incidents (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    affected_service VARCHAR(200),
    root_cause TEXT,
    remediation_steps TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    dedup_count INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT chk_severity CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO')),
    CONSTRAINT chk_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'MITIGATED', 'RESOLVED', 'CLOSED'))
);

-- Incident analyses table
CREATE TABLE IF NOT EXISTS incident_analyses (
    id BIGSERIAL PRIMARY KEY,
    analysis_id VARCHAR(100) NOT NULL UNIQUE,
    incident_id BIGINT NOT NULL,
    analysis_content TEXT,
    root_cause_analysis TEXT,
    remediation_steps TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_service VARCHAR(100) NOT NULL,
    CONSTRAINT fk_incident FOREIGN KEY (incident_id) REFERENCES incidents (id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_severity ON incidents(severity);
CREATE INDEX idx_incidents_affected_service ON incidents(affected_service);
CREATE INDEX idx_incidents_created_at ON incidents(created_at);
CREATE INDEX idx_incidents_incident_id ON incidents(incident_id);
CREATE INDEX idx_incident_analyses_incident_id ON incident_analyses(incident_id);
CREATE INDEX idx_incident_analyses_analysis_id ON incident_analyses(analysis_id);
CREATE INDEX idx_incident_analyses_created_at ON incident_analyses(created_at);
