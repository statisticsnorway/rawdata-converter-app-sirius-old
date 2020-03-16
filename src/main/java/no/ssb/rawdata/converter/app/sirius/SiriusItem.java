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
    private byte[] manifestJson;
    private byte[] hendelseXml;
    private byte[] skattemeldingXml;

    public static SiriusItem from(RawdataMessage rawdataMessage) {
        return SiriusItem.builder()
          .ulid(rawdataMessage.ulid())
          .position(rawdataMessage.position())
          .manifestJson(rawdataMessage.get("manifest.json"))
          .hendelseXml(rawdataMessage.get("entry"))
          .skattemeldingXml(rawdataMessage.get("skattemelding"))
          .build();
    }

    public boolean hasManifestJson() {
        return manifestJson != null;
    }

    public String getManifestJsonAsString() {
        return hasManifestJson() ? new String(manifestJson) : null;
    }

    public boolean hasSkattemelding() {
        return skattemeldingXml != null;
    }

    public String getSkattemeldingXmlAsString() {
        return hasSkattemelding() ? new String(skattemeldingXml) : null;
    }

    public boolean hasHendelse() {
        return hendelseXml != null;
    }

    public String getHendelseXmlAsString() {
        return hasHendelse() ? new String(hendelseXml) : null;
    }

    public String toIdString() {
        return String.format("%s (pos=%s)", ulid, position);
    }
}
