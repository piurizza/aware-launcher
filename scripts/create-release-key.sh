#!/usr/bin/env bash
# Creates the release signing key and stores it as GitHub Actions secrets.
#
# Run once, from the repo root, with the GitHub CLI logged in (gh auth login).
# Back up the generated keystore somewhere safe: if you lose it, users must
# uninstall Aware to install future updates.
set -euo pipefail

keystore="${1:-aware-release.jks}"
alias="aware"

if [ -e "$keystore" ]; then
  echo "$keystore already exists; refusing to overwrite it." >&2
  exit 1
fi

read -rsp "Keystore password (min 6 chars): " password
echo

keytool -genkeypair -v \
  -keystore "$keystore" \
  -alias "$alias" \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$password" -keypass "$password" \
  -dname "CN=Aware"

# Secrets live in a "release" environment so only the release workflow can read them.
gh api --silent -X PUT "repos/{owner}/{repo}/environments/release"

base64 < "$keystore" | tr -d '\n' | gh secret set AWARE_KEYSTORE_BASE64 --env release
printf '%s' "$password" | gh secret set AWARE_KEYSTORE_PASSWORD --env release
printf '%s' "$password" | gh secret set AWARE_KEY_PASSWORD --env release
printf '%s' "$alias" | gh secret set AWARE_KEY_ALIAS --env release

echo "Done. Secrets saved to the 'release' environment."
echo "Now move $keystore out of the repo and back it up (it is gitignored)."
