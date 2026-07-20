#!/bin/sh
set -eu

result="$(psql --username=postgres --dbname=postgres --tuples-only --no-align \
    --set=app_user="$APP_DB_USER" --set=app_db="$APP_DB_NAME" <<'SQL'
SELECT 1
WHERE EXISTS (SELECT FROM pg_roles WHERE rolname = :'app_user')
  AND EXISTS (SELECT FROM pg_database WHERE datname = :'app_db');
SQL
)"

[ "$result" = "1" ]
