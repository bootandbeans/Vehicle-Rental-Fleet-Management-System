#!/bin/sh
# ---------------------------------------------------------------------------
# Container entrypoint.
#
# Managed platforms (Render, Railway, Fly.io, Heroku, ...) usually hand out a
# single connection string:
#
#     DATABASE_URL=postgresql://user:password@host:5432/database?sslmode=require
#
# Spring Boot needs a JDBC URL, so the two are bridged here - but only when the
# application was not configured explicitly. Everything is a no-op for
# `docker compose` and for any deployment that sets DB_HOST/DB_PORT/... or
# SPRING_DATASOURCE_URL itself, so no behaviour changes silently.
#
# Notes / limits:
#   * a JDBC URL passed in DATABASE_URL is used as is;
#   * a missing port falls back to 5432, a missing database to the user name;
#   * percent-encoded credentials (`p%40ss`) are not decoded - set
#     SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD in that case.
# ---------------------------------------------------------------------------
set -e

if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
    case "$DATABASE_URL" in
        jdbc:*)
            export SPRING_DATASOURCE_URL="$DATABASE_URL"
            echo "entrypoint: DATABASE_URL is already a JDBC URL"
            ;;
        postgres://* | postgresql://*)
            without_scheme="${DATABASE_URL#*://}"
            case "$without_scheme" in
                *@*)
                    credentials="${without_scheme%%@*}"
                    location="${without_scheme#*@}"
                    export SPRING_DATASOURCE_USERNAME="${credentials%%:*}"
                    case "$credentials" in
                        *:*) export SPRING_DATASOURCE_PASSWORD="${credentials#*:}" ;;
                    esac
                    ;;
                *)
                    # No credentials in the URL - the platform supplies them separately.
                    location="$without_scheme"
                    ;;
            esac
            case "$location" in
                *\?*)
                    query="?${location#*\?}"
                    location="${location%%\?*}"
                    ;;
                *)
                    query=""
                    ;;
            esac
            host_and_port="${location%%/*}"
            database="${location#*/}"
            case "$host_and_port" in
                *:*) ;;
                *) host_and_port="${host_and_port}:5432" ;;
            esac
            case "$database" in
                "$location") database="" ;;
            esac
            export SPRING_DATASOURCE_URL="jdbc:postgresql://${host_and_port}/${database}${query}"
            echo "entrypoint: DATABASE_URL translated to ${SPRING_DATASOURCE_URL}"
            ;;
        *)
            echo "entrypoint: DATABASE_URL has an unsupported scheme, ignoring it" >&2
            ;;
    esac
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar
