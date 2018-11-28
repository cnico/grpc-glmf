package fr.glmfgrpc;

import java.io.IOException;

import fr.glmf.grpc.BonjourRequest;
import fr.glmf.grpc.BonjourResponse;
import fr.glmf.grpc.HelloworldServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class HelloServeur {

	private Server server;

	private static final int port = 1027;
	
	private void start() throws IOException, InterruptedException {
		server = ServerBuilder.forPort(port).addService(new HelloworldServiceImpl()).build().start();
		System.out.println("Serveur démarré en écoute sur le port : " + port);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				HelloServeur.this.stop();
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

	public static void main(String[] args) throws IOException, InterruptedException {
		final HelloServeur server = new HelloServeur();
		server.start();
	}

	/**
	 * Comportement du service gRPC "bonjour".
	 */
	class HelloworldServiceImpl extends HelloworldServiceGrpc.HelloworldServiceImplBase {

		@Override
		public void bonjour(BonjourRequest request, StreamObserver<BonjourResponse> responseObserver) {
			String nom = request.getNom();
			System.out.println("Demande de bonjour de " + nom);
			BonjourResponse response = BonjourResponse.newBuilder().setReponse("Bonjour " + nom).build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}
}
