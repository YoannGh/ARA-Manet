package solution.exo2;

import manet.Message;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import solution.exo1.NeighborProtocolImpl;

public class EmitterDistance extends EmitterGossip{
	private static final String PAR_NEIGHBORPID ="neighborprotocol";

	private static int neighbor_pid;

	public EmitterDistance(String prefix) {
		super(prefix);
		//EmitterDistance.neighbor_pid=Configuration.getPid(prefix+"."+PAR_NEIGHBORPID);
	}

	//Probability algorithm send according to actual density
	public void emit(Node host, Message msg) {
		super.emit(host, msg, true);
	}
}
