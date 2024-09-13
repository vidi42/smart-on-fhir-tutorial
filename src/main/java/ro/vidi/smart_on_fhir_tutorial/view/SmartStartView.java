/* (C)2024 */
package ro.vidi.smart_on_fhir_tutorial.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import ro.vidi.smart_on_fhir_tutorial.AppState;
import ro.vidi.smart_on_fhir_tutorial.FhirClient;
import ro.vidi.smart_on_fhir_tutorial.OidcClient;
import ro.vidi.smart_on_fhir_tutorial.SmartMetadata;

@Route("smart-start")
@Slf4j
public class SmartStartView extends VerticalLayout {

    private final FhirClient fhirClient;
    private final OidcClient oidcClient;

    private final TextArea fhirServerUrlTextArea;
    private final Button discoverUsingFhirMetadata;
    private final Button discoverUsingOidcConfig;
    private final TextArea metadataAuthorizeUrl;
    private final TextArea metadataTokenUrl;
    private final TextField clientIdTextField;
    private final MultiSelectComboBox<String> scopesMultiSelectComboBox;
    private final TextArea authorizationUrlTextArea;
    private final TextArea metadataFullContent;

    public SmartStartView(FhirClient fhirClient, OidcClient oidcClient) {

        this.fhirClient = fhirClient;
        this.oidcClient = oidcClient;

        fhirServerUrlTextArea = new TextArea("FHIR Server URL");
        fhirServerUrlTextArea.setWidth("100%");
        fhirServerUrlTextArea.setRequired(true);
        fhirServerUrlTextArea.addValueChangeListener(
                event -> enableDiscoverMetadataButtons(isValidURL(event.getValue())));
        fhirServerUrlTextArea.setHelperComponent(
                new Html(
                        "<p>See <a href=\"https://launch.smarthealthit.org\""
                                + " target=\"_blank\">https://launch.smarthealthit.org</a> for"
                                + " simulating a FHIR server."));

        Button defaultFhirServerUrlButton = new Button("Use default");
        defaultFhirServerUrlButton.addClickListener(
                event -> fhirServerUrlTextArea.setValue(fhirClient.getDefaultFhirServerUrl()));

        HorizontalLayout fhirServerUrlLayout =
                new HorizontalLayout(fhirServerUrlTextArea, defaultFhirServerUrlButton);
        fhirServerUrlLayout.setWidth("100%");
        fhirServerUrlLayout.setVerticalComponentAlignment(
                Alignment.CENTER, defaultFhirServerUrlButton);

        discoverUsingFhirMetadata = new Button("Discover URLs using FHIR Metadata");
        discoverUsingFhirMetadata.addClickListener(event -> setSmartMetadata(true));
        discoverUsingOidcConfig = new Button("Discover URLs using OIDC Configuration");
        discoverUsingOidcConfig.addClickListener(event -> setSmartMetadata(false));

        HorizontalLayout discoverMetadataLayout =
                new HorizontalLayout(discoverUsingFhirMetadata, discoverUsingOidcConfig);
        discoverMetadataLayout.setWidth("100%");

        metadataAuthorizeUrl = new TextArea("Authorize URL");
        metadataAuthorizeUrl.setReadOnly(true);
        metadataAuthorizeUrl.setWidth("100%");

        metadataTokenUrl = new TextArea("Token URL");
        metadataTokenUrl.setReadOnly(true);
        metadataTokenUrl.setWidth("100%");

        metadataFullContent = new TextArea("Metadata content");
        metadataFullContent.setReadOnly(true);
        metadataFullContent.setWidth("100%");
        metadataFullContent.setMaxHeight("200px");

        VerticalLayout urlsLayout = new VerticalLayout(metadataAuthorizeUrl, metadataTokenUrl);
        urlsLayout.setWidth("100%");
        VerticalLayout fullLayout = new VerticalLayout(metadataFullContent);
        fullLayout.setWidth("100%");

        Accordion metadataAccordion = new Accordion();
        metadataAccordion.setWidth("100%");
        metadataAccordion.add("Auth Flow URLs", urlsLayout);
        metadataAccordion.add("Full metadata", fullLayout);

        clientIdTextField = new TextField("Client ID");
        clientIdTextField.setWidth("100%");
        clientIdTextField.setRequired(true);
        clientIdTextField.setValue("smart-on-fhir-tutorial");
        clientIdTextField.addValueChangeListener(event -> changeAuthorizationUrl());

        Set<String> defaultScopes =
                new HashSet<>(
                        Arrays.asList(
                                "openid", "fhirUser", "profile", "launch/patient", "patient/*.r"));

        scopesMultiSelectComboBox = new MultiSelectComboBox<>("Scopes");
        scopesMultiSelectComboBox.setWidth("100%");
        scopesMultiSelectComboBox.setItems(defaultScopes);
        scopesMultiSelectComboBox.select(defaultScopes);
        scopesMultiSelectComboBox.setAllowCustomValue(true);
        scopesMultiSelectComboBox.setRequired(true);
        scopesMultiSelectComboBox.addCustomValueSetListener(
                e -> {
                    String customValue = e.getDetail();
                    Set<String> selectedItems = new HashSet<>(scopesMultiSelectComboBox.getValue());
                    defaultScopes.add(customValue);
                    selectedItems.add(customValue);
                    scopesMultiSelectComboBox.setItems(defaultScopes);
                    scopesMultiSelectComboBox.setValue(selectedItems);
                });
        scopesMultiSelectComboBox.setHelperComponent(
                new Html(
                        """
<p>
    See <a href="https://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context.html" target="_blank">FHIR Scopes and launch context doc.</a> for more details.
</p>
"""));
        scopesMultiSelectComboBox.addValueChangeListener(event -> changeAuthorizationUrl());

        var parametersLayout = new HorizontalLayout(clientIdTextField, scopesMultiSelectComboBox);
        parametersLayout.setWidth("100%");

        authorizationUrlTextArea = new TextArea("Authorization URL");
        authorizationUrlTextArea.setReadOnly(true);
        authorizationUrlTextArea.setWidth("100%");
        authorizationUrlTextArea.setHelperComponent(
                new Html(
                        """
<div>
    <p>
        See <a href="https://www.hl7.org/fhir/smart-app-launch/app-launch.html#obtain-authorization-code" target="_blank">How to obtain authorization code</a> for more details on the URL structure.
    </p>
    <p>
        The state is a JSON containing details passed to the authorization flow and sent back into the callback.
    </p>
</div>
"""));

        var startButton = new Button("Authorize");
        startButton.addClickListener(
                click ->
                        getUI().ifPresent(
                                        ui ->
                                                ui.getPage()
                                                        .setLocation(
                                                                authorizationUrlTextArea
                                                                        .getValue())));
        startButton.addClickShortcut(Key.ENTER);

        add(
                new H1("SMART Start"),
                new Text(
                        "Configure the FHIR Server URL, discover the Auth URLs, then start the"
                                + " authorization flow."),
                fhirServerUrlLayout,
                discoverMetadataLayout,
                metadataAccordion,
                parametersLayout,
                authorizationUrlTextArea,
                startButton);
    }

    private void enableDiscoverMetadataButtons(boolean enabled) {
        discoverUsingFhirMetadata.setEnabled(enabled);
        discoverUsingOidcConfig.setEnabled(enabled);
    }

    private void setSmartMetadata(boolean usingFhirMetadata) {
        var fhirServerUrl = fhirServerUrlTextArea.getValue();

        if (StringUtils.isBlank(fhirServerUrl)) {
            return;
        }

        try {
            SmartMetadata smartMetadata;
            if (usingFhirMetadata) {
                smartMetadata = fhirClient.getMetadataSmartUrls(fhirServerUrl);
            } else {
                smartMetadata = oidcClient.getWellKnownInfo(fhirServerUrl);
            }

            metadataAuthorizeUrl.setValue(smartMetadata.getAuthorizeUrl());
            metadataTokenUrl.setValue(smartMetadata.getTokenUrl());
            metadataFullContent.setValue(smartMetadata.getJsonResponse());

            authorizationUrlTextArea.setValue(getAuthorizationUrl(fhirServerUrl));
        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
            ViewUtils.showNotificationError("Failed to read smart URLs. Check the logs.");
        } catch (IOException | ParseException e) {
            log.error(e.getMessage(), e);
            ViewUtils.showNotificationError("Cannot build the state object. Check the logs.");
        }
    }

    private void changeAuthorizationUrl() {
        if (authorizationUrlTextArea != null
                && StringUtils.isNoneBlank(
                        fhirServerUrlTextArea.getValue(), clientIdTextField.getValue())
                && !CollectionUtils.isEmpty(scopesMultiSelectComboBox.getValue())) {
            try {
                authorizationUrlTextArea.setValue(
                        getAuthorizationUrl(fhirServerUrlTextArea.getValue()));
            } catch (URISyntaxException | JsonProcessingException e) {
                log.error(e.getMessage(), e);
                ViewUtils.showNotificationError(
                        "Failed to build authorization URL. Check the logs.");
            }
        }
    }

    private String getAuthorizationUrl(String fhirServerUrl)
            throws URISyntaxException, JsonProcessingException {

        AppState state = new AppState();
        state.setTokenUrl(metadataTokenUrl.getValue());
        state.setOtherDetails("local_details");
        state.setClientId(clientIdTextField.getValue());
        state.setFhirServerUrl(fhirServerUrl);

        return oidcClient
                .buildAuthorizationUrl(
                        fhirServerUrl,
                        metadataAuthorizeUrl.getValue(),
                        clientIdTextField.getValue(),
                        scopesMultiSelectComboBox.getValue(),
                        state)
                .toString();
    }

    public boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
}
