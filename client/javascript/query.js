/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const { Gateway, Wallets } = require('fabric-network');
const path = require('path');
const fs = require('fs');


async function main() {
    try {
        // load the network configuration
        const ccpPath = path.resolve(__dirname, 'download/connection_profile.json');
        const ccp = JSON.parse(fs.readFileSync(ccpPath, 'utf8'));
        const mspId = ccp.client.organization;

        // load the exported user
        const userPath = path.resolve(__dirname, 'download/user.json');
        const user = JSON.parse(fs.readFileSync(userPath, 'utf8'));

        // Create a new file system based wallet for managing identities.
        const walletPath = path.join(process.cwd(), 'wallet');
        const wallet = await Wallets.newFileSystemWallet(walletPath);

        var identity = await wallet.get(user.name);
        if (!identity) {
            const x509Identity = {
                credentials: {
                    certificate: Buffer.from(user.cert, 'base64').toString('utf8'),
                    privateKey: Buffer.from(user.key, 'base64').toString('utf8'),
                },
                mspId: mspId,
                type: 'X.509',
            };
            await wallet.put(user.name, x509Identity);
            identity = await wallet.get(user.name);
        }

        // Create a new gateway for connecting to our peer node.
        const gateway = new Gateway();
        await gateway.connect(ccp, {wallet: wallet, identity: user.name, discovery: { enabled: true, asLocalhost: false } });

        // Get the network (channel) our contract is deployed to.
        const network = await gateway.getNetwork('defaultchannel');

        // Get the contract from the network.
        const contract = network.getContract('fabcar_go');

        // Evaluate the specified transaction.
        // queryCar transaction - requires 1 argument, ex: ('queryCar', 'CAR4')
        // queryAllCars transaction - requires no arguments, ex: ('queryAllCars')
        const result = await contract.evaluateTransaction('queryAllCars');
        console.log(`Transaction has been evaluated, result is: ${result.toString()}`);

        // Disconnect from the gateway.
        await gateway.disconnect();
        
    } catch (error) {
        console.error(`Failed to evaluate transaction: ${error}`);
        process.exit(1);
    }
}

main();
