DROP TABLE ngramspace.document;
    
CREATE TABLE ngramspace.document (
  hash text PRIMARY KEY,
  content text,
  key text
) WITH comment='Texts'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;