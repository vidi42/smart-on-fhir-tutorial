/* (C)2024 */
package ro.vidi.smart_on_fhir_tutorial;

import lombok.Data;

@Data
public class SmartMetadata {

    private String authorizeUrl;

    private String tokenUrl;

    private String jsonResponse;
}
