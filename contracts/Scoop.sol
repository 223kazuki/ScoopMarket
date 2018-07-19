pragma solidity ^0.4.23;

import "openzeppelin-solidity/contracts/token/ERC721/ERC721Token.sol";
import "openzeppelin-solidity/contracts/ownership/Ownable.sol";
import "openzeppelin-solidity/contracts/math/SafeMath.sol";

contract Scoop is ERC721Token, Ownable {
    using SafeMath for uint;

    uint internal tokenIDNonce = 0;
    uint public mintCost = 10 ** 16 wei;

    constructor() public ERC721Token("Scoop", "SCP") {}

    function mint() external payable {
        require(msg.value == mintCost);
        uint tokenId = tokenIDNonce;
        tokenIDNonce = tokenIDNonce.add(1);
        super._mint(msg.sender, tokenId);
    }

    function setTokenURI(uint _tokenId, string _url) external onlyOwnerOf(_tokenId) {
        super._setTokenURI(_tokenId, _url);
    }

    function setMintCost(uint _mintCost) external onlyOwner {
        mintCost = _mintCost;
    }

}