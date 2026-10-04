#!/usr/bin/env bash
set -euo pipefail

NOTICE='Copyright © 2026 Aarav Singh (Pauze). All rights reserved.'

status=0
while IFS= read -r -d '' file; do
  if ! grep -Fq "$NOTICE" "$file"; then
    echo "Missing copyright notice: $file"
    status=1
  fi
done < <(find app backend -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.java' -o -name '*.js' -o -name '*.ts' -o -name '*.tsx' -o -name '*.sql' -o -name '*.xml' \) -print0)

exit "$status"
