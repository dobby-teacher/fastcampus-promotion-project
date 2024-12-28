DROP TABLE IF EXISTS point_balance;
DROP TABLE IF EXISTS point;
DROP TABLE IF EXISTS daily_point_report;

CREATE TABLE point_balance
(
    id         BIGINT AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    balance    BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_point_balance_user_id UNIQUE (user_id)
);

CREATE TABLE point
(
    id               BIGINT AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL,
    amount           BIGINT      NOT NULL,
    type             VARCHAR(20) NOT NULL,
    description      VARCHAR(255),
    balance_snapshot BIGINT      NOT NULL,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE daily_point_report
(
    id            BIGINT AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    report_date   DATE   NOT NULL,
    earn_amount   BIGINT NOT NULL DEFAULT 0,
    use_amount    BIGINT NOT NULL DEFAULT 0,
    cancel_amount BIGINT NOT NULL DEFAULT 0,
    net_amount    BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_daily_point_report UNIQUE (user_id, report_date)
);
