package no.ssb.rawdata.converter.app.sirius;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.ssb.avro.convert.xml.XmlToRecords;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.rawdata.converter.core.AggregateSchemaBuilder;
import no.ssb.rawdata.converter.core.RawdataConverter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;

import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Singleton
@Slf4j
public class SiriusRawdataConverter implements RawdataConverter {
    private final Schema skattemeldingSchema;
    private final Schema aggregateSchema;

    private static final String ELEMENT_NAME_SIRIUS_SKATTEMELDING = "skattemeldingUtflatet";
    private static final String DEFAULT_SCHEMA_FILE_SIRIUS_SKATTEMELDING = "schema/sirius-skattemelding.avsc";

    public SiriusRawdataConverter(SiriusRawdataConverterConfig siriusConverterConfig) {

        try {
            skattemeldingSchema = new Schema.Parser().parse(loadSchema(siriusConverterConfig.getSchemaFileSkattemelding(), DEFAULT_SCHEMA_FILE_SIRIUS_SKATTEMELDING));
            aggregateSchema = new AggregateSchemaBuilder("no.ssb.dataset")
              .schema(ELEMENT_NAME_SIRIUS_SKATTEMELDING, skattemeldingSchema)
              .build();
        }
        catch (IOException e) {
            throw new SiriusRawdataConverterException("Unable to locate avro schema. " + e.getMessage());
        }
    }

    @Override
    public Schema targetAvroSchema() {
        return aggregateSchema;
    }

    @Override
    public GenericRecord convert(RawdataMessage rawdataMessage) {
        log.trace("convert sirius rawdata message {}", rawdataMessage);
        GenericRecordBuilder rootRecordBuilder = new GenericRecordBuilder(aggregateSchema);
        SiriusItem siriusItem = SiriusItem.from(rawdataMessage);
        if (siriusItem.hasSkattemelding()) {
            xmlToAvro(siriusItem.getSkattemeldingXml(), ELEMENT_NAME_SIRIUS_SKATTEMELDING, skattemeldingSchema, rootRecordBuilder);
        }
        else {
            log.info("Missing skattemelding data for sirius item {}", siriusItem.toIdString());
        }

        return rootRecordBuilder.build();
    }

    // TODO: Split up this code. Gotcha: try with resources will close the stream
    void xmlToAvro(byte[] xmlData, String rootXmlElementName, Schema schema, GenericRecordBuilder recordBuilder) {
        InputStream xmlInputStream = new ByteArrayInputStream(xmlData);
        try (XmlToRecords xmlToRecords = new XmlToRecords(xmlInputStream, rootXmlElementName, schema)) {
            xmlToRecords.forEach(record ->
              recordBuilder.set(ELEMENT_NAME_SIRIUS_SKATTEMELDING, record)
            );
        } catch (XMLStreamException | IOException e) {
            throw new SiriusRawdataConverterException("Error converting Sirius XML", e);
        }
    }

    @SneakyThrows
    private InputStream loadSchema(Optional<String> schemaFile, String defaultSchemaFile) {
        try {
            if (schemaFile.isPresent()) {
                return new FileInputStream(Paths.get(schemaFile.get()).toAbsolutePath().normalize().toFile());
            }
            else {
                return getClass().getClassLoader().getResourceAsStream(defaultSchemaFile);
            }
        }
        catch (FileNotFoundException e) {
            throw new SiriusRawdataConverterException("Unable to locate avro schema: " + e.getMessage());
        }
    }

    static class SiriusRawdataConverterException extends RuntimeException {
        public SiriusRawdataConverterException(String message) {
            super(message);
        }

        public SiriusRawdataConverterException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
