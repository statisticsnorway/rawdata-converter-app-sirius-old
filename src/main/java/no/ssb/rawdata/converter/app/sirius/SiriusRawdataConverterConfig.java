package no.ssb.rawdata.converter.app.sirius;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@ConfigurationProperties("rawdata.converter.app.sirius")
@Value
@Slf4j
public class SiriusRawdataConverterConfig {

    private Optional<String> schemaFileSkattemeldingUtflatet = Optional.empty();

}
