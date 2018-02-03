package solution.exo1;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import peersim.config.Configuration;

import peersim.core.Control;
import peersim.core.Network;
import peersim.core.CommonState;
import peersim.core.Node;


public class DensityControler implements Control{

	private static final String PAR_NEIGHBORPID ="neighborprotocol";

	private int neighbor_pid;
	private int period;
	private int range;
	private double totalAvgNei = 0;
	private double totalStandVar = 0;
	private List<Double> avg_trace = new ArrayList<Double>();

	public DensityControler(String prefix) {
		this.neighbor_pid=Configuration.getPid(prefix+"."+PAR_NEIGHBORPID);
		this.period = Configuration.getInt(prefix+"."+"step");
		this.range = Configuration.getInt(prefix+"."+"range", -1);
	}

	// Di(t)
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

	// Ei(t)
	private void standardVariation() {
		double avg = avg_trace.get(avg_trace.size()-1)/avg_trace.size();
		double sv = 0;
		for(int i = 0 ; i< Network.size() ; i++){
			Node n = Network.get(i);
			NeighborProtocolImpl npi = (NeighborProtocolImpl) n.getProtocol(neighbor_pid);
			sv += Math.pow((npi.getNeighbors().size() - avg), 2);
		}
		sv = sv/Network.size();
		totalStandVar += Math.sqrt(sv);
	}

	// D(t)
	private double avgNeiSinceStartup() {
		return totalAvgNei/avg_trace.size();
	}

	// E(t)
	private double avgStandVarSinceStartup() {
		return totalStandVar/avg_trace.size();
	}

	// ED(t)
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

		// A la dernière exécution du DensityControler on affiche les résultats calculés
		if(CommonState.getTime() >= (CommonState.getEndTime() - period)) {
			double dt = avgNeiSinceStartup();
			System.out.println("Portée: " + range);
			System.out.println("D(t): " + formatter.format(dt));
			System.out.println("E(t)/D(t): " + formatter.format((avgStandVarSinceStartup() / dt)));
			System.out.println("ED(t)/D(t): " + formatter.format((ED() / dt)));
		}
		return false;
	}

	public Object clone(){
		DensityControler res=null;
		try {
			res=(DensityControler)super.clone();
			res.neighbor_pid = neighbor_pid;
			res.period = period;
			res.range = range;
			res.totalAvgNei = totalAvgNei;
			res.totalStandVar = totalStandVar;
			res.avg_trace = new ArrayList<>(avg_trace);
		} catch (CloneNotSupportedException e) {}
		return res;
	}
}
