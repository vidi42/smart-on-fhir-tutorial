/* (C)2024 */
package ro.vidi.smart_on_fhir_tutorial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class OidcClient {

    @Autowired private Environment environment;

    @Autowired private ObjectMapper mapper;

    /**
     * Builds an authorize URL as described in the <a
     * href="https://www.hl7.org/fhir/smart-app-launch/app-launch.html">SMART App Launch Doc</a>.
     *
     * <pre>
     * https://ehr/authorize?
     *      response_type=code&
     *      client_id=app-client-id&
     *      redirect_uri=https%3A%2F%2Fapp%2Fafter-auth&
     *      launch=xyz123&
     *      scope=launch+patient%2FObservation.rs+patient%2FPatient.rs+openid+fhirUser&
     *      state=98wrghuwuogerg97&
     *      aud=https://ehr/fhir
     * </pre>
     */
    public URI buildAuthorizationUrl(
            String fhirServerUrl,
            String authorizeUrl,
            String clientId,
            Set<String> scopes,
            AppState state)
            throws URISyntaxException, JsonProcessingException {

        Scope scope = new Scope();
        scopes.forEach(scope::add);

        return new AuthenticationRequest.Builder(
                        ResponseType.CODE,
                        scope,
                        new ClientID(clientId),
                        new URI(
                                "http://127.0.0.1:"
                                        + environment.getProperty("local.server.port")
                                        + "/smart-callback"))
                .endpointURI(new URI(authorizeUrl))
                .responseType(ResponseType.CODE)
                .state(
                        new State(
                                new String(
                                        Base64.encodeBase64(
                                                mapper.writeValueAsString(state).getBytes()))))
                .customParameter("aud", fhirServerUrl)
                .build()
                .toURI();
    }

    public OIDCTokenResponse getAccessToken(String tokenUrl, String code, String clientId)
            throws URISyntaxException, IOException, ParseException {
        URI tokenEndpoint = new URI(tokenUrl);
        AuthorizationGrant codeGrant =
                new AuthorizationCodeGrant(
                        new AuthorizationCode(code),
                        new URI(
                                "http://127.0.0.1:"
                                        + environment.getProperty("local.server.port")
                                        + "/smart-callback"));

        TokenRequest request = new TokenRequest(tokenEndpoint, new ClientID(clientId), codeGrant);
        TokenResponse tokenResponse = OIDCTokenResponseParser.parse(request.toHTTPRequest().send());
        return (OIDCTokenResponse) tokenResponse.toSuccessResponse();
    }

    public SmartMetadata getWellKnownInfo(String fhirServerUrl) throws IOException, ParseException {
        OIDCProviderConfigurationRequest oidcProviderConfigurationRequest =
                new OIDCProviderConfigurationRequest(new Issuer(fhirServerUrl));
        OIDCProviderMetadata parse =
                OIDCProviderMetadata.parse(
                        oidcProviderConfigurationRequest
                                .toHTTPRequest()
                                .send()
                                .getContentAsJSONObject());

        SmartMetadata smartMetadata = new SmartMetadata();
        smartMetadata.setAuthorizeUrl(parse.getAuthorizationEndpointURI().toString());
        smartMetadata.setTokenUrl(parse.getTokenEndpointURI().toString());
        smartMetadata.setJsonResponse(parse.toJSONObject().toJSONString());

        return smartMetadata;
    }
}
