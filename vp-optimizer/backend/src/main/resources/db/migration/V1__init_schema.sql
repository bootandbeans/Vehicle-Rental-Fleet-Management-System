-- ---------------------------------------------------------------------------
-- V1 : core schema for the Product & Volume Point optimization system
-- ---------------------------------------------------------------------------

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE products (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    sku            VARCHAR(64),
    description    VARCHAR(500),
    mrp            NUMERIC(14, 2) NOT NULL,
    volume_point   INTEGER        NOT NULL,
    category_id    BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    min_quantity   INTEGER,
    max_quantity   INTEGER,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_products_mrp_non_negative CHECK (mrp >= 0),
    CONSTRAINT ck_products_vp_non_negative CHECK (volume_point >= 0),
    CONSTRAINT ck_products_min_quantity CHECK (min_quantity IS NULL OR min_quantity >= 0),
    CONSTRAINT ck_products_max_quantity CHECK (max_quantity IS NULL OR max_quantity >= 1),
    CONSTRAINT ck_products_quantity_range CHECK (
        min_quantity IS NULL OR max_quantity IS NULL OR max_quantity >= min_quantity),
    CONSTRAINT uk_products_sku UNIQUE (sku)
);

CREATE INDEX idx_products_active ON products (active);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_sku ON products (sku);
CREATE INDEX idx_products_name ON products (name);

CREATE TABLE optimization_sessions (
    id                     BIGSERIAL PRIMARY KEY,
    name                   VARCHAR(150),
    status                 VARCHAR(32)   NOT NULL,
    discount_percent       NUMERIC(7, 4) NOT NULL,
    gst_percent            NUMERIC(7, 4) NOT NULL,
    target_vp              INTEGER       NOT NULL,
    tolerance_type         VARCHAR(20)   NOT NULL,
    tolerance_value        NUMERIC(10, 4) NOT NULL,
    min_vp                 INTEGER       NOT NULL,
    max_vp                 INTEGER       NOT NULL,
    result_limit           INTEGER       NOT NULL,
    selected_product_count INTEGER       NOT NULL,
    solution_count         INTEGER       NOT NULL DEFAULT 0,
    alternative_count      INTEGER       NOT NULL DEFAULT 0,
    dp_capacity            INTEGER,
    evaluated_states       BIGINT,
    engine_millis          BIGINT,
    message                VARCHAR(500),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_sessions_discount CHECK (discount_percent >= 0 AND discount_percent <= 100),
    CONSTRAINT ck_sessions_gst CHECK (gst_percent >= 0 AND gst_percent <= 100),
    CONSTRAINT ck_sessions_target CHECK (target_vp >= 0),
    CONSTRAINT ck_sessions_tolerance CHECK (tolerance_value >= 0),
    CONSTRAINT ck_sessions_result_limit CHECK (result_limit >= 1)
);

CREATE INDEX idx_sessions_created_at ON optimization_sessions (created_at);

-- Snapshot of every product that took part in a session (ALLOWED / REQUIRED).
-- Products deliberately excluded from a session are simply absent.
CREATE TABLE optimization_session_products (
    id                       BIGSERIAL PRIMARY KEY,
    optimization_session_id  BIGINT       NOT NULL REFERENCES optimization_sessions (id) ON DELETE CASCADE,
    product_id               BIGINT REFERENCES products (id) ON DELETE SET NULL,
    product_name             VARCHAR(150) NOT NULL,
    sku                      VARCHAR(64),
    category_name            VARCHAR(80),
    selection_type           VARCHAR(20)  NOT NULL,
    mrp                      NUMERIC(14, 2) NOT NULL,
    volume_point             INTEGER      NOT NULL,
    min_quantity             INTEGER      NOT NULL,
    max_quantity             INTEGER      NOT NULL
);

CREATE INDEX idx_session_products_session ON optimization_session_products (optimization_session_id);
CREATE INDEX idx_session_products_product ON optimization_session_products (product_id);

-- Results are stored as a full snapshot so historical sessions stay reproducible even after
-- product prices / VP values change.
CREATE TABLE optimization_results (
    id                      BIGSERIAL PRIMARY KEY,
    optimization_session_id BIGINT        NOT NULL REFERENCES optimization_sessions (id) ON DELETE CASCADE,
    solution_rank           INTEGER       NOT NULL,
    is_alternative          BOOLEAN       NOT NULL DEFAULT FALSE,
    within_range            BOOLEAN       NOT NULL,
    exact_target            BOOLEAN       NOT NULL,
    canonical_key           VARCHAR(1000) NOT NULL,
    total_vp                INTEGER       NOT NULL,
    vp_difference           INTEGER       NOT NULL,
    total_quantity          INTEGER       NOT NULL,
    number_of_unique_products INTEGER     NOT NULL,
    total_mrp               NUMERIC(16, 2) NOT NULL,
    total_discount          NUMERIC(16, 2) NOT NULL,
    total_gst               NUMERIC(16, 2) NOT NULL,
    final_payable_amount    NUMERIC(16, 2) NOT NULL,
    cost_per_vp             NUMERIC(16, 4),
    explanation             VARCHAR(1000) NOT NULL
);

CREATE INDEX idx_results_session ON optimization_results (optimization_session_id);
CREATE INDEX idx_results_rank ON optimization_results (optimization_session_id, solution_rank);

CREATE TABLE optimization_result_lines (
    id                        BIGSERIAL PRIMARY KEY,
    optimization_result_id    BIGINT       NOT NULL REFERENCES optimization_results (id) ON DELETE CASCADE,
    product_id                BIGINT,
    product_name              VARCHAR(150) NOT NULL,
    sku                       VARCHAR(64),
    category_name             VARCHAR(80),
    quantity                  INTEGER      NOT NULL,
    mrp                       NUMERIC(14, 2) NOT NULL,
    volume_point              INTEGER      NOT NULL,
    total_vp                  INTEGER      NOT NULL,
    is_required               BOOLEAN      NOT NULL DEFAULT FALSE,
    discount_percent          NUMERIC(7, 4)  NOT NULL,
    gst_percent               NUMERIC(7, 4)  NOT NULL,
    discounted_unit_price     NUMERIC(14, 2) NOT NULL,
    gst_unit_amount           NUMERIC(14, 2) NOT NULL,
    final_unit_price          NUMERIC(14, 2) NOT NULL,
    total_product_cost        NUMERIC(16, 2) NOT NULL,
    CONSTRAINT ck_lines_quantity CHECK (quantity >= 0)
);

CREATE INDEX idx_lines_result ON optimization_result_lines (optimization_result_id);
