#!/bin/bash
# Shared .env parsing for dev.sh and start.sh
# Source this file to load environment variables from .env

load_env() {
    if [ -f ".env" ]; then
        while IFS= read -r line || [ -n "$line" ]; do
            [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
            line=$(echo "$line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')
            [[ -z "$line" ]] && continue
            key="${line%%=*}"
            value="${line#*=}"
            key=$(echo "$key" | xargs)
            value=$(echo "$value" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^"\(.*)"$/\1/' -e "s/^'\(.*\)'$/\1/")
            export "$key"="$value"
        done < ".env"
    fi
}
