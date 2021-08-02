/*
SPDX-License-Identifier: Apache-2.0
*/

package com.naver.agent.http;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.security.CryptoPrimitives;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Benchmark {

	static {
		System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "false");
	}

	public static int threadCount = 100;
	public static int requestCount = 200;	// request per thread

	public static class HttpTx implements Callable<Object> {

		public HttpTx() {
		}

		@Override
		public Object call() {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://nelo2-col.navercorp.com/_hello"))
					.build();
			HttpClient client = HttpClient.newHttpClient();
			for (int i=0; i < requestCount; i++) {
				try {
					HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//					System.out.println("response: " + response.body());
				} catch(Exception e) {
					System.out.println(i + "th request failed with error: " + e.getMessage());
				}
			}

			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		List<Callable<Object>> transactions = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			transactions.add(new HttpTx());
		}

		System.out.println("Start http tx ...");
		long start = System.currentTimeMillis();
		executor.invokeAll(transactions);
		executor.shutdown();
		long end = System.currentTimeMillis();
		System.out.println("http tx finished.");
		System.out.println(threadCount * requestCount + " tx in " + (end - start) + "ms");
		System.out.println("TPS: " + (int) (threadCount * requestCount * 1000 / (end - start)));

		System.exit(0);
	}
}
