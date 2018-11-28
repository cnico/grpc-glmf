package fr.glmfgrpc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import fr.glmf.grpc.BonjourRequest;
import fr.glmf.grpc.BonjourResponse;
import fr.glmf.grpc.HelloworldServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class HelloClient {

	private final ManagedChannel channel;
	private final HelloworldServiceGrpc.HelloworldServiceBlockingStub blockingStub;
	
	private static final int PORT = 1027;

	public HelloClient(String host, int port) {
		this.channel = ManagedChannelBuilder.forAddress(host, port)
				//Pas de SSL.
				.usePlaintext().build();
		blockingStub = HelloworldServiceGrpc.newBlockingStub(channel);
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	public String hello(String nom) {
		BonjourResponse response = blockingStub.bonjour(BonjourRequest.newBuilder().setNom(nom).build());
		return response.getReponse();
	}


	public static void main(String[] args) throws Exception {
		HelloClient client = new HelloClient("localhost", PORT);
		
		System.out.println("Lancement de la boucle d'attente. Saisissez un mot : ");
		try {
			final BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				final String msg = console.readLine();
				if (msg == null || msg.equalsIgnoreCase("bye")) {
					break;
				} else {
					//Appel gRPC fait via la méthode hello.
					System.out.println(client.hello(msg));
				}
			}
			System.out.println("Fin.");
		} finally {
			client.shutdown();
		}
	}

}
