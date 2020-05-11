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
import no.ssb.rawdata.converter.core.RawdataMessageException;
import no.ssb.rawdata.converter.core.pseudo.PseudoService;
import no.ssb.rawdata.converter.core.util.RawdataMessageUtil;
import no.ssb.rawdata.converter.core.util.Xml;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;

import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Singleton
@Slf4j
public class SiriusRawdataConverter extends AbstractRawdataConverter {
    private final Schema skattemeldingSchema;
    private final Schema hendelseSchema;
    private final Schema aggregateSchema;
    private final PseudoService pseudoService;
    private final SiriusRawdataConverterConfig converterConfig;

    private static final String RAWDATA_ENTRY_SKATTEMELDING = "skattemelding";
    private static final String RAWDATA_ENTRY_HENDELSE = "entry";

    private static final String ELEMENT_NAME_METADATA = "metadata";
    private static final String ELEMENT_NAME_MANIFEST = "dcManifest";
    private static final String ELEMENT_NAME_SIRIUS_HENDELSE = "hendelse";
    private static final String ELEMENT_NAME_SIRIUS_SKATTEMELDING = "skattemeldingUtflatet";

    public SiriusRawdataConverter(SiriusRawdataConverterConfig converterConfig, @NonNull PseudoService pseudoService) {
        this.converterConfig = converterConfig;
        skattemeldingSchema = readAvroSchema(converterConfig.getSchemaFileSkattemelding());
        hendelseSchema = readAvroSchema(converterConfig.getSchemaFileHendelse());
        aggregateSchema = new AggregateSchemaBuilder("no.ssb.dapla")
                .schema(ELEMENT_NAME_METADATA, Metadata.SCHEMA)
                .schema(ELEMENT_NAME_MANIFEST, DataCollectorManifest.SCHEMA)
                .schema(ELEMENT_NAME_SIRIUS_HENDELSE, hendelseSchema)
                .schema(ELEMENT_NAME_SIRIUS_SKATTEMELDING, skattemeldingSchema)
                .build();
        this.pseudoService = pseudoService;

        log.info("Converter config:\n{}", converterConfig.toDebugString());
    }

    @Override
    public Schema targetAvroSchema() {
        return aggregateSchema;
    }

    @Override
    public boolean isConvertible(RawdataMessage msg) {
        try {
            RawdataMessageUtil.assertKeysPresent(msg, RAWDATA_ENTRY_HENDELSE, RAWDATA_ENTRY_SKATTEMELDING);
        }
        catch (RawdataMessageException e) {
            log.warn(e.getMessage());
            return false;
        }

        String hendelseXml = new String(msg.get(RAWDATA_ENTRY_HENDELSE));
        String skattemeldingXml = new String(msg.get(RAWDATA_ENTRY_SKATTEMELDING));
        Map<String, Object> hendelse = Xml.toGenericMap(hendelseXml);
        Map<String, Object> skattemelding = Xml.toGenericMap(skattemeldingXml);
        String gjelderPeriode = (String) hendelse.get("gjelderPeriode");
        boolean skjermet = "true".equals(skattemelding.get("skjermet"));

        // TODO: Keep skjermet records
        if (skjermet) {
            log.warn("Encountered skjermet skattemelding. Will be skipped for now due to schema validation issues.");
        }
        else if (converterConfig.getGjelderPeriode().equals(gjelderPeriode)) {
            return true;
        }

        log.info("Skipping RawdataMessage with ulid={}, gjelderPeriode={}", msg.ulid(), gjelderPeriode);
        return false;
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

        if (siriusItem.hasHendelse() && siriusItem.getHendelseXml().length > 0) {
            try {
                xmlToAvro(siriusItem.getHendelseXml(), ELEMENT_NAME_SIRIUS_HENDELSE, hendelseSchema, resultBuilder);
            } catch (Exception e) {
                resultBuilder.addFailure(e);
                log.warn(String.format("Failed to convert hendelse xml %s - size=%d bytes", siriusItem.toIdString(), siriusItem.getHendelseXml().length), e);
            }
        } else {
            log.warn("Missing hendelse data {} - size={} bytes", siriusItem.toIdString(), siriusItem.getHendelseXml().length);
        }

        if (siriusItem.hasSkattemelding() && siriusItem.getSkattemeldingXml().length > 0) {
            try {
                xmlToAvro(siriusItem.getSkattemeldingXml(), ELEMENT_NAME_SIRIUS_SKATTEMELDING, skattemeldingSchema, resultBuilder);
            } catch (Exception e) {
                resultBuilder.addFailure(e);
                log.warn(String.format("Failed to convert skattemelding xml %s - size=%d bytes", siriusItem.toIdString(), siriusItem.getSkattemeldingXml().length), e);
            }
        } else {
            log.warn("Missing skattemelding data {} - size={} bytes", siriusItem.toIdString(), siriusItem.getSkattemeldingXml().length);
        }

        return resultBuilder.build();
    }

    // TODO: Split up this code. Gotcha: try with resources will close the stream
    void xmlToAvro(byte[] xmlData, String rootXmlElementName, Schema schema, ConversionResultBuilder resultBuilder) {
        InputStream xmlInputStream = new ByteArrayInputStream(xmlData);

        try (XmlToRecords xmlToRecords = new XmlToRecords(xmlInputStream, rootXmlElementName, schema, pseudoService::pseudonymize)) {
            xmlToRecords.forEach(record ->
                    resultBuilder.withRecord(rootXmlElementName, record)
            );
        } catch (XMLStreamException | IOException e) {
            throw new SiriusRawdataConverterException("Error converting Sirius XML (" + rootXmlElementName + ")", e);
        }
    }

    static class SiriusRawdataConverterException extends RuntimeException {
        public SiriusRawdataConverterException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
