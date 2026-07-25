CREATE TABLE IF NOT EXISTS merchant_application (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id  VARCHAR(36)  NOT NULL,
    merchant_name   VARCHAR(255) NOT NULL,
    merchant_number VARCHAR(32)  NULL,
    status          VARCHAR(32)  NOT NULL,
    failure_reason  VARCHAR(512) NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (application_id)
);
