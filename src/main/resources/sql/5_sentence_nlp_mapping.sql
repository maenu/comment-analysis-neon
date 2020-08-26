CREATE TABLE "{{data}}_5_sentence_nlp_mapping"
(
    "class"                TEXT    NOT NULL,
    "category_sentence_id" INTEGER NOT NULL,
    "category_sentence"    TEXT    NOT NULL,
    "category"             TEXT    NOT NULL,
    "stratum"              NUMERIC    NOT NULL,
    "heuristics"           TEXT    NOT NULL,
    FOREIGN KEY ("class") REFERENCES "{{data}}_0_raw" ("class"),
    PRIMARY KEY ("class", "category_sentence_id"),
    FOREIGN KEY ("category_sentence_id") REFERENCES "{{data}}_2_sentence" ("id")
)