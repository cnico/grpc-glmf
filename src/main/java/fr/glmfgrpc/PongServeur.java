package fr.glmfgrpc;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import fr.glmf.grpc.CoupType;
import fr.glmf.grpc.Ping;
import fr.glmf.grpc.PingPongServiceGrpc.PingPongServiceImplBase;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

/**
 * Serveur gRPC servant de partenaire pour une partie de ping pong multi balle.
 * Pour arrêter, faire un kill.
 */
public class PongServeur {

	private Server server;

	private static Executor executor = Executors.newCachedThreadPool();
	private static Random rands = new Random();

	private static boolean partieRapide = false;
	
	private static final int port = 1029;
	
	
	public static void main(String[] args) throws IOException, InterruptedException {
		final PongServeur server = new PongServeur();
		server.start();
	}
	
	private void start() throws IOException, InterruptedException {
	
		server = ServerBuilder.forPort(port).addService(new PingPongServiceImpl()).build().start();
		System.out.println("Serveur démarré en écoute sur le port : " + port);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				PongServeur.this.stop();
				System.out.println("*** serveur arrêté");
			}
		});
		
		//Boucle inifinie du serveur : 
		server.awaitTermination();
	}

	private void stop() {
		if (server != null) {
			server.shutdown();
		}
	}

/**
 * Comportement du serveur gRPC.
 * La méthode lancer est celle définie dans le .proto.
 *
 */
	class PingPongServiceImpl extends PingPongServiceImplBase {

		@Override
		public StreamObserver<Ping> lancer(StreamObserver<Ping> responseObserver) {

			return new StreamObserver<Ping>() {
				@Override
				public void onNext(Ping balle) {
					// Score supérieur à 11 : partie finie. On renvoie la balle pour le décompte du score.
					if (balle.getScorePing() > 11) {
						tracer("*** Partie gagnée par Ping pour la balle " + balle.getNumBalle() + " !");
						renvoyerBalle(responseObserver, balle, CoupType.FIN);
						return;
					}
					if (balle.getScorePong() > 11) {
						tracer("*** Partie gagnée par Pong pour la balle " + balle.getNumBalle() + " !");
						renvoyerBalle(responseObserver, balle, CoupType.FIN);
						return;
					}
					// Les coups de réponse sont joués de manière asynchrone via l'executor
					// car autrement, il n'y aurait aucun parallélisme de traitement des balles.
					executor.execute(() -> {
						if (!partieRapide) {
							//simule un temps de traitement.
							int delais = rands.nextInt(500);
							sleep(delais);
						}
						
						boolean coupReussi = rands.nextBoolean();
						if (coupReussi) {
							tracer("Pong renvoie la balle " + balle.getNumBalle() + ". Score de "
									+ balle.getScorePing() + " à " + balle.getScorePong());
							renvoyerBalle(responseObserver, balle, CoupType.REUSSI);
						} else {
							tracer("Pong rate la balle " + balle.getNumBalle() + ". Score de "
									+ (balle.getScorePing() + 1) + " à " + balle.getScorePong());
							renvoyerBalle(responseObserver, balle, CoupType.RATE);
						}
					});

				}

				@Override
				public void onCompleted() {
					System.out.println("Fin de partie.");
				}

				@Override
				public void onError(Throwable t) {
					System.err.println("Erreur reçue du client.");
					t.printStackTrace();
				}
			};
		}

		/**
		 * StreamObserver n'est pas Thread Safe donc les usages de responseObserver sont à synchronizer !!!
		 */
		private synchronized void renvoyerBalle(StreamObserver<Ping> responseObserver, Ping balle, CoupType coupType) {
			int nouveauScorePing = CoupType.RATE.equals(coupType) ? balle.getScorePing() + 1 : balle.getScorePing();
			responseObserver.onNext(Ping.newBuilder(balle).setCoup(coupType).setScorePing(nouveauScorePing).build());
		}
	}

	private static void tracer(String str) {
		if (!partieRapide) {
			System.out.println(str);
		}
	}
	
	private static void sleep(long delais) {
		try {
			Thread.sleep(delais);
		} catch (InterruptedException e) {
			// Rien à faire.
		}
	}
}
