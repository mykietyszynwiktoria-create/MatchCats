CREATE TABLE mc_message_reads (
    message_id bigint NOT NULL REFERENCES mc_messages(id) ON DELETE CASCADE,
    reader_id integer NOT NULL REFERENCES mc_accounts(id) ON DELETE CASCADE,
    read_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id, reader_id)
);
CREATE INDEX mc_message_reads_reader_idx ON mc_message_reads(reader_id);
