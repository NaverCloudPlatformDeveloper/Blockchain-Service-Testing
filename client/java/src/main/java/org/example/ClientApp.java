/*
SPDX-License-Identifier: Apache-2.0
*/

package org.example;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.security.CryptoPrimitives;

import javax.json.Json;
import javax.json.JsonObject;

public class ClientApp {

	static {
		System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "false");
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
		try (Gateway gateway = builder.connect()) {

			// get the network and contract
			Network network = gateway.getNetwork("defaultchannel");
			Contract contract = network.getContract("fabcar_go");

			byte[] result;

			result = contract.evaluateTransaction("queryAllCars");
			System.out.println(new String(result));
			System.exit(0);

			// contract.submitTransaction("createCar", "CAR10", "VW", "Polo", "Grey", "Mary");

			// result = contract.evaluateTransaction("queryCar", "CAR10");
			// System.out.println(new String(result));

			// contract.submitTransaction("changeCarOwner", "CAR10", "Archie");

			// result = contract.evaluateTransaction("queryCar", "CAR10");
			// System.out.println(new String(result));
		}
	}

}
