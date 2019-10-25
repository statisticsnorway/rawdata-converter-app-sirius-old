package no.ssb.rawdata.converter.app.sirius;

import de.huxhorn.sulky.ulid.ULID;
import lombok.Builder;
import lombok.Value;
import no.ssb.rawdata.api.RawdataMessage;

@Builder
@Value
public class SiriusItem {
    private final ULID.Value ulid;
    private final String position;
    private byte[] skattemeldingXml;
    private byte[] metadataJson;

    public static SiriusItem from(RawdataMessage rawdataMessage) {
        return SiriusItem.builder()
          .ulid(rawdataMessage.ulid())
          .position(rawdataMessage.position())
          .skattemeldingXml(rawdataMessage.get("skattemelding"))
          .metadataJson(rawdataMessage.get("manifest.json"))
          .build();
    }
    public boolean hasMetadata() {
        return metadataJson != null;
    }

    public boolean hasSkattemelding() {
        return skattemeldingXml != null;
    }

    public String toIdString() {
        return String.format("%s (pos=%s)", ulid, position);
    }
}
