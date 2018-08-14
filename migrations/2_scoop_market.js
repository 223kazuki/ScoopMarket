const ScoopMarket = artifacts.require('./ScoopMarket.sol');

module.exports = (deployer) => {
  deployer.deploy(ScoopMarket);
};
