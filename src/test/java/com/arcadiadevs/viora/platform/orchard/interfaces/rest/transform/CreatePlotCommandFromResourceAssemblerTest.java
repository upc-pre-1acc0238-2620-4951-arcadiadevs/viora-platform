package com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.CreatePlotResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreatePlotCommandFromResourceAssembler Unit Tests")
class CreatePlotCommandFromResourceAssemblerTest {

    @Test
    @DisplayName("Should transform CreatePlotResource to DelimitPlotCommand with all attributes mapped")
    void shouldTransformResourceToCommand() {
        var resource = new CreatePlotResource(
                "Cuartel San Jerónimo",
                "CRIOLLA",
                "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}",
                7.0,
                5.0
        );

        DelimitPlotCommand command = CreatePlotCommandFromResourceAssembler.toCommandFromResource(
                "550e8400-e29b-41d4-a716-446655440000",
                resource
        );

        assertThat(command).isNotNull();
        assertThat(command.producerId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(command.name()).isEqualTo("Cuartel San Jerónimo");
        assertThat(command.variety()).isEqualTo("CRIOLLA");
        assertThat(command.geometry()).isEqualTo(resource.polygonGeoJson());
        assertThat(command.rowSpacingM()).isEqualTo(7.0);
        assertThat(command.treeSpacingM()).isEqualTo(5.0);
    }
}
