package com.uit.fortressbank.keycloak;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Protocol mapper that injects the deviceId stored in the user session notes into
 * both the access token and ID token.
 */
public class DeviceIdProtocolMapper extends AbstractOIDCProtocolMapper {

    public static final String PROVIDER_ID = "device-id-protocol-mapper";
    public static final String DEVICE_ID_CLAIM = "deviceId";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        // Reuse standard OIDC mapper helpers for claim configuration
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(CONFIG_PROPERTIES);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(CONFIG_PROPERTIES, DeviceIdProtocolMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Device ID Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return "Token Mapper";
    }

    @Override
    public String getHelpText() {
        return "Adds the deviceId stored in the user session notes to access and ID tokens.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    protected void setClaim(IDToken token,
                            ProtocolMapperModel mappingModel,
                            UserSessionModel userSession,
                            KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx) {
        if (userSession == null) {
            return;
        }
        String deviceId = userSession.getNote(SingleDeviceAuthenticator.DEVICE_ID_NOTE_KEY);
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }

        // Determine the claim name from config or fall back to default
        String claimName = mappingModel.getConfig() != null
                ? mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME)
                : null;
        if (claimName == null || claimName.isBlank()) {
            claimName = DEVICE_ID_CLAIM;
        }

        token.getOtherClaims().put(claimName, deviceId);
    }

    public static ProtocolMapperModel create(String name, boolean includeInAccessToken, boolean includeInIdToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        var config = new java.util.HashMap<String, String>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, DEVICE_ID_CLAIM);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, String.valueOf(includeInAccessToken));
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, String.valueOf(includeInIdToken));
        mapper.setConfig(config);

        return mapper;
    }
}


