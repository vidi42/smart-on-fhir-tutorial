/* (C)2024 */
package ro.vidi.smart_on_fhir_tutorial.view;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientResponseException;
import ro.vidi.smart_on_fhir_tutorial.AppState;
import ro.vidi.smart_on_fhir_tutorial.FhirClient;
import ro.vidi.smart_on_fhir_tutorial.OidcClient;

@Route("smart-callback")
@Slf4j
public class SmartCallbackView extends VerticalLayout implements HasUrlParameter<String> {

    private final FhirClient fhirClient;

    private final OidcClient oidcClient;

    private final TextArea authorizationCode;
    private final TextArea encodedState;
    private final TextArea accessToken;
    private final Button accessFhirInfo;

    private OIDCTokenResponse accessTokenResponse = null;

    public SmartCallbackView(FhirClient fhirClient, OidcClient oidcClient) {

        this.fhirClient = fhirClient;
        this.oidcClient = oidcClient;

        authorizationCode = new TextArea("Code");
        authorizationCode.setWidth("100%");
        authorizationCode.setReadOnly(true);
        authorizationCode.setPlaceholder("Code received from the oauth flow");
        authorizationCode.addValueChangeListener(
                event -> {
                    if (event.getValue() != null) {
                        authorizationCode.setHelperComponent(
                                new Html(
                                        """
<p>View on <a href="https://jwt.io?token=%s" target="_blank">jwt.io</a> (pre-populated).</p>
"""
                                                .formatted(event.getValue())));
                    } else {
                        authorizationCode.setHelperComponent(null);
                    }
                });

        encodedState = new TextArea("State");
        encodedState.setWidth("100%");
        encodedState.setReadOnly(true);
        encodedState.setPlaceholder("Code received from the oauth flow");
        encodedState.setHelperComponent(
                new Html(
                        """
<p>
    Base64 encoded JSON containing the details passed when starting the flow.
</p>
"""));

        Button getAccessTokenButton = new Button("Get access token");
        getAccessTokenButton.addClickListener(event -> obtainAccessToken());

        accessToken = new TextArea("Access Token");
        accessToken.setWidth("100%");
        accessToken.setReadOnly(true);
        accessToken.setHelperComponent(
                new Html(
                        """
<p>
    See <a href="https://www.hl7.org/fhir/smart-app-launch/app-launch.html#obtain-access-token" target="_blank">How to exchange authorization code for access token</a> for more details.
</p>
"""));

        Button restartFlowButton = new Button("Restart");
        restartFlowButton.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("")));

        accessFhirInfo = new Button("Access FHIR Data");
        accessFhirInfo.setEnabled(false);
        accessFhirInfo.addClickListener(
                event -> {
                    Map<String, List<String>> queryParameters = new HashMap<>();
                    queryParameters.put(
                            "token",
                            Collections.singletonList(
                                    accessTokenResponse
                                            .getOIDCTokens()
                                            .getAccessToken()
                                            .getValue()));
                    queryParameters.put(
                            "patientId",
                            Collections.singletonList(
                                    String.valueOf(
                                            accessTokenResponse
                                                    .getCustomParameters()
                                                    .get("patient"))));
                    queryParameters.put(
                            "state", Collections.singletonList(encodedState.getValue()));
                    getUI().ifPresent(
                                    ui ->
                                            ui.navigate(
                                                    "smart-fhir-client",
                                                    new QueryParameters(queryParameters)));
                });

        HorizontalLayout buttonLayout = new HorizontalLayout(restartFlowButton, accessFhirInfo);
        buttonLayout.setWidth("100%");

        add(
                new H1("SMART Callback"),
                new Text(
                        "Now that the authorization code was obtained, exchange it for an access"
                                + " token to be used when accessing the FHIR data."),
                authorizationCode,
                encodedState,
                getAccessTokenButton,
                accessToken,
                buttonLayout);
    }

    private void obtainAccessToken() {

        try {
            AppState state = fhirClient.decodeState(encodedState.getValue());
            accessTokenResponse =
                    oidcClient.getAccessToken(
                            state.getTokenUrl(), authorizationCode.getValue(), state.getClientId());
            this.accessToken.setValue(accessTokenResponse.toJSONObject().toJSONString());
            this.accessFhirInfo.setEnabled(true);
        } catch (IOException
                | RestClientResponseException
                | URISyntaxException
                | ParseException e) {
            log.error(e.getMessage(), e);
            ViewUtils.showNotificationError("Cannot obtain access token. Check the logs.");
            this.accessFhirInfo.setEnabled(false);
        }
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String parameter) {
        Location location = beforeEvent.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();

        queryParameters.getSingleParameter("code").ifPresent(authorizationCode::setValue);
        queryParameters.getSingleParameter("state").ifPresent(encodedState::setValue);
    }
}
