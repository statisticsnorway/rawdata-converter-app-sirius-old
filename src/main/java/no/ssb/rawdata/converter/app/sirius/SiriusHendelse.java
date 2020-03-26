package no.ssb.rawdata.converter.app.sirius;

import lombok.Data;
import no.ssb.rawdata.converter.core.util.Xml;

@Data
public class SiriusHendelse {

    private String sekvensnummer;
    private String identifikator;
    private String gjelderPeriode;
    private String registreringstidspunkt;
    private String hendelsetype;

    public static SiriusHendelse parseXml(String xml) {
        return Xml.toObject(SiriusHendelse.class, xml);
    }
}
