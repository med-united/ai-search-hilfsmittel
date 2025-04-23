package de.service.health.hilfsmittel.server.equipment;

import de.service.health.hilfsmittel.xsd.HMVPRODUKTCtp;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "root")
@XmlAccessorType(XmlAccessType.FIELD)
public class Root {
    @XmlElement(name = "HMV_PRODUKT", namespace = "GI4X:/xml-schema/ESOL-HMV/1.0")
    private HMVPRODUKTCtp hmvProdukt;
}
