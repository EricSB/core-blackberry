package blackberry.action.sync.transport;

import blackberry.debug.Debug;
import blackberry.debug.DebugLevel;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.system.RadioInfo;

public class GprsTransport extends HttpTransport {

    //#ifdef DEBUG
    private static Debug debug = new Debug("GprsTransport", DebugLevel.VERBOSE);
    //#endif
    
    public GprsTransport(String host) {
        super(host);
    }

    public boolean isAvailable() {
        boolean gprs = (RadioInfo.getNetworkService() & RadioInfo.NETWORK_SERVICE_DATA) > 0;
        return gprs;
    }

    protected String getSuffix() {
        return ";deviceside=true";

    }
    
    public String toString() {
        return "GprsTransport " + host ;
    }
}
