#!/usr/bin/env bash
set -euo pipefail

NOTICE='Copyright © 2026 Aarav Singh (Pauze). All rights reserved.'

status=0

check_file() {
  local file="$1"
  if [[ -f "$file" ]] && ! grep -Fq "$NOTICE" "$file"; then
    echo "Missing copyright notice: $file"
    status=1
  fi
}

if [[ "$#" -gt 0 ]]; then
  for file in "$@"; do
    case "$file" in
      app/*|backend/*)
        case "$file" in
          *.kt|*.kts|*.java|*.js|*.ts|*.tsx|*.sql|*.xml) check_file "$file" ;;
        esac
        ;;
    esac
  done
else
  while IFS= read -r -d '' file; do
    check_file "$file"
  done < <(find app backend -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.java' -o -name '*.js' -o -name '*.ts' -o -name '*.tsx' -o -name '*.sql' -o -name '*.xml' \) -print0)
fi

exit "$status"
