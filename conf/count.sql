DROP TABLE ngramspace.count;
CREATE TABLE ngramspace.count (
  word text PRIMARY KEY,
  value int
) WITH comment='Texts'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;