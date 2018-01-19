package solution.exo1;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class DensityControler implements EDProtocol{
	
	public static final String loop_event="LOOPEVENT";
	public static final String trig_event="TRIGEVENT";
	
	private static final String PAR_TRIGGER ="trigger";
	private static final String PAR_NEIGHBORPID ="neighborprotocol";
	
	private final int my_pid;
	private final int neighbor_pid;
	private int trigger;
	
	public DensityControler(String prefix) {
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.trigger=Configuration.getInt(prefix+"."+PAR_TRIGGER);
		this.neighbor_pid=Configuration.getInt(prefix+"."+PAR_NEIGHBORPID,-1);
	}
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(pid != my_pid){
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		//Trigger itself if not done
		if(trigger > 0) {
			EDSimulator.add(trigger, trig_event, node, my_pid);
			trigger = 0;
		}
		if(event instanceof String) {
			String ev = (String)event;
			if(ev.equals(loop_event)) {
				calculate();
				EDSimulator.add(1, loop_event, node, my_pid);
			}
			else if (ev.equals(trig_event)) {
				display_stats(node);
			}
			return;
		}
		System.out.println("Received unknown event: " + event);
		throw new RuntimeException("Receive unknown Event");
	}
	
	private void calculate() {
	}

	private void display_stats(Node host) {
		for(int i = 0; i < Network.size(); i++) {
			Node n = Network.get(i);
			//NeighborProtocolImpl npi = (NeighborProtocolImpl) n.getProtocol(neighbor_pid);
			//int nb = npi.getNeighbors().size();
			//System.out.println("Node[" + i + "] have " + nb + " neighbors at time" + trigger);
		}
	}

	public Object clone(){
		DensityControler res=null;
		try {
			res=(DensityControler)super.clone();
			res.trigger = trigger;
		} catch (CloneNotSupportedException e) {}
		return res;
	}
}
