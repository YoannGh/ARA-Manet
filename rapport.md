# Exercice 1

## Question 1

Donnez l'algorithme général de déplacement d'un noeud:
```
Initialement le noeud n'est pas en mouvement.

move:
Si le noeud n'est pas en mouvement:
  - calcul aléatoire de la vitesse du noeud
  - calcul de sa prochaine destination en se basant sur la stratégie de destination

Calcul de la distance courante restante à parcourir
Calcul de la distance totale à parcourir par le noeud jusqu'à sa prochaine destination.

Si le noeud est arrivé à destination:
	il s'arrête et s'envoit un Event de type pause qui va relancer l'algorithme
	move après que le temps 'pause' soit écoulé.
Sinon:
	Le noeud se déplace vers sa destination et s'envoit un Event pour déclencher son prochain déplacement.
```

## Question 2
Contenu du fichier config.cfg pour cette question:
```
random.seed 6
network.size 42
simulation.endtime 200000

protocol.positionprotocolimpl PositionProtocolImpl
protocol.positionprotocolimpl.maxspeed 100
protocol.positionprotocolimpl.minspeed 10
protocol.positionprotocolimpl.width 1000
protocol.positionprotocolimpl.height 1000
protocol.positionprotocolimpl.pause 3000

initial_position_strategy Strategy1InitNext
initial_position_strategy.positionprotocol positionprotocolimpl
next_destination_strategy Strategy1InitNext
next_destination_strategy.positionprotocol positionprotocolimpl

control.graphicalmonitor GraphicalMonitor
control.graphicalmonitor.positionprotocol positionprotocolimpl
control.graphicalmonitor.time_slow 0.0002
control.graphicalmonitor.step 1

init.init_module Initialisation
```

## Question 3

- SD: Stratégie déplacement
- SPI: Stratégie Position Initiale

Le SD définit une position pseudo aléatoire comprise entre les paramètres width et height définis dans le fichier de config.
La SPI de la Stratégie 1 appelle simplement le SD avec une vitesse de zéro.


## Question 4

Le SD de la stratégie 2 retourne la position actuelle du noeud. Les noeuds sont donc statiques.

## Question 5

```java
public class EmitterImpl implements Emitter {

	private static final String pp_PID = "positionprotocolimpl";
	
	private static final String PAR_LATENCY="latency";
	private static final String PAR_SCOPE="scope";
	
	private final int my_pid;
	
	private int latency;
	private int scope;
	
	public EmitterImpl(String prefix){
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.latency=Configuration.getInt(prefix+"."+PAR_LATENCY);
		this.scope=Configuration.getInt(prefix+"."+PAR_SCOPE);
	}

	@Override
	public void emit(Node host, Message msg) {
		int positionprotocol_pid = Configuration.lookupPid(pp_PID);
		PositionProtocol ppEmitter = (PositionProtocol) host.getProtocol(positionprotocol_pid);
		Position positionEmitter = ppEmitter.getCurrentPosition();

		for(int i = 0; i < Network.size(); i++) {
			Node n = Network.get(i);
			if(n.getID() != host.getID()) {
				PositionProtocol ppNode = (PositionProtocol) n.getProtocol(positionprotocol_pid);
				Position positionNode = ppNode.getCurrentPosition();
				double distance = Math.hypot(	positionEmitter.getX()-positionNode.getX(),
												positionEmitter.getY()-positionNode.getY());

				if (distance <= scope) {
					EDSimulator.add(latency, msg, n, msg.getPid());
				}
			}
		}
	}

	@Override
	public int getLatency() {
		return latency;
	}

	@Override
	public int getScope() {
		return scope;
	}
	
	public Object clone(){
		EmitterImpl res=null;
		try {
			res=(EmitterImpl)super.clone();
			res.latency = latency;
			res.scope = scope;
		} catch (CloneNotSupportedException e) {}
		return res;
	}

}
```

## Question 6

```java
public class NeighborProtocolImpl implements NeighborProtocol, EDProtocol {
	
	public static final String DO_HEARTBEAT_EVENT = "do_heartbeat";
	public static final String TIMEOUT_EVENT = "timeout";

	public static final String MSG_TAG_PROBE = "probe";

	private static final String PAR_PROBE_PERIOD="probe_period";
	private static final String PAR_TIMEOUT="timeout";
	private static final String PAR_EMITTER="emitter";
	private static final String PAR_NEIGHBOR_LISTENER="neighbor_listener";
	
	private final int my_pid;
	
	private int timeout;
	private long probe_period;
	private int emitter_pid;
	private int neightbor_listener_pid;
	private int current_probe_id = 1;

	private List<Message> neighbors_msg;
	private List<Long> neighbors_ids;
	
	public NeighborProtocolImpl(String prefix){
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.probe_period=Configuration.getLong(prefix+"."+PAR_PROBE_PERIOD);
		this.timeout=Configuration.getInt(prefix+"."+PAR_TIMEOUT);
		this.emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER);
		this.neightbor_listener_pid=Configuration.getPid(prefix+"."+PAR_NEIGHBOR_LISTENER, -1);

		neighbors_msg = new LinkedList<>();
		neighbors_ids = new LinkedList<>();

	}
	
	@Override
	public List<Long> getNeighbors() {
		return neighbors_ids;
	}
	
	public Object clone(){
		NeighborProtocolImpl res=null;
		try {
			res=(NeighborProtocolImpl)super.clone();
			res.timeout = timeout;
			res.probe_period = probe_period;
			res.emitter_pid = emitter_pid;
			res.neightbor_listener_pid = neightbor_listener_pid;
			res.current_probe_id = current_probe_id;
			res.neighbors_msg = new ArrayList<>(neighbors_msg);
			res.neighbors_ids = new ArrayList<>(neighbors_ids);
		} catch (CloneNotSupportedException e) {}
		return res;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(pid != my_pid){
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		if(event instanceof String) {
			String ev = (String) event;
			if(ev.equals(DO_HEARTBEAT_EVENT)) {
				current_probe_id++;

				for(int i = 0; i < Network.size(); i++) {
					Node dest = Network.get(i);
					if(dest.getID() != node.getID()) {
						Message probeMsg = new Message(node.getID(), dest.getID(), MSG_TAG_PROBE, new Integer(current_probe_id) , my_pid);
						Emitter emitter = (Emitter) node.getProtocol(emitter_pid);
						//Envoi du Probe
						emitter.emit(node, probeMsg);
					}
				}
				//Armer l'envoi du prochain Probe
				EDSimulator.add(probe_period, DO_HEARTBEAT_EVENT, node, my_pid);
				return;
			}
			//Réception d'un évènement TimeOut
			else if (ev.equals(TIMEOUT_EVENT)) {
				for(Iterator<Message> itr = neighbors_msg.iterator(); itr.hasNext(); ) {
					Message m = itr.next();
					Integer msg_probe_id = (Integer) m.getContent();
					if(msg_probe_id != current_probe_id) {
						itr.remove();
						neighbors_ids.remove(m.getIdSrc());
						if(neightbor_listener_pid != -1)
							((NeighborhoodListener)node.getProtocol(neightbor_listener_pid)).lostNeighborDetected(node, m.getIdSrc());
					}
				}
				return;
			}
		}
		else if(event instanceof Message) {
			Message msg = (Message) event;
			if(msg.getIdDest() == node.getID()) {
				// Réception d'un Probe
				if (msg.getTag().equals(MSG_TAG_PROBE)) {
					Integer msg_probe_id = (Integer) msg.getContent();
					if (msg_probe_id == current_probe_id) {
						if (!neighbors_ids.contains(msg.getIdSrc())) {
							neighbors_msg.add(msg);
							neighbors_ids.add(msg.getIdSrc());
							if (neightbor_listener_pid != -1)
								((NeighborhoodListener) node.getProtocol(neightbor_listener_pid)).newNeighborDetected(node, msg.getIdSrc());
						}
					}
					// Armer le prochain TimeOut
					EDSimulator.add(timeout, TIMEOUT_EVENT, node, my_pid);
				}
			}
			return;
		}
		System.out.println("Received unknown event: " + event);
		throw new RuntimeException("Receive unknown Event");
	}

}
```