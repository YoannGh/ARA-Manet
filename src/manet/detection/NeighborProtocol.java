package manet.detection;

import java.util.List;

import peersim.core.Protocol;
import solution.exo1.EmitterImpl;

public interface NeighborProtocol extends Protocol {

	
	/*Renvoie la liste courante des Id des voisins directs*/
	public List<Long> getNeighbors();
	
	
}
