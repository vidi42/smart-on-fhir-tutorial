/* (C)2024 */
package ro.vidi.smart_on_fhir_tutorial;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.client.api.IBasicClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.UriType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class FhirClient {

    @Getter
    @Value("${fhir.server.url}")
    private String defaultFhirServerUrl;

    @Autowired private Environment environment;

    @Autowired private ObjectMapper mapper;

    public SmartMetadata getMetadataSmartUrls(String fhirServerUrl) {
        IBasicClient client =
                FhirContext.forR4().newRestfulClient(IBasicClient.class, fhirServerUrl);

        CapabilityStatement capabilityStatement =
                (CapabilityStatement) client.getServerConformanceStatement();

        SmartMetadata smartMetadata = new SmartMetadata();
        smartMetadata.setJsonResponse(convertResourceToString(capabilityStatement));
        for (CapabilityStatement.CapabilityStatementRestComponent rest :
                capabilityStatement.getRest()) {
            if (rest.getSecurity() != null) {
                for (Extension extension : rest.getSecurity().getExtension()) {
                    if (extension
                                    .getUrl()
                                    .equals(
                                            "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris")
                            && !CollectionUtils.isEmpty(extension.getExtension())) {
                        for (Extension smartExtension : extension.getExtension()) {
                            switch (smartExtension.getUrl()) {
                                case "authorize":
                                    smartMetadata.setAuthorizeUrl(
                                            ((UriType) smartExtension.getValue()).getValue());
                                    break;
                                case "token":
                                    smartMetadata.setTokenUrl(
                                            ((UriType) smartExtension.getValue()).getValue());
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
        return smartMetadata;
    }

    public AppState decodeState(String stateEncoded) throws IOException {
        return mapper.readValue(Base64.decodeBase64(stateEncoded.getBytes()), AppState.class);
    }

    public Patient getPatient(String fhirServerUrl, String accessToken, String patientId) {
        IPatientClient client =
                FhirContext.forR4().newRestfulClient(IPatientClient.class, fhirServerUrl);
        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(accessToken);
        client.registerInterceptor(authInterceptor);

        return client.readPatient(new IdType(patientId));
    }

    public String convertResourceToString(IBaseResource resource) {
        return FhirContext.forR4().newJsonParser().encodeResourceToString(resource);
    }

    private interface IPatientClient extends IBasicClient {
        /**
         * Read a patient from a server by ID
         */
        @Read
        Patient readPatient(@IdParam IdType theId);
    }
}
