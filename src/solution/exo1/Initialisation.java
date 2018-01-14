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
		int neighborprotocol_pid = Configuration.lookupPid(np_PID);
		for(int i = 0; i < Network.size(); i++) {
			Node dest = Network.get(i);
			PositionProtocol pp = (PositionProtocolImpl) dest.getProtocol(positionprotocol_pid);
			pp.initialiseCurrentPosition(dest);
			//Démarrer le déplacement des noeuds
			EDSimulator.add(1, PositionProtocolImpl.loop_event, dest, positionprotocol_pid);
			//Démarrer l'envoi des Messages Probe
			EDSimulator.add(1, NeighborProtocolImpl.DO_HEARTBEAT_EVENT, dest, neighborprotocol_pid);
			//Démarrer les TimeOut
			EDSimulator.add(1, NeighborProtocolImpl.TIMEOUT_EVENT, dest, neighborprotocol_pid);
		}
		return false;
	}
}