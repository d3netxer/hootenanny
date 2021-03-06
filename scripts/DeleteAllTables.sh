#!/bin/bash

source $HOOT_HOME/conf/DatabaseConfig.sh

export PGPASSWORD=$DB_PASSWORD

# Drop all tables
SQL=$(psql -h localhost -t -A -d $DB_NAME -U $DB_USER -c "select 'drop table \"' || tablename || '\" cascade;' from pg_tables where tableowner like '$DB_USER';")
echo $SQL | psql -h localhost -d $DB_NAME -U $DB_USER

# Drop all sequences
SQL=$(psql -h localhost -t -A -d $DB_NAME -U $DB_USER -c "SELECT 'DROP SEQUENCE \"' || relname || '\";' FROM pg_class c WHERE c.relkind='S';")
echo $SQL | psql -h localhost -d $DB_NAME -U $DB_USER
