package de.tu_darmstadt;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * Created by Stathis on 6/20/17.
 */

@XmlRootElement(name = "servers")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerStore {

    @XmlElement(name = "server")
    private ArrayList<ShareHolder> servers;

    public ArrayList<ShareHolder> getServers() {
        return servers;
    }

    public void setServers(ArrayList<ShareHolder> servers) {
        this.servers = servers;
    }
}
