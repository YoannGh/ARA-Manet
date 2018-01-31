package solution.exo1;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import peersim.config.Configuration;

import peersim.core.Control;
import peersim.core.Network;
import peersim.core.CommonState;
import peersim.core.Node;


public class DensityControler implements Control{

	private static final String PAR_NEIGHBORPID ="neighborprotocol";
	private static final String np_PID = "neighborprotocolimpl";

	private final int neighbor_pid;
	private final int period;
	private final int range;
	private double totalAvgNei = 0;
	private double totalStandVar = 0;
	private ArrayList<Double> avg_trace = new ArrayList<Double>();

	public DensityControler(String prefix) {
		//this.neighbor_pid=Configuration.getInt(prefix+"."+PAR_NEIGHBORPID);
		this.neighbor_pid = Configuration.lookupPid(np_PID);
		this.period = Configuration.getInt(prefix+"."+"step");
		this.range = Configuration.getInt(prefix+"."+"range", -1);
	}

	private double avgNei() {
		double avg = 0;
		for(int i = 0 ; i< Network.size() ; i++){
			Node n = Network.get(i);
			NeighborProtocolImpl npi = (NeighborProtocolImpl) n.getProtocol(neighbor_pid);
			avg += npi.getNeighbors().size();
		}
		avg /= Network.size();
		totalAvgNei+=avg;
		avg_trace.add(avg);
		return avg;
	}

	private double avgNeiSinceStartup() {
		return totalAvgNei/avg_trace.size();
	}

	private void standardVariation() {
		double avg = totalAvgNei/avg_trace.size();
		double sv = 0;
		for(int i = 0 ; i< Network.size() ; i++){
			Node n = Network.get(i);
			NeighborProtocolImpl npi = (NeighborProtocolImpl) n.getProtocol(neighbor_pid);
			sv += Math.pow((npi.getNeighbors().size() - avg), 2);
		}
		totalStandVar += Math.sqrt(sv);
	}

	private double avgStandVarSinceStartup() {
		return totalStandVar/avg_trace.size();
	}

	private double ED(){
		double value = 0;
		for(double i : avg_trace){
			value += Math.pow(i-avgNeiSinceStartup(),2);
		}
		value =value/avg_trace.size();
		return Math.sqrt(value);
	}

	@Override
	public boolean execute() {
		avgNei();
		standardVariation();
		NumberFormat formatter = new DecimalFormat("#0.00");
		
		System.out.println(formatter.format((double)CommonState.getTime() * 100 / CommonState.getEndTime()) + "%");

		if(CommonState.getTime() >= (CommonState.getEndTime() - period)) {
			double dt = avgNeiSinceStartup();
			System.out.println("D(t): " + formatter.format(dt));
			System.out.println("E(t)/D(t): " + formatter.format((avgStandVarSinceStartup() / dt)));
			System.out.println("ED(t)/D(t): " + formatter.format((ED() / dt)));
//			System.out.println("|     " + range + "|    3|   3| " + formatter.format(dt) + "|               " +
//								formatter.format((avgStandVarSinceStartup() / dt))	+ "|                " +
//								formatter.format((ED() / dt)) + "|");
			System.out.println("|                " + range + "|  " + formatter.format(dt) + "|               " + formatter.format((ED() / dt)) + "|");
		}
		return false;
	}

	public Object clone(){
		DensityControler res=null;
		try {
			res=(DensityControler)super.clone();
		} catch (CloneNotSupportedException e) {}
		return res;
	}
}
