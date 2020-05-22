DROP TABLE ngramspace.gram1;
CREATE TABLE ngramspace.gram1 (
  word text PRIMARY KEY,
  value bigint
) WITH comment='Texts'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;

DROP TABLE ngramspace.gram2;
CREATE TABLE ngramspace.gram2 (
  word text PRIMARY KEY,
  value bigint
) WITH comment='Texts'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;

DROP TABLE ngramspace.gram3;
CREATE TABLE ngramspace.gram3 (
  word text PRIMARY KEY,
  value bigint
) WITH comment='Texts'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;

DROP TABLE ngramspace.gram4;
CREATE TABLE ngramspace.gram4 (
  word text PRIMARY KEY,
  value bigint
) WITH comment='Texts'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;

DROP TABLE ngramspace.gram5;
CREATE TABLE ngramspace.gram5 (
  word text PRIMARY KEY,
  value bigint
) WITH comment='Texts'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;
