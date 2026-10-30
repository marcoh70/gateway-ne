
#!/usr/bin/env bash
set -euo pipefail

# Keycloak configuration
KEYCLOAK_URL="http://localhost:9090"
REALM="ne_realm"
CLIENT_ID="react-bff"
REDIRECT_URI="http://localhost:8081/callback"

TOKEN_URL="${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token"
AUTH_URL="${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/auth"

# Generate PKCE verifier and challenge
CODE_VERIFIER=$(openssl rand -hex 32)

CODE_CHALLENGE=$(
  printf '%s' "$CODE_VERIFIER" |
    openssl dgst -sha256 -binary |
    openssl base64 -A |
    tr '+/' '-_' |
    tr -d '='
)

STATE=$(openssl rand -hex 16)

# Build authorization URL
AUTH_REQUEST="${AUTH_URL}?client_id=${CLIENT_ID}"
AUTH_REQUEST+="&response_type=code"
AUTH_REQUEST+="&scope=openid"
AUTH_REQUEST+="&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Fcallback"
AUTH_REQUEST+="&code_challenge=${CODE_CHALLENGE}"
AUTH_REQUEST+="&code_challenge_method=S256"
AUTH_REQUEST+="&state=${STATE}"

echo
echo "1. Open this URL in your browser and sign in to Keycloak:"
echo
echo "$AUTH_REQUEST"
echo
echo "2. After login, the browser will redirect to localhost:8081/callback."
echo "   Copy the value of the 'code' parameter from the URL."
echo
read -r -p "Paste the authorization code: " AUTH_CODE

# Exchange authorization code for the original access token
RESPONSE=$(
  curl -fsS -X POST "$TOKEN_URL" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=authorization_code" \
    --data-urlencode "client_id=${CLIENT_ID}" \
    --data-urlencode "code=${AUTH_CODE}" \
    --data-urlencode "redirect_uri=${REDIRECT_URI}" \
    --data-urlencode "code_verifier=${CODE_VERIFIER}"
)

ACCESS_TOKEN=$(printf '%s' "$RESPONSE" | python3 -c '
import json, sys
data = json.load(sys.stdin)
if "access_token" not in data:
    print(json.dumps(data, indent=2), file=sys.stderr)
    sys.exit(1)
print(data["access_token"])
')

printf '\nAccess token obtained successfully.\n'
printf 'Use it to call the API Gateway:\n\n'
printf 'export ACCESS_TOKEN='\''%s'\''\n' "$ACCESS_TOKEN"
