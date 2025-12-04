package com.uit.fortressbank.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SingleDeviceAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(SingleDeviceAuthenticator.class);

    public static final String CONFIG_FORCE_LOGIN = "forceLogin";
    public static final String CONFIG_REQUIRE_DEVICE_ID = "requireDeviceId";
    public static final String DEVICE_ID_NOTE_KEY = "deviceId";
    public static final String DEVICE_ID_PARAM = "deviceId";
    public static final String DEVICE_ID_HEADER = "X-Device-Id";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        var httpRequest = context.getHttpRequest();

        String deviceId = extractDeviceId(httpRequest.getDecodedFormParameters(),
                httpRequest.getHttpHeaders().getRequestHeaders().getFirst(DEVICE_ID_HEADER));

        boolean requireDeviceId = getBooleanConfig(context, CONFIG_REQUIRE_DEVICE_ID, true);

        if (deviceId == null || deviceId.isBlank()) {
            if (requireDeviceId) {
                LOG.warn("Missing deviceId on login request");
                // Do not set any session note if deviceId is required but missing
                Response challenge = Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"deviceId_required\",\"error_description\":\"deviceId is required for login\"}")
                        .type("application/json")
                        .build();
                context.failureChallenge(AuthenticationFlowError.INVALID_CLIENT_SESSION, challenge);
                return;
            } else {
                deviceId = "unknown";
            }
        }

        // Persist deviceId into the authentication session notes so that once a user session
        // is established, it will be available as a user session note for protocol mappers.
        if (context.getAuthenticationSession() != null) {
            context.getAuthenticationSession().setUserSessionNote(DEVICE_ID_NOTE_KEY, deviceId);
        }

        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();
        UserModel user = context.getUser();

        if (user == null) {
            LOG.debug("No user yet in context; skipping single-device enforcement");
            context.success();
            return;
        }

        List<UserSessionModel> existingSessions = session.sessions()
                .getUserSessionsStream(realm, user)
                .collect(Collectors.toList());

        if (existingSessions == null || existingSessions.isEmpty()) {
            // First login: allow and set note later when session is created.
            context.success();
            return;
        }

        boolean forceLogin = getBooleanConfig(context, CONFIG_FORCE_LOGIN, true);

        for (UserSessionModel existing : existingSessions) {
            String existingDeviceId = existing.getNote(DEVICE_ID_NOTE_KEY);

            if (existingDeviceId == null) {
                existingDeviceId = "";
            }

            if (Objects.equals(existingDeviceId, deviceId)) {
                // Same device; allow login and refresh note
                existing.setNote(DEVICE_ID_NOTE_KEY, deviceId);
                context.success();
                return;
            }
        }

        if (forceLogin) {
            LOG.infof("Force login enabled; removing %d existing sessions for user %s",
                    existingSessions.size(), user.getUsername());
            for (UserSessionModel existing : existingSessions) {
                session.sessions().removeUserSession(realm, existing);
            }
            context.success();
        } else {
            LOG.infof("Blocking login for user %s: already logged in on another device", user.getUsername());
            Response challenge = Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"already_logged_in\",\"error_description\":\"Already logged in on another device\"}")
                    .type("application/json")
                    .build();
            context.failureChallenge(AuthenticationFlowError.USER_CONFLICT, challenge);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // No-op; all logic handled in authenticate
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions
    }

    @Override
    public void close() {
        // No-op
    }

    private String extractDeviceId(MultivaluedMap<String, String> formParameters, String headerDeviceId) {
        if (formParameters != null) {
            String fromForm = formParameters.getFirst(DEVICE_ID_PARAM);
            if (fromForm != null && !fromForm.isBlank()) {
                return fromForm;
            }
        }
        if (headerDeviceId != null && !headerDeviceId.isBlank()) {
            return headerDeviceId;
        }
        return null;
    }

    private boolean getBooleanConfig(AuthenticationFlowContext context, String key, boolean defaultValue) {
        String value = context.getAuthenticatorConfig() != null
                ? context.getAuthenticatorConfig().getConfig().get(key)
                : null;
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}


