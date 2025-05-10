#!/bin/bash
set -e

# Сначала изменяем конфигурацию сервера для использования md5
# Это изменение будет применено после перезапуска сервера, который делает entrypoint
echo "password_encryption = md5" >> "$PGDATA/postgresql.conf"
# Также убедимся, что pg_hba.conf требует md5
{
    echo "# pg_hba.conf modified by init script to use md5"
    echo "local   all             all                                     trust"
    echo "host    all             all             0.0.0.0/0               md5"
    echo "host    all             all             ::/0                    md5"
} > "$PGDATA/pg_hba.conf"
echo "postgresql.conf and pg_hba.conf updated for md5."

# Теперь, после того как сервер будет перезапущен entrypoint-скриптом Docker
# и применит password_encryption = md5, следующая команда ALTER USER
# создаст md5-хэш. Однако, скрипты в initdb.d выполняются до этого
# полноценного перезапуска.
# Поэтому, более надежно, если сам образ при инициализации пользователя
# учтет password_encryption = md5.

# Давайте попробуем сначала только изменить pg_hba.conf на md5
# и оставить password_encryption как есть (scram-sha-256 по умолчанию для PG15).
# А пароль установим через ALTER USER.
# Если это не сработает, то нужно будет изменять postgresql.conf и перезапускать
# сервер внутри скрипта, что сложнее.

# ОБНОВЛЕННЫЙ, БОЛЕЕ ПРОСТОЙ СКРИПТ ДЛЯ ТЕСТА MD5:
# Мы просто меняем pg_hba.conf на md5, а пароль уже установлен
# entrypoint-скриптом образа (который должен был использовать scram-sha-256).
# Если пароль был 'postgres', то md5-версия этого пароля будет отличаться от scram-sha-256.
# Нам нужно, чтобы и сервер хранил md5, и pg_hba требовал md5.

# САМЫЙ НАДЕЖНЫЙ ПОДХОД для md5:
# 1. Изменить postgresql.conf на password_encryption = md5
# 2. Изменить pg_hba.conf на host ... md5
# 3. Установить/переустановить пароль пользователя ПОСЛЕ этих изменений.

# Попробуем так:
echo "password_encryption = md5" >> "$PGDATA/postgresql.conf" # Для следующего старта

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    ALTER SYSTEM SET password_encryption = 'md5';
    SELECT pg_reload_conf(); -- Применяем немедленно, если возможно
    ALTER USER "$POSTGRES_USER" WITH PASSWORD '$POSTGRES_PASSWORD'; -- Устанавливаем пароль, который теперь должен быть md5
EOSQL
echo "User $POSTGRES_USER password (re)set for md5, password_encryption set to md5."

{
    echo "# pg_hba.conf modified by init script to use md5"
    echo "local   all             all                                     trust"
    echo "host    all             all             0.0.0.0/0               md5"
    echo "host    all             all             ::/0                    md5"
} > "$PGDATA/pg_hba.conf"
echo "pg_hba.conf updated to require md5 for all host connections."