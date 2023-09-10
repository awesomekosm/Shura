--Migration on Thu Sep  7 09:29:53 PM EDT 2023
CREATE TABLE media
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id bigint NOT NULL,
    name VARCHAR,
    artist VARCHAR,
    album VARCHAR,
    link VARCHAR,
    guid VARCHAR,
    source VARCHAR,
    request_guid VARCHAR,
    request_time VARCHAR,
    start_time VARCHAR,
    finish_time VARCHAR
);

CREATE INDEX media_request_finish_source_idx ON media (request_guid, finish_time, source);

