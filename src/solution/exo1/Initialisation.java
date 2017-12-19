package solution.exo1;

import manet.positioning.PositionProtocol;
import manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class Initialisation implements Control {
	
	private static final String PID = "positionprotocolimpl";
	
	public  Initialisation(String prefix) {}
	
	@Override
	public boolean execute() {
		int positionprotocol_pid = Configuration.lookupPid(PID);
		for(int i = 0; i < Network.size(); i++) {
			Node src = Network.get(i);
			PositionProtocol pp = (PositionProtocolImpl) src.getProtocol(positionprotocol_pid);
			pp.initialiseCurrentPosition(src);
			EDSimulator.add(1, PositionProtocolImpl.loop_event, src, positionprotocol_pid);
		}
		return false;
	}
}
