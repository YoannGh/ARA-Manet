package solution.exo1;

import manet.positioning.PositionProtocol;
import manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class Initialisation implements Control {

	private static final String pp_PID = "positionprotocolimpl";
	private static final String np_PID = "neighborprotocolimpl";

	public  Initialisation(String prefix) {}

	@Override
	public boolean execute() {
		int positionprotocol_pid = Configuration.lookupPid(pp_PID);

		for(int i = 0; i < Network.size(); i++) {
			Node n = Network.get(i);
			PositionProtocol pp = (PositionProtocolImpl) n.getProtocol(positionprotocol_pid);
			pp.initialiseCurrentPosition(n);
		}
		return false;
	}
}