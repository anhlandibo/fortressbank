package com.uit.fortressbank.keycloak;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class SingleDeviceAuthenticatorFactory implements AuthenticatorFactory {

    public static final String ID = "single-device-authenticator";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty forceLogin = new ProviderConfigProperty();
        forceLogin.setName(SingleDeviceAuthenticator.CONFIG_FORCE_LOGIN);
        forceLogin.setLabel("Force login on new device");
        forceLogin.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        forceLogin.setDefaultValue("true");
        forceLogin.setHelpText("If true, a new login from a different deviceId will invalidate existing sessions. If false, the new login will be rejected.");
        CONFIG_PROPERTIES.add(forceLogin);

        ProviderConfigProperty requireDeviceId = new ProviderConfigProperty();
        requireDeviceId.setName(SingleDeviceAuthenticator.CONFIG_REQUIRE_DEVICE_ID);
        requireDeviceId.setLabel("Require deviceId");
        requireDeviceId.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        requireDeviceId.setDefaultValue("true");
        requireDeviceId.setHelpText("If true, login requests without deviceId will be rejected.");
        CONFIG_PROPERTIES.add(requireDeviceId);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayType() {
        return "Single Device Authenticator";
    }

    @Override
    public String getHelpText() {
        return "Enforces a single active device per user by comparing deviceId across user sessions.";
    }

    @Override
    public String getReferenceCategory() {
        return "single-device";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new SingleDeviceAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
        // No global config
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No-op
    }

    @Override
    public void close() {
        // No-op
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }
}


