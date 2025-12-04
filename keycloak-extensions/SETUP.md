# Single Device Authentication Setup Guide

## Overview
This extension enforces single-device authentication in Keycloak by:
- Tracking device IDs during login
- Preventing multiple concurrent sessions from different devices
- Adding deviceId to access tokens and ID tokens

## Prerequisites
- Keycloak extension JAR is deployed and Keycloak is restarted
- Extension is loaded (check Keycloak logs for SPI registration)

## Keycloak GUI Configuration Steps

### Step 1: Create a Custom Authentication Flow

**Important**: You cannot modify built-in flows directly. You must copy them first.

#### Option A: For Browser-Based Login (Recommended)

1. **Navigate to Authentication Flows**
   - Go to: `Authentication` → `Flows` in the left sidebar
   - Select your realm (e.g., `fortressbank-realm`)

2. **Copy the Forms Sub-Flow**
   - Scroll down to find the `forms` flow (it's a sub-flow, not top-level)
   - Click the dropdown menu (three dots) next to `forms`
   - Select `Duplicate`
   - Give it a name like: `forms with single device`
   - Click `Ok`

3. **Add Single Device Authenticator to the Copied Flow**
   - Click on your new `forms with single device` flow
   - Click `Add execution` button
   - In the dropdown, select: `Single Device Authenticator`
   - Set requirement to: `REQUIRED`
   - Click `Add`

4. **Position the Authenticator**
   - Use the up/down arrows to move it to the correct position
   - **Important**: Place it AFTER `Username Password Form` but BEFORE any OTP/2FA steps
   - Typical order:
     1. Username Password Form (REQUIRED)
     2. **Single Device Authenticator (REQUIRED)** ← Should be here
     3. Browser - Conditional OTP (CONDITIONAL)

5. **Copy the Browser Flow**
   - Go back to the flows list
   - Find the `browser` flow (top-level flow)
   - Click the dropdown menu (three dots) next to `browser`
   - Select `Duplicate`
   - Give it a name like: `browser with single device`
   - Click `Ok`

6. **Update Browser Flow to Use New Forms Sub-Flow**
   - Click on your new `browser with single device` flow
   - Find the execution that says `forms` (it's a sub-flow execution)
   - Click the dropdown menu next to it
   - Select `Config` or click the execution to edit it
   - Change the `Alias` dropdown to select: `forms with single device`
   - Click `Save`

7. **Bind the Custom Browser Flow**
   - Go to: `Authentication` → `Flows` → `Bindings` tab
   - Find the `Browser Flow` dropdown
   - Select your new flow: `browser with single device`
   - Click `Save`

#### Option B: For Direct Grant (API) Login

1. **Copy the Direct Grant Flow**
   - Go to: `Authentication` → `Flows`
   - Find the `direct grant` flow
   - Click the dropdown menu (three dots) → `Duplicate`
   - Name it: `direct grant with single device`

2. **Add Single Device Authenticator**
   - Click on your new `direct grant with single device` flow
   - Click `Add execution`
   - Select: `Single Device Authenticator`
   - Set to: `REQUIRED`
   - Position it AFTER `Direct Grant - Validate Password` and BEFORE `Direct Grant - Conditional OTP`

3. **Bind the Flow**
   - Go to: `Authentication` → `Flows` → `Bindings` tab
   - Find `Direct Grant Flow` dropdown
   - Select: `direct grant with single device`
   - Click `Save`

### Step 2: Configure the Authenticator

1. **Open Authenticator Configuration**
   - Click the config icon (⚙️) next to `Single Device Authenticator`
   - Or click `Actions` → `Config` next to the execution

2. **Configure Settings**
   - **Alias**: Give it a name (e.g., `single-device-config`)
   - **Force login on new device**: 
     - `true`: New login from different device will logout existing sessions
     - `false`: New login from different device will be rejected (returns 409 Conflict)
   - **Require deviceId**: 
     - `true`: Login requests without deviceId will be rejected (returns 400 Bad Request)
     - `false`: Allows login without deviceId (uses "unknown" as deviceId)

3. **Save Configuration**
   - Click `Save`

### Step 3: Verify Protocol Mapper (Already Configured)

The Device ID protocol mapper should already be configured for the `kong` client. Verify:

1. **Navigate to Client Configuration**
   - Go to: `Clients` → Select `kong` client
   - Click on `Client scopes` tab → `kong-dedicated` (or check mappers directly)

2. **Check Mapper Configuration**
   - Look for mapper named: `Device ID`
   - Verify it includes:
     - `id.token.claim`: true
     - `access.token.claim`: true
     - `claim.name`: deviceId

## Testing

### Test Login with Device ID

**Using Header:**
```bash
curl -X POST http://localhost:8080/realms/fortressbank-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "X-Device-Id: device-123" \
  -d "client_id=kong" \
  -d "username=testuser" \
  -d "password=yourpassword" \
  -d "grant_type=password"
```

**Using Form Parameter:**
```bash
curl -X POST http://localhost:8080/realms/fortressbank-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=kong" \
  -d "username=testuser" \
  -d "password=yourpassword" \
  -d "deviceId=device-123" \
  -d "grant_type=password"
```

### Expected Behavior

1. **First Login**: Should succeed and create a session
2. **Second Login (Same Device)**: Should succeed (refreshes session)
3. **Second Login (Different Device)**:
   - If `forceLogin=true`: Logs out first device, allows new login
   - If `forceLogin=false`: Returns 409 Conflict error

### Verify Device ID in Token

Decode the access token and verify it contains:
```json
{
  "deviceId": "device-123",
  ...
}
```

## Troubleshooting

### Error: "It is illegal to add execution to a built in flow"
**Solution**: You cannot modify built-in flows directly. You must:
1. Duplicate the flow you want to modify (e.g., `forms` or `browser`)
2. Add the authenticator to the duplicated flow
3. If you duplicated a sub-flow, also duplicate the parent flow and update it to reference the new sub-flow
4. Bind the new flow in the `Bindings` tab

### Authenticator Not Showing Up
- Verify the JAR is in Keycloak's `providers` directory
- Check Keycloak logs for SPI registration errors
- Restart Keycloak after deploying the extension
- Verify the extension is loaded: Go to `Authentication` → `Required Actions` and check if you see custom authenticators

### Device ID Not in Token
- Verify the protocol mapper is configured for your client
- Check that the mapper is included in the client's default scopes
- Ensure the user session note is being set (check Keycloak logs)
- Verify the mapper is enabled (not disabled)

### Login Always Fails
- Check authenticator requirement is not set to DISABLED
- Verify the authenticator is positioned correctly in the flow
- Check Keycloak logs for authentication errors
- Ensure deviceId is being sent in the request (check headers or form parameters)

### Flow Not Working After Binding
- Verify you've saved all changes
- Check that the sub-flow references are correct (if using a custom browser flow)
- Try logging out and logging back in to clear old sessions
- Check Keycloak logs for flow execution errors

## Configuration Examples

### Strict Single Device (Recommended for Banking)
- Force login: `true`
- Require deviceId: `true`
- Requirement: `REQUIRED`

### Lenient Single Device (Allow Unknown Devices)
- Force login: `true`
- Require deviceId: `false`
- Requirement: `REQUIRED`

### Optional Single Device (Allow Multiple Devices)
- Force login: `false`
- Require deviceId: `false`
- Requirement: `ALTERNATIVE` or `DISABLED`

