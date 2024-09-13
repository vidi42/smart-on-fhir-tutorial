/* (C)2024 */
package ro.vidi.smart_on_fhir_tutorial;

import lombok.Data;

@Data
public class AppState {

    private String tokenUrl;

    private String otherDetails;

    private String clientId;

    private String fhirServerUrl;
}
