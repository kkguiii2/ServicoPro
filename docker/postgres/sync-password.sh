#!/bin/sh
set -eu

export PGPASSWORD="$(cat /run/secrets/postgres_admin_password)"
app_password="$(cat /run/secrets/app_db_password)"

psql --host=postgres --username=postgres --dbname=postgres \
    --set=app_user="$APP_DB_USER" --set=app_password="$app_password" <<'SQL'
ALTER ROLE :"app_user" WITH PASSWORD :'app_password';
SQL

unset PGPASSWORD app_password
