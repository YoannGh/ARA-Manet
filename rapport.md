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

	protected static final String pp_PID = "positionprotocolimpl";
	
	private static final String PAR_LATENCY="latency";
	private static final String PAR_SCOPE="scope";
	
	private final int my_pid;
	
	protected int latency;
	protected int scope;
	
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
			Node dest = Network.get(i);
			if (dest.getID() != host.getID()) {
				PositionProtocol ppDest = (PositionProtocol) dest.getProtocol(positionprotocol_pid);
				Position positionDest = ppDest.getCurrentPosition();
				double distance = Math.hypot(	positionEmitter.getX()-positionDest.getX(),
						positionEmitter.getY()-positionDest.getY());

				if (distance <= scope) {
					EDSimulator.add(latency, msg, dest, msg.getPid());
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

				Message probeMsg = new Message(node.getID(), Emitter.ALL, MSG_TAG_PROBE, null, my_pid);
				EmitterImpl emitter = (EmitterImpl) node.getProtocol(emitter_pid);
				//Envoi du Probe
				emitter.emit(node, probeMsg);
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
|     125|    1|   1|  1,00|               1,35|                0,18|
|     250|    1|   1|  3,87|               1,11|                0,12|
|     375|    1|   1|  8,01|               1,05|                0,09|
|     500|    1|   1| 12,99|               1,03|                0,10|
|     625|    1|   1| 19,09|               1,03|                0,09|
|     750|    1|   1| 24,84|               1,02|                0,08|
|     875|    1|   1| 30,12|               1,01|                0,07|
|    1000|    1|   1| 35,71|               0,99|                0,06|
|        |     |    |      |                   |                    |
|     125|    3|   3| 30,19|               1,01|                0,08|
|     250|    3|   3| 27,21|               1,01|                0,08|
|     375|    3|   3| 26,06|               1,01|                0,10|
|     500|    3|   3| 25,70|               1,01|                0,09|
|     625|    3|   3| 25,06|               1,02|                0,08|
|     750|    3|   3| 25,36|               1,02|                0,07|
|     875|    3|   3| 25,46|               1,02|                0,07|
|    1000|    3|   3| 25,19|               1,02|                0,08|

## Question 11

- Stratégie 1:
  La taille de la zone étant fixe, l'augmentation de la portée est donc directement proportionnelle avec celle de la densité.
- Stratégie 3:
  La taille de la zone de déplacement augmente proportionnellement avec le scope, la densité reste donc constante avec l'augmentation de la portée.
  
  
# exercice 2

## Question 1


| Taille du réseau | D(t)  | \frac{ED(t)}{D(T)} |
|------------------|-------|-------------------|
|                20|  9,62 |               0,24|
|                30|  14,18|               0,21|
|                40|  21,42|               0,10|
|                50|  25,77|               0,06|
|                60|  28,55|               0,09|
|                70|  34,47|               0,06|
|                80|  40,89|               0,05|
|                90|  46,37|               0,04|
|               100|  48,03|               0,04|
|               120|  55,20|               0,03|
|               140|  64,63|               0,02|
|               160|  68,56|               0,04|
|               180|  76,97|               0,02|
|               200|  81,44|               0,02|

Sans surprise avec une taille du terrain constante, la densité du réseau augmente avec le nombre de noeuds. De même, la variation de la densité diminue avec l'augmentation de la taille du réseau.
On peut conclure qu'a partir d'un réseau de taille 50, la variation du nombre de voisins reste à peut près constante durant toute la durée d'éxecution du programme.

## Question 2

Pour detecter la terminaison d'un gossip, il suffit de maintenir deux variables représentant le nombre de messages reçus et le nombre de messages envoyés (de type gossip). Lorsque ces deux variable sont identiques, c'est que tous les messages envoyés on été reçu, et qu'il n'y a donc plus de messages en cours de transmition. Le gossip est donc terminé, on peut alors lancer le suivant.

## Question 4

L'atteignabilité est de 100% quelquesoit la taille du réseau, ce résultat est logique vu que l'algorithme de flooding retransmet les messages dans tous les cas et le réseau est connexe durant toute l'execution du programme(garantie par SPI_5 et SD_4) 

## Question 5

Pour chaque test, l'economie de redifusion correspond à la valeur de probabilité donnée en entrée. 
Pour les valeures de p supérieures à 0.5, la taille du réseau ne semble pas avoir d'impacts sur les résultats, en revanche, pour un p inférieurs, on note une nette amélioration de l'atteignabilité lorsque le réseau dépasse 50, taille a partir de laquelle le nombre de voisins est constant, comme démontré à la question 1.


## Question 6

Afin de maximiser à la fois Att et Er, il semble logique que la probabilité doit être inversement proportionelle à la taille du voisinage, en effet, si la densité est importante, la probabilité qu'un voisin ai reçu le message en meme temps que moi est plus élevée. On remarque sur les graphiques que les meilleurs résultats sont dans la fourchette d'une taille de réseau de 40-70 noeuds. Pour des réseaux plus petits, l'atteignabilité est trop faible, due au fait qu'il n'y ai pas assez de voisins, et pour les reseaux plus grand la redifusion est trop élévée, du à la densité trop élévée.

Évolution de l'atteignabilité en fonction de la densité du réseau et d'une probabilité inversement
 proportionnelle à la densité.

## Question 7

Comparé aux algorithmes précedents, c'est pour le moment la solution qui semble la plus optimale et qui offre la meilleure maximisation à la fois de l'atteignabilité et de l'économie de rediffusion, et ce quelquesoit la taille du réseau. Avec un att moyen proche de 100% pour toute taille de réseau, et un er aux alentours de 17%.