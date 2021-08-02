/*
SPDX-License-Identifier: Apache-2.0
*/

package com.naver.agent.fabcar;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.security.CryptoPrimitives;

import javax.json.Json;
import javax.json.JsonObject;

public class Benchmark {

	static {
		System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "false");
	}

	public static int threadCount = 100;
	public static int requestCount = 200;	// request per thread

	public static class WriteTx implements Callable<Object> {
		int startIndex;
		Contract contract;

		public WriteTx(int startIndex, Contract contract) {
			this.startIndex = startIndex;
			this.contract = contract;
		}

		@Override
		public Object call() {
			String randomOwner = "Owner" + new Random().nextInt(1000);
			for (int i=0; i < requestCount; i++) {
				String carNumber = "CAR" + (startIndex + i);
				try {
					byte [] result = contract.submitTransaction("createCar", carNumber, "VW", "Polo", "Grey", randomOwner);
				} catch(Exception e) {
					System.out.println(carNumber + " failed with error: " + e.getMessage());
				}
			}

			return null;
		}
	}

	public static class ReadTx implements Callable<Object> {
		int startIndex;
		Contract contract;

		public ReadTx(int startIndex, Contract contract) {
			this.startIndex = startIndex;
			this.contract = contract;
		}

		public Object call() {
			for (int i=0; i < requestCount; i++) {
				String carNumber = "CAR" + (startIndex + i);
				try {
					byte [] result = contract.evaluateTransaction("queryCar", carNumber);
				} catch(Exception e) {
					System.out.println(carNumber + " failed with error: " + e.getMessage());
				}
			}

			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		// Load a file system based wallet for managing identities.
		Path walletPath = Paths.get("wallet");
		Wallet wallet = Wallet.createFileSystemWallet(walletPath);

		// load a CCP
		Path networkConfigPath = Paths.get("download", "connection_profile.json");
		InputStream is = new FileInputStream(networkConfigPath.toFile());
		NetworkConfig ccp = NetworkConfig.fromJsonStream(is);
		String mspId = ccp.getClientOrganization().getMspId();

		// load the exported user
		Path userPath = Paths.get("download", "user.json");
		is = new FileInputStream(userPath.toFile());
		JsonObject userObject = (JsonObject) Json.createReader(is).read();
		String userId = userObject.getString("name");

		boolean userExists = wallet.exists(userId);
		if (!userExists) {
			CryptoPrimitives crypto = new CryptoPrimitives();
			Wallet.Identity user = Wallet.Identity.createIdentity(mspId,
					new String(Base64.getDecoder().decode(userObject.getString("cert"))),
					crypto.bytesToPrivateKey(Base64.getDecoder().decode(userObject.getString("key"))));
			wallet.put(userId, user);
		}

		Gateway.Builder builder = Gateway.createBuilder();
		builder.identity(wallet, userId).networkConfig(networkConfigPath).discovery(true);

		// create a gateway connection
		Gateway gateway = builder.connect();

		// get the network and contract
		Network network = gateway.getNetwork("defaultchannel");
		Contract contract = network.getContract("fabcar_go");

		// check client JVM performance
		runDummyBench();

		// warming up
		System.out.println("Warming up...");
		contract.submitTransaction("createCar", "CAR0", "VW", "Polo", "Grey", "Warming up");
		System.out.println("Warming up Done");

		runWriteTx(contract);
		System.out.println("Sleeping some seconds...");
		Thread.sleep(1000 * 5);
		runReadTx(contract);

		gateway.close();
		System.exit(0);
	}

	public static void runDummyBench() {
		System.out.println("Start dummy Benchmark ...");
		long start = System.currentTimeMillis();
		List<String> transactions = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			for (int j = 0; j < 1000; j++) {
				transactions.add("Strings" + i + "-" + j);
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("dummy Benchmark finished in " + (end - start) + "ms");
	}

	public static void runWriteTx(Contract contract) throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		List<Callable<Object>> transactions = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			transactions.add(new Benchmark.WriteTx(i * requestCount, contract));
		}

		System.out.println("Start write tx ...");
		long start = System.currentTimeMillis();
		executor.invokeAll(transactions);
		executor.shutdown();
		long end = System.currentTimeMillis();
		System.out.println("write tx finished.");
		System.out.println(threadCount * requestCount + " tx in " + (end - start) + "ms");
		System.out.println("TPS: " + (int) (threadCount * requestCount * 1000 / (end - start)));
	}

	public static void runReadTx(Contract contract) throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		List<Callable<Object>> transactions = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			transactions.add(new Benchmark.ReadTx(i * requestCount, contract));
		}

		System.out.println("Start read tx ...");
		long start = System.currentTimeMillis();
		executor.invokeAll(transactions);
		executor.shutdown();
		long end = System.currentTimeMillis();
		System.out.println("read tx finished.");
		System.out.println(threadCount * requestCount + " tx in " + (end - start) + "ms");
		System.out.println("TPS: " + (int) (threadCount * requestCount * 1000 / (end - start)));
	}
}
