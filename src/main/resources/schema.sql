-- Create a table to store query history
CREATE TABLE IF NOT EXISTS query_history (
    id SERIAL PRIMARY KEY,
    query_text TEXT NOT NULL,
    sql_generated TEXT,
    result_count INTEGER,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    execution_time_ms BIGINT
);

-- Create a table to store table metadata
CREATE TABLE IF NOT EXISTS table_metadata (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(255) NOT NULL,
    description TEXT,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(table_name)
);

-- Create a table to store column metadata
CREATE TABLE IF NOT EXISTS column_metadata (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(255) NOT NULL,
    column_name VARCHAR(255) NOT NULL,
    data_type VARCHAR(100) NOT NULL,
    is_nullable BOOLEAN DEFAULT true,
    description TEXT,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (table_name) REFERENCES table_metadata(table_name) ON DELETE CASCADE,
    UNIQUE(table_name, column_name)
);

-- Create an index on query history for better performance
CREATE INDEX IF NOT EXISTS idx_query_history_created_at ON query_history(created_at);
CREATE INDEX IF NOT EXISTS idx_query_history_status ON query_history(status);

-- Create a function to update the last_updated timestamp
CREATE OR REPLACE FUNCTION update_last_updated()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers to automatically update timestamps
CREATE TRIGGER update_table_metadata_timestamp
BEFORE UPDATE ON table_metadata
FOR EACH ROW EXECUTE FUNCTION update_last_updated();

CREATE TRIGGER update_column_metadata_timestamp
BEFORE UPDATE ON column_metadata
FOR EACH ROW EXECUTE FUNCTION update_last_updated();
