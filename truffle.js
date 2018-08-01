var HDWalletProvider = require("truffle-hdwallet-provider");
var mnemonic = process.env.MNEMONIC;
var accessToken = process.env.INFURA_ACCESS_TOKEN;

module.exports = {
  networks: {
    ropsten: {
      provider: function() {
        var provider = new HDWalletProvider(
          mnemonic,
          "https://ropsten.infura.io/" + accessToken
        );
        console.log(provider.addresses)
        return provider;
      },
      network_id: 1533140371286,
      gas: 4700000
    },
    development: {
      host: '127.0.0.1',
      port: 8545,
      network_id: '*',
      gas: 6000000
    }
  }
};
