package no.ssb.rawdata.converter.app.sirius;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.ssb.avro.convert.xml.XmlToRecords;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.rawdata.converter.core.AbstractRawdataConverter;
import no.ssb.rawdata.converter.core.AggregateSchemaBuilder;
import no.ssb.rawdata.converter.core.ConversionResult;
import no.ssb.rawdata.converter.core.ConversionResult.ConversionResultBuilder;
import no.ssb.rawdata.converter.core.DataCollectorManifest;
import no.ssb.rawdata.converter.core.Metadata;
import no.ssb.rawdata.converter.core.MetadataGenericRecordBuilder;
import no.ssb.rawdata.converter.core.pseudo.PseudoService;
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
    private final Schema hendelseSchema;
    private final Schema aggregateSchema;
    private final PseudoService pseudoService;

    private static final String ELEMENT_NAME_METADATA = "metadata";
    private static final String ELEMENT_NAME_MANIFEST = "dcManifest";
    private static final String ELEMENT_NAME_SIRIUS_HENDELSE = "hendelse";
    private static final String ELEMENT_NAME_SIRIUS_SKATTEMELDING = "skattemeldingUtflatet";

    public SiriusRawdataConverter(SiriusRawdataConverterConfig siriusConverterConfig, @NonNull PseudoService pseudoService) {
        skattemeldingSchema = readAvroSchema(siriusConverterConfig.getSchemaFileSkattemeldingUtflatet());
        hendelseSchema = readAvroSchema(siriusConverterConfig.getSchemaFileHendelse());
        aggregateSchema = new AggregateSchemaBuilder("no.ssb.dapla")
                .schema(ELEMENT_NAME_METADATA, Metadata.SCHEMA)
                .schema(ELEMENT_NAME_MANIFEST, DataCollectorManifest.SCHEMA)
                .schema(ELEMENT_NAME_SIRIUS_HENDELSE, hendelseSchema)
                .schema(ELEMENT_NAME_SIRIUS_SKATTEMELDING, skattemeldingSchema)
                .build();

        this.pseudoService = pseudoService;
    }

    @Override
    public Schema targetAvroSchema() {
        return aggregateSchema;
    }

    @Override
    public ConversionResult convert(RawdataMessage rawdataMessage) {
        log.trace("convert sirius rawdata message {}", rawdataMessage);
        ConversionResultBuilder resultBuilder = new ConversionResultBuilder(new GenericRecordBuilder(aggregateSchema));

        SiriusItem siriusItem = SiriusItem.from(rawdataMessage);

        if (siriusItem.hasManifestJson()) {
            GenericRecord metadataRecord = MetadataGenericRecordBuilder
              .fromRawdataManifest(siriusItem.getManifestJson())
              .withULID(siriusItem.getUlid())
              .build();
            resultBuilder.withRecord(ELEMENT_NAME_METADATA, metadataRecord);
            resultBuilder.withRecord(ELEMENT_NAME_MANIFEST, DataCollectorManifest.toGenericRecord(siriusItem.getManifestJson()));

        } else {
            log.warn("Missing metadata for sirius item {}.", siriusItem.toIdString());
        }

        if (siriusItem.hasHendelse()) {
            xmlToAvro(siriusItem.getHendelseXml(), ELEMENT_NAME_SIRIUS_HENDELSE, hendelseSchema, resultBuilder);
        } else {
            log.warn("Missing hendelse data for sirius item {}.", siriusItem.toIdString());
        }

        if (siriusItem.hasSkattemelding()) {
            try {
                xmlToAvro(siriusItem.getSkattemeldingXml(), ELEMENT_NAME_SIRIUS_SKATTEMELDING, skattemeldingSchema, resultBuilder);
            } catch (Exception e) {
                resultBuilder.addFailure(e);
                log.warn("Failed to convert skattemelding xml", e);
            }

        } else {
            log.warn("Missing skattemelding data for sirius item {}", siriusItem.toIdString());
        }

        // TODO: Error handling. Skip this item without breaking the conversion stream completely
        return resultBuilder.build();
    }

    // TODO: Split up this code. Gotcha: try with resources will close the stream
    void xmlToAvro(byte[] xmlData, String rootXmlElementName, Schema schema, ConversionResultBuilder resultBuilder) {
        InputStream xmlInputStream = new ByteArrayInputStream(xmlData);

        try (XmlToRecords xmlToRecords = new XmlToRecords(xmlInputStream, rootXmlElementName, schema, pseudoService::pseudonyimze)) {
            xmlToRecords.forEach(record ->
                    resultBuilder.withRecord(rootXmlElementName, record)
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
