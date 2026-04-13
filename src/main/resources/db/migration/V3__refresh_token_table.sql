CREATE TABLE refresh_token(
        id BIGSERIAL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        token_hash VARCHAR(255) NOT NULL,
        issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        expires_at TIMESTAMP NOT NULL,
        CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user_id ON refresh_token(user_id);