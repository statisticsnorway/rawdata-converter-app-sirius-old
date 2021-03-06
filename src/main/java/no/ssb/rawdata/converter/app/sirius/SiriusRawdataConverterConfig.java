package no.ssb.rawdata.converter.app.sirius;

import com.google.common.base.Strings;
import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@ConfigurationProperties("rawdata.converter.app.sirius")
@Data
public class SiriusRawdataConverterConfig {

    @NotEmpty
    private String schemaFileSkattemelding;

    @NotEmpty
    private String schemaFileHendelse;

    @NotEmpty
    private String gjelderPeriode;

    public String toDebugString() {
        return debugItem("schema skattemelding", schemaFileSkattemelding)
          + debugItem("schema hendelse", schemaFileHendelse)
          + debugItem("gjelder periode", gjelderPeriode);
    }

    private String debugItem(String label, Object value) {
        return Strings.padEnd(label, 24, '.') + " " + value + "\n";
    }

}
