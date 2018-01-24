package solution.exo1;

import java.util.Vector;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.CommonState;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class DensityControler implements EDProtocol{
	
	public static final String init_event="INIT";
	public static final String loop_event="LOOPEVENT";
	public static final String trig_event="TRIGEVENT";

	private static final String PAR_FIRST_TRIGGER="firstTrigger";
	private static final String PAR_PERIOD ="period";
	private static final String PAR_NEIGHBORPID ="neighborprotocol";

	private final int my_pid;
	private final int neighbor_pid;

	private int period;
	private int firstTrigger;
	private Vector<Vector<Integer>> neighborTrace;

	public DensityControler(String prefix) {
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.firstTrigger=Configuration.getInt(prefix+"."+PAR_FIRST_TRIGGER);
		this.period=Configuration.getInt(prefix+"."+PAR_PERIOD);
		//this.neighbor_pid=Configuration.getInt(prefix+"."+PAR_NEIGHBORPID, -1);
		//todo FIX THIS
		this.neighbor_pid = 3;
		initBuffers();
	}

	
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(pid != my_pid){
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		if(event instanceof String) {
			String ev = (String)event;
			if(ev.equals(loop_event)) {
				update();
				EDSimulator.add(1, loop_event, node, my_pid);
			}
			else if (ev.equals(trig_event)) {
				process_trigger();
				EDSimulator.add(period, trig_event, node, my_pid);
			}
			else if (ev.equals(init_event)) {
				EDSimulator.add(firstTrigger, loop_event, node, my_pid);
				EDSimulator.add(firstTrigger + period, trig_event, node, my_pid);
			}
			return;
		}
		System.out.println("Received unknown event: " + event);
		throw new RuntimeException("Receive unknown Event");
	}

	private void update() {
		for(int i = 0; i < Network.size(); i++) {
			Node n = Network.get(i);
			NeighborProtocolImpl npi = (NeighborProtocolImpl) n.getProtocol(neighbor_pid);
			neighborTrace.get(i).addElement(npi.getNeighbors().size());
		}
	}

	private void process_trigger() {
		Vector<Float> avgNeihbor = new Vector<Float>();
		float accAvg, accET, totAvg, totET, totED;
		totAvg = totET = totED = 0;

		//Calculation of all Di(t) and D(t)
		for(int i = 0; i < Network.size(); i++) {
			accAvg = 0;
			for(int val : neighborTrace.get(i)) {
				accAvg+= val;
			}
			accAvg /= neighborTrace.get(i).size();
			avgNeihbor.addElement(accAvg);
			totAvg+=accAvg;
		}
		totAvg /= Network.size();

		//Calculation of all Ei(t) and E(t)
		for(int i = 0; i < Network.size(); i++) {
			accET = 0;
			for(int j : neighborTrace.get(i)) {
				accET+= Math.pow(j-avgNeihbor.get(i), 2);
			}
			totET += Math.sqrt(accET/neighborTrace.get(i).size());
		}
		totET /= Network.size();

		//Calcul of DE(t)

		System.out.println("Portée | SPI | SD | D(t) | E(t)/D(t) | ED(t)/D(t)");
		System.out.println("| | | " + totAvg + " | " + totET/totAvg + " | " + totED/1);
		initBuffers();
	}

	private void initBuffers() {
		this.neighborTrace = new Vector<Vector<Integer>>();
		for(int i = 0; i < Network.size(); i++) {
			neighborTrace.addElement(new Vector<Integer>());
		}
	}



	public Object clone(){
		DensityControler res=null;
		try {
			res=(DensityControler)super.clone();
			res.period = period;
		} catch (CloneNotSupportedException e) {}
		return res;
	}
}
