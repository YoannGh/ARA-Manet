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

Évolution de l'atteignabilité et de l'économie de rediffusion en fonction de la densité du réseau 
en utilisant un algorithme de flooding.

| Taille du réseau | Att500 Moyen (%) | ER500 Moyen (%) | Ecart-type Att | Ecart-type ER |
|------------------|------------------|-----------------|----------------|---------------|
|                20|             100,0|              0,0|               0|              0|
|                30|             100,0|              0,0|               0|              0|
|                40|             100,0|              0,0|               0|              0|
|                50|             100,0|              0,0|               0|              0|
|                60|             100,0|              0,0|               0|              0|
|                70|             100,0|              0,0|               0|              0|
|                80|             100,0|              0,0|               0|              0|
|                90|             100,0|              0,0|               0|              0|
|               100|             100,0|              0,0|               0|              0|
|               120|             100,0|              0,0|               0|              0|
|               140|             100,0|              0,0|               0|              0|
|               160|             100,0|              0,0|               0|              0|
|               180|             100,0|              0,0|               0|              0|
|               200|             100,0|              0,0|               0|              0|

L'atteignabilité est de 100% quelquesoit la taille du réseau, ce résultat est logique vu que l'algorithme de flooding retransmet les messages dans tous les cas et le réseau est connexe durant toute l'execution du programme(garantie par SPI_5 et SD_4) 

## Question 5

Pour chaque test, l'economie de redifusion correspond à la valeur de probabilité donnée en entrée. 
Pour les valeures de p supérieures à 0.5, la taille du réseau ne semble pas avoir d'impacts sur les résultats, en revanche, pour un p inférieurs, on note une nette amélioration de l'atteignabilité lorsque le réseau dépasse 50, taille a partir de laquelle le nombre de voisins est constant, comme démontré à la question 1.

Évolution de l'atteignabilité en fonction de la densité du réseau en utilisant 
un algorithme probabiliste.

| Taille du réseau | Probabilité p | Att500 Moyen (%) |
|------------------|---------------|------------------|
|                20|            0.2|                33|
|                30|            0.2|                73|
|                40|            0.2|                32|
|                50|            0.2|                81|
|                60|            0.2|                79|
|                70|            0.2|                85|
|                80|            0.2|                80|
|                90|            0.2|                79|
|               100|            0.2|                74|
|               120|            0.2|                84|
|               140|            0.2|                94|
|               160|            0.2|                71|
|               180|            0.2|                72|
|               200|            0.2|                75|




|                20|            0.3|                54|
|                30|            0.3|                90|
|                40|            0.3|                46|
|                50|            0.3|                96|
|                60|            0.3|                95|
|                70|            0.3|                97|
|                80|            0.3|                97|
|                90|            0.3|                94|
|               100|            0.3|                87|
|               120|            0.3|                97|
|               140|            0.3|                99|
|               160|            0.3|                90|
|               180|            0.3|                90|
|               200|            0.3|                91|
|                20|            0.4|                75|
|                30|            0.4|                97|
|                40|            0.4|                52|
|                50|            0.4|                99|
|                60|            0.4|                98|
|                70|            0.4|               100|
|                80|            0.4|                99|
|                90|            0.4|                99|
|               100|            0.4|                98|
|               120|            0.4|                99|
|               140|            0.4|               100|
|               160|            0.4|                98|
|               180|            0.4|                97|
|               200|            0.4|                97|
|                20|            0.5|                91|
|                30|            0.5|               100|
|                40|            0.5|                65|
|                50|            0.5|                99|
|                60|            0.5|                99|
|                70|            0.5|               100|
|                80|            0.5|                99|
|                90|            0.5|                99|
|               100|            0.5|               100|
|               120|            0.5|               100|
|               140|            0.5|               100|
|               160|            0.5|                99|
|               180|            0.5|                99|
|               200|            0.5|                99|
|                20|            0.6|                92|
|                30|            0.6|                99|
|                40|            0.6|                71|
|                50|            0.6|               100|
|                60|            0.6|               100|
|                70|            0.6|               100|
|                80|            0.6|               100|
|                90|            0.6|               100|
|               100|            0.6|               100|
|               120|            0.6|               100|
|               140|            0.6|               100|
|               160|            0.6|               100|
|               180|            0.6|                99|
|               200|            0.6|               100|
|                20|            0.7|                77|
|                30|            0.7|               100|
|                40|            0.7|                83|
|                50|            0.7|               100|
|                60|            0.7|               100|
|                70|            0.7|               100|
|                80|            0.7|               100|
|                90|            0.7|               100|
|               100|            0.7|               100|
|               120|            0.7|               100|
|               140|            0.7|               100|
|               160|            0.7|               100|
|               180|            0.7|               100|
|               200|            0.7|               100|
|                20|            0.8|                99|
|                30|            0.8|               100|
|                40|            0.8|                85|
|                50|            0.8|                98|
|                60|            0.8|               100|
|                70|            0.8|               100|
|                80|            0.8|               100|
|                90|            0.8|               100|
|               100|            0.8|               100|
|               120|            0.8|               100|
|               140|            0.8|               100|
|               160|            0.8|               100|
|               180|            0.8|               100|
|               200|            0.8|               100|
|                20|            0.9|               100|
|                30|            0.9|               100|
|                40|            0.9|                93|
|                50|            0.9|               100|
|                60|            0.9|               100|
|                70|            0.9|               100|
|                80|            0.9|               100|
|                90|            0.9|               100|
|               100|            0.9|               100|
|               120|            0.9|               100|
|               140|            0.9|               100|
|               160|            0.9|               100|
|               180|            0.9|               100|
|               200|            0.9|               100|
|                20|            1.0|               100|
|                30|            1.0|               100|
|                40|            1.0|               100|
|                50|            1.0|               100|
|                60|            1.0|               100|
|                70|            1.0|               100|
|                80|            1.0|               100|
|                90|            1.0|               100|
|               100|            1.0|               100|
|               120|            1.0|               100|
|               140|            1.0|               100|
|               160|            1.0|               100|
|               180|            1.0|               100|
|               200|            1.0|               100|


## Question 6

Afin de maximiser à la fois Att et Er, il semble logique que la probabilité doit être inversement proportionelle à la taille du voisinage, en effet, si la densité est importante, la probabilité qu'un voisin ai reçu le message en meme temps que moi est plus élevée. On remarque sur les graphiques que les meilleurs résultats sont dans la fourchette d'une taille de réseau de 40-70 noeuds. Pour des réseaux plus petits, l'atteignabilité est trop faible, due au fait qu'il n'y ai pas assez de voisins, et pour les reseaux plus grand la redifusion est trop élévée, du à la densité trop élévée.

Évolution de l'atteignabilité en fonction de la densité du réseau et d'une probabilité inversement
 proportionnelle à la densité.
 
 
| Taille du réseau |  k  | Att500 Moyen (%) | ER500 Moyen (%) |
|------------------|-----|------------------|-----------------|
|                20|    1|                 7|               83|
|                30|    1|                 2|               89|
|                40|    1|                 5|               92|
|                50|    1|                 2|               92|
|                60|    1|                 2|               94|
|                70|    1|                 2|               94|
|                80|    1|                 1|               95|
|                90|    1|                 0|               95|
|               100|    1|                 0|               95|
|               120|    1|                 0|               96|
|               140|    1|                 0|               96|
|               160|    1|                 0|               96|
|               180|    1|                 0|               96|
|               200|    1|                 0|               97|
|                20|    2|                31|               75|
|                30|    2|                13|               83|
|                40|    2|                21|               88|
|                50|    2|                13|               89|
|                60|    2|                15|               91|
|                70|    2|                12|               92|
|                80|    2|                 7|               93|
|                90|    2|                 8|               93|
|               100|    2|                 3|               93|
|               120|    2|                 4|               94|
|               140|    2|                 3|               95|
|               160|    2|                 1|               95|
|               180|    2|                 1|               95|
|               200|    2|                 0|               96|
|                20|    3|                55|               66|
|                30|    3|                28|               77|
|                40|    3|                37|               84|
|                50|    3|                28|               86|
|                60|    3|                33|               88|
|                70|    3|                30|               89|
|                80|    3|                19|               90|
|                90|    3|                22|               91|
|               100|    3|                12|               91|
|               120|    3|                12|               92|
|               140|    3|                11|               93|
|               160|    3|                 3|               94|
|               180|    3|                 5|               94|
|               200|    3|                 4|               95|
|                20|    4|                68|               55|
|                30|    4|                35|               69|
|                40|    4|                59|               80|
|                50|    4|                51|               82|
|                60|    4|                51|               84|
|                70|    4|                50|               86|
|                80|    4|                32|               88|
|                90|    4|                36|               89|
|               100|    4|                24|               89|
|               120|    4|                25|               91|
|               140|    4|                27|               92|
|               160|    4|                16|               92|
|               180|    4|                11|               93|
|               200|    4|                12|               93|
|                20|    5|                90|               44|
|                30|    5|                56|               64|
|                40|    5|                70|               75|
|                50|    5|                64|               78|
|                60|    5|                63|               81|
|                70|    5|                63|               83|
|                80|    5|                53|               85|
|                90|    5|                54|               86|
|               100|    5|                42|               86|
|               120|    5|                41|               88|
|               140|    5|                44|               90|
|               160|    5|                30|               90|
|               180|    5|                26|               91|
|               200|    5|                25|               92|