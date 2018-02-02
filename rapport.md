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

	private Queue<Message> pending_probes;
	private Map<Long, Integer> received_probes;

	public NeighborProtocolImpl(String prefix){
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.probe_period=Configuration.getLong(prefix+"."+PAR_PROBE_PERIOD);
		this.timeout=Configuration.getInt(prefix+"."+PAR_TIMEOUT);
		this.emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER);
		this.neightbor_listener_pid=Configuration.getPid(prefix+"."+PAR_NEIGHBOR_LISTENER, -1);
		pending_probes = new LinkedList<>();
		received_probes = new HashMap<>();
	}

	@Override
	public List<Long> getNeighbors() {
		return new ArrayList<>(received_probes.keySet());
	}

	public Object clone(){
		NeighborProtocolImpl res=null;
		try {
			res=(NeighborProtocolImpl)super.clone();
			res.timeout = timeout;
			res.probe_period = probe_period;
			res.emitter_pid = emitter_pid;
			res.neightbor_listener_pid = neightbor_listener_pid;
			res.pending_probes = new LinkedList<>(pending_probes);
			res.received_probes = new HashMap<>(received_probes);
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

				for(int i = 0; i < Network.size(); i++) {
					Node dest = Network.get(i);
					if(dest.getID() != node.getID()) {
						Message probeMsg = new Message(node.getID(), dest.getID(), MSG_TAG_PROBE, null, my_pid);
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

				Message m = pending_probes.poll();
				if(m != null) {
					Integer nbTimeout = received_probes.get(m.getIdSrc()) - 1;
					if(nbTimeout == 0) {
						received_probes.remove(m.getIdSrc());
						if(neightbor_listener_pid != -1)
							((NeighborhoodListener)node.getProtocol(neightbor_listener_pid)).lostNeighborDetected(node, m.getIdSrc());
					}
					else if(nbTimeout > 0) {
						received_probes.put(m.getIdSrc(), nbTimeout);
					}
					else {
						System.err.println("nbTimeOut = null should not happen");
					}
				} else {
					System.err.println("pending_probes was empty during timeout, this should not happen");
				}
				return;
			}
		}
		else if(event instanceof Message) {
			Message msg = (Message) event;
			if(msg.getIdDest() == node.getID()) {
				// Réception d'un Probe
				if (msg.getTag().equals(MSG_TAG_PROBE)) {

					pending_probes.offer(msg);
					Integer nbTimeout = received_probes.get(msg.getIdSrc());
					if(nbTimeout == null) {
						received_probes.put(msg.getIdSrc(), 1);
						if (neightbor_listener_pid != -1)
							((NeighborhoodListener) node.getProtocol(neightbor_listener_pid)).newNeighborDetected(node, msg.getIdSrc());
					} else {
						received_probes.put(msg.getIdSrc(), nbTimeout + 1);
					}

					// Armer le prochain TimeOut
					EDSimulator.add(timeout, TIMEOUT_EVENT, node, my_pid);
				}
			}
			return;
		}
		System.err.println("Received unknown event: " + event);
		throw new RuntimeException("Receive unknown Event");
	}

}
```

## Question 8
- Stratégie 3
  Cette SD repose sur le paramètre scope, définissant une zone de deplacements carré centrée sur le milieu de la fenêtre.
  La taille de la zone est définie par la formule: (scope-20)*2
  Si scope < 20, la zone est de taille 0, tous les noeuds sont donc statiques au milieux de la fenêtre.
  Dans le pire des cas, les noeuds peuvent être de part et d'autre de la diagonale du carré, soit à une distance de n*\sqrt{2}, avec n etant la taille d'un coté de la zone.
  Ce qui revient à résoudre l'inégalité suivante:
  ((scope-20)*2)*\sqrt{2} < scope
  Après simplification:
  scope < ~30.93
  Le graphe est donc connexe pour un scope de 0 à 30.

- Stratégie 4
  Si le graphe est connexe à l'initialisation, deplace les noeuds dans la zone tout en conservant la propriété connexe du graphe.

- Stratégie 5
  Cette SPI garantie que la distance maximum entre deux noeuds est bornée par le mimimum entre le scope et le parametre distance_init_max. 
  La SPI génère donc un graphe connexe.

- Stratégie 6
  Cette SPI forme une topologie en "étoile" autour du centre de la fenêtre. L'angle de chaque noeud est calculé en fonction de son ID, et la distance depuis le centre est calculé en fonction du scope et de la taille du réseau, garantissant de générer un graphe connexe.

## Question 10


| portée | SPI | SD | D(t) | \frac{E(t)}{D(T)} | \frac{ED(t)}{D(T)} |
|--------|-----|----|------|-------------------|--------------------|
|     125|    1|   1|  0,97|               7,18|                0,22|
|     250|    1|   1|  3,63|               3,70|                0,10|
|     375|    1|   1|  7,66|               2,85|                0,10|
|     500|    1|   1| 13,32|               2,51|                0,08|
|     625|    1|   1| 18,51|               2,38|                0,07|
|     750|    1|   1| 25,87|               2,18|                0,07|
|     875|    1|   1| 30,55|               1,92|                0,07|
|    1000|    1|   1| 35,29|               1,60|                0,04|
|        |     |    |      |                   |                    |
|     125|    3|   3| 30,10|               1,89|                0,07|
|     250|    3|   3| 27,22|               2,05|                0,07|
|     375|    3|   3| 26,19|               2,15|                0,09|
|     500|    3|   3| 25,83|               2,16|                0,08|
|     625|    3|   3| 26,34|               2,22|                0,09|
|     750|    3|   3| 25,22|               2,12|                0,10|
|     875|    3|   3| 25,36|               2,15|                0,10|
|    1000|    3|   3| 25,59|               2,14|                0,09|

## Question 11

- Stratégie 1:
  La taille de la zone étant fixe, l'augmentation de la portée est donc directement proportionnelle avec celle de la densité.
- Stratégie 2:
  La taille de la zone de déplacement augmente proportionnellement avec le scope, la densité reste donc constante avec l'augmentation de la portée.
  
  
# exercice 2

##Question 1

/TODO
| Taille du réseau | D(t) | \frac{E(t)}{D(T)} |
|------------------|------|-------------------|
|                20|  3.18|               0.28|
|                30|  4.90|               0.38|

##Question 2