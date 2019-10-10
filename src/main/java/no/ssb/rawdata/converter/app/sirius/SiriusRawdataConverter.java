package no.ssb.rawdata.converter.app.sirius;

import lombok.extern.slf4j.Slf4j;
import no.ssb.avro.convert.xml.XmlToRecords;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.rawdata.converter.core.AbstractRawdataConverter;
import no.ssb.rawdata.converter.core.AggregateSchemaBuilder;
import no.ssb.rawdata.converter.core.Metadata;
import no.ssb.rawdata.converter.core.MetadataGenericRecordBuilder;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;

import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Singleton
@Slf4j
public class SiriusRawdataConverter extends AbstractRawdataConverter {
    private final Schema skattemeldingSchema;
    private final Schema aggregateSchema;

    private static final String DEFAULT_SCHEMA_FILE_SIRIUS_SKATTEMELDING = "schema/sirius-skattemelding.avsc";
    private static final String ELEMENT_NAME_METADATA = "metadata";
    private static final String ELEMENT_NAME_SIRIUS_SKATTEMELDING = "skattemeldingUtflatet";

    public SiriusRawdataConverter(SiriusRawdataConverterConfig siriusConverterConfig) {
        skattemeldingSchema = readAvroSchema(siriusConverterConfig.getSchemaFileSkattemelding(), DEFAULT_SCHEMA_FILE_SIRIUS_SKATTEMELDING);
        aggregateSchema = new AggregateSchemaBuilder("no.ssb.dataset")
          .schema(ELEMENT_NAME_METADATA, Metadata.SCHEMA)
          .schema(ELEMENT_NAME_SIRIUS_SKATTEMELDING, skattemeldingSchema)
          .build();
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

        // TODO: Error handling. Skip this item without breaking the conversion stream completely
        if (siriusItem.hasMetadata()) {
            GenericRecord metadataRecord = MetadataGenericRecordBuilder.fromRawdataManifest(siriusItem.getMetadataJson()).build();
            rootRecordBuilder.set(ELEMENT_NAME_METADATA, metadataRecord);
        }
        else {
            log.error("Missing metadata for sirius item {}.", siriusItem.toIdString());
        }

        if (siriusItem.hasSkattemelding()) {
            xmlToAvro(siriusItem.getSkattemeldingXml(), ELEMENT_NAME_SIRIUS_SKATTEMELDING, skattemeldingSchema, rootRecordBuilder);
        }
        else {
            log.error("Missing skattemelding data for sirius item {}", siriusItem.toIdString());
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

    static class SiriusRawdataConverterException extends RuntimeException {
        public SiriusRawdataConverterException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
