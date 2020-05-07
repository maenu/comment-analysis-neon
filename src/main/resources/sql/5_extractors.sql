CREATE TABLE "{{data}}_5_extractors"
(
    "partition"  INTEGER NOT NULL,
    "heuristics" BLOB    NOT NULL,
    "dictionary" BLOB    NOT NULL,
    PRIMARY KEY ("partition")
)
