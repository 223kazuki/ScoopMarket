const Scoop = artifacts.require('./Scoop.sol');

module.exports = (deployer) => {
  deployer.deploy(Scoop);
};
