package solution.exo1;

import manet.detection.NeighborhoodListener;
import peersim.config.Configuration;
import peersim.core.Node;

public class NeighborhoodListenerImpl implements NeighborhoodListener {
	
	private static final String PAR_NEIGHBORPID = "neighborprotocol";
	
	private final int my_pid;
	private final int neighbor_pid;

	public NeighborhoodListenerImpl(String prefix) {
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		neighbor_pid= Configuration.getPid(prefix+"."+PAR_NEIGHBORPID, -1);
	}
	
	public void newNeighborDetected(Node host, long id_new_neigbor) {
		//System.out.println("New Neighbor !");
	}
	
	public void lostNeighborDetected(Node host, long id_lost_neigbor) {
		//System.out.println("Lost Neighbor !");		
	}

	public Object clone(){
		NeighborhoodListenerImpl res=null;
		try {
			res=(NeighborhoodListenerImpl)super.clone();
		} catch (CloneNotSupportedException e) {}
		return res;
	}
}
