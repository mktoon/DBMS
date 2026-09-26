#!/bin/sh
# Run inside the Compose database container, including with an existing V1 volume.
# Never echo the generated SQL or credentials. Base64 carries password bytes;
# MySQL QUOTE() escapes them before CREATE/ALTER USER is prepared.
set -eu
: "${MYSQL_ROOT_PASSWORD:?Set MYSQL_ROOT_PASSWORD}"
: "${MYSQL_APP_PASSWORD:?Set MYSQL_APP_PASSWORD}"
password64=$(printf '%s' "$MYSQL_APP_PASSWORD" | base64 | tr -d '\n')
export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"
mysql -uroot <<SQL
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
SET @app_password = CONVERT(FROM_BASE64('$password64') USING utf8mb4);
SET @create_user = CONCAT('CREATE USER IF NOT EXISTS hospital_app@\'%\' IDENTIFIED BY ', QUOTE(@app_password));
PREPARE provision FROM @create_user;
EXECUTE provision;
DEALLOCATE PREPARE provision;
SET @alter_user = CONCAT('ALTER USER hospital_app@\'%\' IDENTIFIED BY ', QUOTE(@app_password));
PREPARE provision FROM @alter_user;
EXECUTE provision;
DEALLOCATE PREPARE provision;
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'hospital_app'@'%';
GRANT 'hospital_v1_app' TO 'hospital_app'@'%';
SET DEFAULT ROLE 'hospital_v1_app' TO 'hospital_app'@'%';
SQL
printf '%s\n' 'Application database account is ready.'
