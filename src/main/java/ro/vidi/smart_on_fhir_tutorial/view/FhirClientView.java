/* (C)2024 */
package ro.vidi.smart_on_fhir_tutorial.view;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import ro.vidi.smart_on_fhir_tutorial.AppState;
import ro.vidi.smart_on_fhir_tutorial.FhirClient;

@Route("smart-fhir-client")
@Slf4j
public class FhirClientView extends VerticalLayout implements HasUrlParameter<String> {

    private final FhirClient fhirClient;

    private final TextArea accessToken;
    private final TextField patientId;
    private final TextArea encodedState;
    private final TextArea patientDetails;

    public FhirClientView(FhirClient fhirClient) {
        accessToken = new TextArea("Access Token");
        accessToken.setWidth("100%");
        accessToken.setReadOnly(true);
        accessToken.setPlaceholder("Code received from the oauth flow");
        accessToken.addValueChangeListener(
                event -> {
                    if (event.getValue() != null) {
                        accessToken.setHelperComponent(
                                new Html(
                                        """
<p>View on <a href="https://jwt.io?token=%s" target="_blank">jwt.io</a> (pre-populated).</p>
"""
                                                .formatted(event.getValue())));
                    } else {
                        accessToken.setHelperComponent(null);
                    }
                });
        patientId = new TextField("Patient ID");
        patientId.setWidth("100%");
        patientId.setReadOnly(true);

        encodedState = new TextArea("State");
        encodedState.setWidth("100%");
        encodedState.setReadOnly(true);
        encodedState.setPlaceholder("Code received from the oauth flow");
        encodedState.setHelperComponent(
                new Html(
                        """
                        <p>
                            Base64 encoded JSON containing the state of the application.
                        </p>
                        """));

        Button getPatientDetails = new Button("Get Patient Details");
        getPatientDetails.addClickListener(event -> obtainPatientDetails());

        patientDetails = new TextArea("Patient Details");
        patientDetails.setWidth("100%");
        patientDetails.setReadOnly(true);

        Button restartFlowButton = new Button("Restart");
        restartFlowButton.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("")));

        add(
                new H1("FHIR Client"),
                new Text(
                        "Now that the Access Token was obtained, use it to read data from the FHIR"
                                + " server."),
                accessToken,
                patientId,
                getPatientDetails,
                patientDetails,
                restartFlowButton);
        this.fhirClient = fhirClient;
    }

    private void obtainPatientDetails() {

        try {
            AppState appState = fhirClient.decodeState(encodedState.getValue());
            patientDetails.setValue(
                    fhirClient.convertResourceToString(
                            fhirClient.getPatient(
                                    appState.getFhirServerUrl(),
                                    accessToken.getValue(),
                                    patientId.getValue())));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            ViewUtils.showNotificationError("Cannot obtain patient details. Check the logs.");
        }
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String parameter) {
        Location location = beforeEvent.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();

        queryParameters.getSingleParameter("token").ifPresent(accessToken::setValue);
        queryParameters.getSingleParameter("patientId").ifPresent(patientId::setValue);
        queryParameters.getSingleParameter("state").ifPresent(encodedState::setValue);
    }
}
