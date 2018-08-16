# Design Pattern Decisions

## Overview
ScoopMarket is a Proof of Existence dApp as long as a Market dApp.
So I selected design patterns by thinking primarily about preventing data loss or tamper.
And because it has sales function, I also adopted design patterns with that it can safely deal with them.

## Design Patterns

### Closed-system Dapp
I developed ScoopMarket as a Closed-system Dapp that consists of single contract.
That is because I want to keep this dApp easy to audit it has no bug which causes data loss or tamper.

On the other side, this choice sacrifices upgradability.
But as upgrading contracts often causes data loss, I didn't choose it.

Instead of upgradability, I applyed emergency stop pattern in order to prevent an attack to a fatal bug.

### ERC-721
Every "Scoop" must be unique and tradable.
So I designed it as a ERC-721 token.
By using OpenZeppelin, I could develop it as secure ERC-721 token.

http://erc721.org/

### Using IPFS
It is not realistic to store images on Blockchain directly.
So ScoopMarket stores image on IPFS and writes only hash of the image data on Blockchain.

By using IPFS, I can take following benefits.

* Reducing the cost of transaction.
* As the hash is unique to the image data itself, the data can not be tamperd.
* Hard to lose data bacause it is stored in decentralized IPFS network.

### Using uPort
As "Scoop" should be uploaded by smart phone, it must run on mobile browser.
But it's difficult to expect users to use web3 enabled mobile browser such as Toshi or Cipher.
That's why I adopted uPort.
Users can access to Blockchain from basic mobile web browser and send transaction by connecting uPort.
Moreover, users can manage "Scoop" in association with their identities on uPort.

### Guard Check Pattern
Each function of contract checkes its input parameters as early as possible.
And if invalid parameter comes, it stops its execution in order not to consume unnecessary gasses.

### Access Restriction Pattern
As there a some roles on ScoopMarket, it is necessary to restrict access to functions according to their role.
I implemented modifiers to manage restrictions of functions because it is easy to apply and audit.

### Pull over Push Pattern
ScoopMarket deals with sending Ethers from contract to users and owner.
The most intuitive method of sending Ether is a direct ```send``` call.
But because that has a potential security risk, I applyed withdrawal pattern.
By using that, it can send Ethre safely.

### Emergency Stop Pattern
If there is an attack to a fatal bug, it shold stop critical functionalities emegently.
I developed this by using "Pausable" contract of OpenZeppelin.
