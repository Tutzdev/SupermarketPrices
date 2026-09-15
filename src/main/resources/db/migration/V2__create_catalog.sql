CREATE TABLE states (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(2) NOT NULL UNIQUE
);

CREATE TABLE cities (
    id UUID PRIMARY KEY,
    state_id UUID NOT NULL REFERENCES states(id),
    name VARCHAR(120) NOT NULL,
    UNIQUE (state_id, name)
);

CREATE TABLE data_sources (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    base_url VARCHAR(2048) NOT NULL,
    enabled BOOLEAN NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE supermarket_chains (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    source_id UUID NOT NULL REFERENCES data_sources(id),
    source_reference VARCHAR(2048) NOT NULL,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (source_id, source_reference)
);

CREATE TABLE stores (
    id UUID PRIMARY KEY,
    supermarket_chain_id UUID NOT NULL REFERENCES supermarket_chains(id),
    city_id UUID NOT NULL REFERENCES cities(id),
    name VARCHAR(160) NOT NULL,
    address VARCHAR(500),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    active BOOLEAN NOT NULL,
    source_id UUID NOT NULL REFERENCES data_sources(id),
    source_reference VARCHAR(2048) NOT NULL,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT stores_latitude_bounds CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT stores_longitude_bounds CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT stores_coordinate_pair CHECK ((latitude IS NULL) = (longitude IS NULL)),
    UNIQUE (source_id, source_reference)
);

CREATE INDEX stores_city_active_idx ON stores(city_id, active);
CREATE INDEX stores_chain_idx ON stores(supermarket_chain_id);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    gtin VARCHAR(14) UNIQUE,
    name VARCHAR(200) NOT NULL,
    brand VARCHAR(120),
    description VARCHAR(2000),
    unit VARCHAR(30),
    quantity NUMERIC(14, 4),
    category VARCHAR(120),
    source_id UUID NOT NULL REFERENCES data_sources(id),
    source_reference VARCHAR(2048) NOT NULL,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT products_quantity_positive CHECK (quantity > 0),
    CONSTRAINT products_gtin_format CHECK (gtin IS NULL OR gtin ~ '^[0-9]{14}$')
);

CREATE TABLE product_source_references (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    source_id UUID NOT NULL REFERENCES data_sources(id),
    source_reference VARCHAR(2048) NOT NULL,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (source_id, source_reference)
);

CREATE INDEX product_source_references_product_idx ON product_source_references(product_id);
