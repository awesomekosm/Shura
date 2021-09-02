--Migration on Thu 02 Sep 2021 06:07:02 PM EDT
CREATE TABLE track
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id bigint NOT NULL,
    link VARCHAR(255),
    name VARCHAR(255),
    origin INTEGER,
    playlist_name VARCHAR(255),
    time INTEGER NOT NULL
);
