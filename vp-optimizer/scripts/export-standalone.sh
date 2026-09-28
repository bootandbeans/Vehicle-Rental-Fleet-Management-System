#!/bin/sh
# ---------------------------------------------------------------------------
# Exports this folder as a *standalone* repository - a copy whose root is the
# project itself, the layout published at github.com/bootandbeans/vp-optimizer.
#
# Inside the monorepo the project lives in `vp-optimizer/`, so a handful of
# files carry that prefix: Render's dockerfilePath/dockerContext, the Netlify
# base directory, and the paths quoted in README.md / DEPLOYMENT.md / fly.toml
# and in the Settings page copy. This script copies the folder to TARGET and
# rewrites exactly those references. No source file, dependency, migration or
# engine behaviour is touched, so `npm test`, `mvn test` and `docker compose up`
# behave identically in both layouts.
#
# Usage:
#   scripts/export-standalone.sh <target-dir>
#
# Example (produces a ready to push repository):
#   scripts/export-standalone.sh ../vp-optimizer-standalone
#   cd ../vp-optimizer-standalone
#   git init -b main && git add -A && git commit -m "Initial commit"
#   git remote add origin https://github.com/bootandbeans/vp-optimizer.git
#   git push -u origin main
# ---------------------------------------------------------------------------
set -eu

usage() {
    echo "usage: $(basename "$0") <target-dir>" >&2
    exit 2
}

case "${1:-}" in
    "" | -h | --help) usage ;;
esac

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
source_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)

target=$1
case "$target" in
    /*) ;;
    *) target="$PWD/$target" ;;
esac
case "$target" in
    "$source_dir" | "$source_dir"/*)
        echo "error: the target must be outside $source_dir" >&2
        exit 1
        ;;
esac

echo "export: $source_dir -> $target"
rm -rf "$target"
mkdir -p "$target"
# tar (instead of cp) keeps hidden files and needs no rsync.
(cd "$source_dir" && tar cf - .) | (cd "$target" && tar xf -)
# Drop dependencies and build output that must never end up in a repository.
find "$target" -type d \
    \( -name node_modules -o -name dist -o -name .vite -o -name coverage -o -name target \) \
    -prune -exec rm -rf {} +
rm -rf "$target/.git"

# Everything below rewrites files *inside the target*.
cd "$target"

# Portable in-place replace: `sed -i` differs between GNU and BSD.
replace() {
    sed "s|$2|$3|g" "$1" > "$1.tmp" && mv "$1.tmp" "$1"
}

# 1. Documentation: monorepo paths -> repository root paths.
for file in README.md DEPLOYMENT.md; do
    # `cd vp-optimizer` disappears: the project *is* the repository root.
    sed '/^cd vp-optimizer$/d' "$file" > "$file.tmp" && mv "$file.tmp" "$file"
    replace "$file" 'cd vp-optimizer/backend' 'cd backend'
    replace "$file" 'cd vp-optimizer/frontend' 'cd frontend'
    replace "$file" 'git add vp-optimizer' 'git add .'
    replace "$file" 'git push origin <your-branch>' 'git push -u origin main'
    replace "$file" 'vp-optimizer/render.yaml' 'render.yaml'
    replace "$file" 'vp-optimizer/DEPLOYMENT.md' 'DEPLOYMENT.md'
    replace "$file" 'vp-optimizer/.gitignore' '.gitignore'
    replace "$file" 'vp-optimizer/backend' 'backend'
    replace "$file" 'vp-optimizer/frontend' 'frontend'
done

# 2. Render Blueprint: the build context is the repository root now.
replace render.yaml './vp-optimizer/backend/Dockerfile' './backend/Dockerfile'
replace render.yaml './vp-optimizer/backend' './backend'
replace render.yaml 'vp-optimizer/DEPLOYMENT.md' 'DEPLOYMENT.md'
replace render.yaml 'The repository is a monorepo, so both paths are relative to the repo root.' \
    'Both paths are relative to the repository root.'

# 3. Fly.io config: commands are run from the backend folder inside this repo.
replace backend/fly.toml '(from vp-optimizer/backend)' '(from backend/)'
replace backend/fly.toml 'vp-optimizer/DEPLOYMENT.md' 'DEPLOYMENT.md'

# 4. Settings page copy names the folder the backend engine lives in.
replace frontend/src/pages/Settings/SettingsPage.tsx 'vp-optimizer/backend' 'backend'

# 5. Netlify: the same settings as the monorepo root file, minus the monorepo base.
cat > netlify.toml <<'NETLIFY'
# ---------------------------------------------------------------------------
# Netlify configuration for the VP Optimizer frontend (frontend/).
# Netlify reads this file from the repository root on import - no dashboard
# settings, no base directory to configure.
#
# Full walkthrough: DEPLOYMENT.md
# ---------------------------------------------------------------------------

[build]
  base = "frontend"
  command = "npm ci && npm run build"
  # Relative to `base`.
  publish = "dist"

[build.environment]
  NODE_VERSION = "22"

# ---------------------------------------------------------------------------
# Redirects are evaluated top to bottom, first match wins.
# ---------------------------------------------------------------------------

# Optional: same-origin proxy to the Spring Boot API. Uncomment and replace the
# host, then set the backend's CORS_ALLOWED_ORIGIN_PATTERNS if you also want
# direct cross-origin calls. While this stays commented out, the UI simply runs
# its offline demo engine (the health probe rejects the SPA fallback document).
#
# [[redirects]]
#   from = "/api/*"
#   to = "https://vp-optimizer-api.onrender.com/api/:splat"
#   status = 200
#   force = true

# Single page app: client side routes such as /results/1001 are served by index.html.
[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
NETLIFY

echo "export: done - $(find . -type f | wc -l | tr -d ' ') files"
echo "export: remaining 'vp-optimizer/' references (expected: the layout labels in README.md):"
grep -rn "vp-optimizer/" . \
    --include='*.md' --include='*.yml' --include='*.yaml' --include='*.toml' \
    --include='*.json' --include='*.ts' --include='*.tsx' --include='*.java' \
    --include='*.sql' --include='*.conf' --include='*.env' --include='*.example' \
    | sed 's|^\./||' || echo "  (none)"
