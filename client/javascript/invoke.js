/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const { Gateway, Wallets } = require('fabric-network');
const fs = require('fs');
const path = require('path');

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

        // Submit the specified transaction.
        // createCar transaction - requires 5 argument, ex: ('createCar', 'CAR12', 'Honda', 'Accord', 'Black', 'Tom')
        // changeCarOwner transaction - requires 2 args , ex: ('changeCarOwner', 'CAR12', 'Dave')
        const number = Math.floor(Math.random() * 100);
        await contract.submitTransaction('createCar', 'CAR' + number, 'Honda', 'Accord', 'Black', 'Tom');
        console.log('Transaction has been submitted for CAR' + number);

        // Disconnect from the gateway.
        await gateway.disconnect();

    } catch (error) {
        console.error(`Failed to submit transaction: ${error}`);
        process.exit(1);
    }
}

main();
