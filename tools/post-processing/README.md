# Document post-processing
An App for Cassandra document post-processing via Akka.

First of all, you have to build Cassandra DB, you can make use of docker-compose.yaml file provided. Next, creation of keyspace and tables is crucial.
Start with the ngramtables.sql file.

In the virtual machine:
`docker ps`

Copy a file onto container

docker cp ngramtables.sql count.sql document.sql ngrams.sql container_id:/

go to virtual container

`docker exec -it container_id bash`

and in container bash, run command for other files too

```
cqlsh -f ngramtables.sql
cqlsh -f count.sql
cqlsh -f document.sql
cqlsh -f ngrams.sql
```

This can be done on the machine itself, container is now running, only for DB.

First, the app has to be compiled and the artefact built.

`sbt assembly`

1. The first step is for document (sentences) extraction, the parameter mode -m de, the parameter -s (sentence length), here is 4.
Now, the table document is filled with documents, having more than 4 sentences without broken character encoding, with sentences extracted.

You can check in on the virtual container
```
docker exec -it container_id bash
cqlsh
select * from ngramspace.document limit 10;

java -jar file.jar -m de -s 4
```

2. Second step is for word statistics calculation
`java -jar file.jar -m s `

This time the count table is filled with unigrams, we use it for word statistics to know which words are very rare, and finally we can omitt them before building ngramspace



