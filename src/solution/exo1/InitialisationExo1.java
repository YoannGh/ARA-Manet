package solution.exo1;

import manet.detection.NeighborProtocol;
import manet.positioning.PositionProtocol;
import manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class InitialisationExo1 implements Control {

	private static final String pp_PID = "positionprotocolimpl";
	private static final String np_PID = "neighborprotocolimpl";

	public InitialisationExo1(String prefix) {}

	@Override
	public boolean execute() {
		int positionprotocol_pid = Configuration.lookupPid(pp_PID);
		int neighborprotocol_pid = Configuration.lookupPid(np_PID);
		for(int i = 0; i < Network.size(); i++) {
			Node n = Network.get(i);
			PositionProtocol pp = (PositionProtocol) n.getProtocol(positionprotocol_pid);
			NeighborProtocol np = (NeighborProtocol) n.getProtocol(neighborprotocol_pid);
			pp.initialiseCurrentPosition(n);
			//Démarrer le déplacement des noeuds
			EDSimulator.add(1, PositionProtocolImpl.loop_event, n, positionprotocol_pid);
			//Démarrer l'envoi des Messages Probe
			EDSimulator.add(1, NeighborProtocolImpl.DO_HEARTBEAT_EVENT, n, neighborprotocol_pid);
		}
		return false;
	}
}