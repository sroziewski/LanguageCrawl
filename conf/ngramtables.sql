DROP KEYSPACE ngramspace;
CREATE KEYSPACE ngramspace WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor': 1};

    DROP TABLE ngramspace.text;
    DROP TABLE ngramspace.url;
    
    CREATE TABLE ngramspace.text (
      key text PRIMARY KEY,
      content text,
      page int
    ) WITH comment='Texts'
      AND COMPACT STORAGE
      AND read_repair_chance = 1.0;

    CREATE TABLE ngramspace.url (
      key text PRIMARY KEY,
      state int,
      completed int,
      all int
    ) WITH comment='Urls'
      AND COMPACT STORAGE
      AND read_repair_chance = 1.0;

