package fr.glmfgrpc;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import fr.glmf.grpc.CoupType;
import fr.glmf.grpc.Ping;
import fr.glmf.grpc.PingPongServiceGrpc;
import fr.glmf.grpc.PingPongServiceGrpc.PingPongServiceStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

/**
 * Serveur gRPC servant pour une partie de ping pong multi balle.
 * 
 * Ce client permet de démontrer la relation bidirectionnelle de gRPC en mode stream
 * et quelques éléments de performance lorsque l'on monte le nombre de balles à 10000 par exemple.
 *
 */
public class PingClient {

	private static ExecutorService executor = Executors.newCachedThreadPool();
	private static Random rands = new Random();
	private static StreamObserver<Ping> requetes = null;
	private final ManagedChannel channel;
	private final PingPongServiceStub nonBlockingStub;

	private static AtomicInteger nbPartiesEnCours = new AtomicInteger();
	
	private static AtomicInteger nbEchanges = new AtomicInteger();

	private static final int NB_BALLES = 5;
	private static final boolean partieRapide = false;
	
	private static final int PORT = 1029;

	private static boolean erreurRecue = false;
	
	public PingClient(String host, int port) {
		this.channel = ManagedChannelBuilder.forAddress(host, port)
				//Pas de SSL.
				.usePlaintext().build();
		nonBlockingStub = PingPongServiceGrpc.newStub(channel);
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	public static void main(String[] args) throws Exception {
		PingClient client = new PingClient("localhost", PORT);

		try {
			Instant debutPartie = Instant.now();
			
			//comportement du client pour la méthode lancer définie dans le .proto
			requetes = client.nonBlockingStub.lancer(new StreamObserver<Ping>() {

				// Arrivée d'une balle du serveur.
				@Override
				public void onNext(Ping balle) {
					// Score supérieur à 11 : partie finie.
					if (balle.getScorePing() > 11) {
						tracer("*** Partie gagnée par Ping pour la balle " + balle.getNumBalle() + " !");
						nbPartiesEnCours.decrementAndGet();
						return;
					}
					if (balle.getScorePong() > 11) {
						tracer("*** Partie gagnée par Pong pour la balle " + balle.getNumBalle() + " !");
						nbPartiesEnCours.decrementAndGet();
						return;
					}
					
					// Les coups suite à arrivée d'une balle sont joués de manière asynchrone via l'executor
					// car autrement, il n'y aurait aucun parallélisme de traitement des balles.
					executor.execute(() -> {
						if (!partieRapide) {
							int delais = rands.nextInt(500);
							sleep(delais);
						}
						boolean coupReussi = rands.nextBoolean();
						if (coupReussi) {
							tracer("Ping renvoie la balle " + balle.getNumBalle() + ". Score de "
									+ balle.getScorePing() + " à " + balle.getScorePong());
							envoyerBalle(balle, CoupType.REUSSI);
						} else {
							tracer("Ping rate la balle " + balle.getNumBalle() + ". Score de "
									+ balle.getScorePing() + " à " + (balle.getScorePong() + 1));
							envoyerBalle(balle, CoupType.RATE);
						}
					});
				}

				@Override
				public void onCompleted() {
					tracer("Fin de partie.");
				}

				@Override
				public void onError(Throwable t) {
					System.err.println("Erreur reçue du serveur.");
					t.printStackTrace();
					erreurRecue = true;
				}

			});
			
			System.out.println("Lancement des " + NB_BALLES + " balles.");
			
			for (int i = 0; i < NB_BALLES; i++) {
				envoyerBalle(Ping.newBuilder().setCoup(CoupType.SERVICE).setNumBalle(i).setScorePing(0).setScorePong(0)
						.build(), CoupType.SERVICE);
				nbPartiesEnCours.incrementAndGet();
			}

			// Attente de la fin des parties
			while (!erreurRecue && nbPartiesEnCours.get() > 0) {
				sleep(500);
			}

			Duration dureePartie = Duration.between(debutPartie, Instant.now());
			long vitesse = dureePartie.getSeconds() == 0 ? nbEchanges.get() : nbEchanges.get() / dureePartie.getSeconds();
			System.out.println("Fin de partie. A la prochaine ! Durée : " + dureePartie.getSeconds() + "s (" + vitesse + " échanges / seconde).");
			// On dit au serveur que les parties sont finies avec le client.
			// => fermeture du canal de communication.
			requetes.onCompleted();

		} finally {
			// On libère proprement les ressources.
			executor.shutdown();
			client.shutdown();
		}
	}

	/**
	 * Synchronized car responseObserver n'est pas Thread Safe !!!
	 */
	private static synchronized void envoyerBalle(Ping balle, CoupType coupType) {
		nbEchanges.incrementAndGet();
		if (CoupType.SERVICE.equals(coupType)) {
			requetes.onNext(balle);
			return;
		}
		// retour suite à coup de pong.
		// si raté, on donne un point à pong.
		int nouveauScorePong = CoupType.RATE.equals(coupType) ? balle.getScorePong() + 1 : balle.getScorePong();
		requetes.onNext(Ping.newBuilder(balle).setCoup(coupType).setScorePong(nouveauScorePong).build());
	}


	private static void sleep(long delais) {
		try {
			Thread.sleep(delais);
		} catch (InterruptedException e) {
			// Rien à faire.
		}
	}

	private static void tracer(String str) {
		if (!partieRapide) {
			System.out.println(str);
		}
	}
	
}
