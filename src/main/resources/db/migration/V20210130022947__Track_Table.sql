--Migration on Sat 30 Jan 2021 02:29:47 AM EST
CREATE TABLE TRACKS(
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id                INTEGER,
    name                    TEXT,
    link                    TEXT,
    playlist_name            TEXT,
    origin                  TEXT
)
