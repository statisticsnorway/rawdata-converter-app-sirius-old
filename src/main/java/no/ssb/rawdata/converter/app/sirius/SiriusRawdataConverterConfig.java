package no.ssb.rawdata.converter.app.sirius;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@ConfigurationProperties("rawdata.converter.app.sirius")
@Data
@Slf4j
public class SiriusRawdataConverterConfig {

    private String schemaFileSkattemeldingUtflatet = "schema/sirius-skattemelding-utflatet.avsc";
    private String schemaFileHendelse = "schema/sirius-hendelse.avsc";

}
