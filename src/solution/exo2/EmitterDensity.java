package solution.exo2;

import manet.Message;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import solution.exo1.NeighborProtocolImpl;

public class EmitterDensity extends EmitterGossip{

	private static final String PAR_K = "k";
	private static final String PAR_NEIGHBORPID ="neighborprotocol";

	private static final String MSG_TAG_GOSSIP = "gossip";

	private static double k;
	private static int neighbor_pid;

	public EmitterDensity(String prefix) {
		super(prefix);
		EmitterDensity.k=Configuration.getDouble(prefix+"."+PAR_K);
		EmitterDensity.neighbor_pid=Configuration.getPid(prefix+"."+PAR_NEIGHBORPID);
	}

	//Probability algorithm send according to actual density
	public void emit(Node host, Message msg) {
		if(msg.getTag() == MSG_TAG_GOSSIP) {
			int V;
			boolean p;
			NeighborProtocolImpl npi = (NeighborProtocolImpl) host.getProtocol(neighbor_pid);
			V = npi.getNeighbors().size();
			//p = CommonState.r.nextDouble() < k*V;
			p = (CommonState.r.nextDouble() < k/V) ? true:false;
			super.emit(host, msg, p);		
		}
		else {
			super.emit(host, msg);
		}		
	}
}