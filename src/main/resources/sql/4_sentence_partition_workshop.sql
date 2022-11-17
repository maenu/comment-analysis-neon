CREATE TABLE "{{data}}_4_sentence_partition_workshop"
(
    "partition"           INTEGER NOT NULL,
    "category"                  TEXT    NOT NULL,
    "comment_sentence"          TEXT    NOT NULL,
    "class"                  TEXT    NOT NULL,
    FOREIGN KEY ("class") REFERENCES "{{data}}_0_raw" ("class")
)
