pragma solidity ^0.4.23;

// TODO: Delete ../node_modules/
import "../node_modules/openzeppelin-solidity/contracts/token/ERC721/ERC721Token.sol";
import "../node_modules/openzeppelin-solidity/contracts/ownership/Ownable.sol";
import "../node_modules/openzeppelin-solidity/contracts/math/SafeMath.sol";

contract Scoop is ERC721Token, Ownable {
    using SafeMath for uint;

    uint internal tokenIDNonce = 0;
    uint public mintCost = 10 ** 16 wei;

    constructor() public ERC721Token("Scoop", "SCP") {}

    function mint(string _uri) external payable {
        require(msg.value == mintCost);
        uint tokenID = tokenIDNonce;
        tokenIDNonce = tokenIDNonce.add(1);
        super._mint(msg.sender, tokenID);
        super._setTokenURI(tokenID, _uri);
    }

    function setMintCost(uint _mintCost) external onlyOwner {
        mintCost = _mintCost;
    }

    function scoopsOf(address _address) external view returns (uint[]) {
        return ownedTokens[_address];
    }

    function scoop(uint _tokenID) external view returns (uint, string) {
        return (_tokenID, tokenURI(_tokenID));
    }
}