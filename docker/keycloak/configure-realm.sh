#!/bin/bash
# Realm settings that the realm import cannot express (ADR 0006, section 5). Runs after every start and is
# idempotent. Both settings lock the user's name after registration, so the copy in app_user cannot drift:
#
# - Keycloak always adds account/manage-account to the default roles when it creates the account client, also on
#   import. Without it, users cannot edit their profile through the account console or its REST API.
# - The UPDATE_PROFILE required action also serves application-initiated requests (kc_action=UPDATE_PROFILE), which
#   anyone can start by building the URL. Disabling it closes that way to edit the profile. The import cannot
#   disable it alone: a partial requiredActions list replaces Keycloak's defaults.
#
# The e-mail address needs nothing here: it is the username, and editing the username is off in the realm.
set -euo pipefail

readonly REALM=talmar
readonly CONFIG=/tmp/kcadm.config

kcadm() {
  /opt/keycloak/bin/kcadm.sh "$@" --config "$CONFIG"
}

kcadm config credentials --server http://keycloak:8080 --realm master \
  --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD"

default_account_roles=$(kcadm get-roles -r "$REALM" --rname "default-roles-$REALM" --cclientid account)
if [[ "$default_account_roles" == *'"name" : "manage-account"'* ]]; then
  kcadm remove-roles -r "$REALM" --rname "default-roles-$REALM" --cclientid account --rolename manage-account
fi

kcadm update authentication/required-actions/UPDATE_PROFILE -r "$REALM" -s enabled=false

echo "Realm $REALM configured"
