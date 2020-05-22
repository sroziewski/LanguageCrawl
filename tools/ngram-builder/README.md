# The Ngram Builder

First of all, you have to build Cassandra DB, you can make use of docker-compose.yaml file provided. Next, creation of keyspace and tables is crucial.
Start with the ngramtables.sql file.

In the virtual machine:
docker ps

Copy a file onto container

docker cp ngramtables.sql count.sql document.sql ngrams.sql container_id:/

go to virtual container

docker exec -it container_id bash

and in container bash, run command for other files too

cqlsh -f ngramtables.sql
cqlsh -f count.sql
cqlsh -f document.sql
cqlsh -f ngrams.sql

This can be done on the machine itself, container is now running, only for DB.

First, the app has to be compiled and the artefact built.

sbt assembly

Run the app

java -jar package-name.jar -n ngram-length (1-5)
