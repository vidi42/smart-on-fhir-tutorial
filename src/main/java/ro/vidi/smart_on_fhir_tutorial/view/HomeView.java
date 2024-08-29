package ro.vidi.smart_on_fhir_tutorial.view;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class HomeView extends VerticalLayout {

    public HomeView() {

        Button standaloneProviderLaunchFlow = new Button("Start Standalone Provider Launch Flow");
        standaloneProviderLaunchFlow.addClickListener(event -> getUI().ifPresent(
                ui -> ui.navigate("smart-start")));
        add(
                new H1("Welcome"),
                new Text("This Sample application will guide you trough the SMART on FHIR flow."),
                standaloneProviderLaunchFlow
                );

    }

}
