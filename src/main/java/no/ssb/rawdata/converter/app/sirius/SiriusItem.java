package no.ssb.rawdata.converter.app.sirius;

import de.huxhorn.sulky.ulid.ULID;
import lombok.Builder;
import lombok.Value;
import no.ssb.rawdata.api.RawdataMessage;

import java.util.TreeSet;

@Builder
@Value
public class SiriusItem {
    private final ULID.Value ulid;
    private final String position;
    private byte[] skattemeldingXml;

    public static SiriusItem from(RawdataMessage rawdataMessage) {
        TreeSet<String> keys = new TreeSet(rawdataMessage.keys());

        String skattemeldingDocKey = keys.ceiling("skattemelding");

        return SiriusItem.builder()
          .ulid(rawdataMessage.ulid())
          .position(rawdataMessage.position())
          .skattemeldingXml(rawdataMessage.get(skattemeldingDocKey))
          .build();
    }

    public boolean hasSkattemelding() {
        return skattemeldingXml != null;
    }

    public String toIdString() {
        return String.format("%s (pos=%s)", ulid, position);
    }
}
