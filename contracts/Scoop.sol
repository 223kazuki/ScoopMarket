pragma solidity ^0.4.23;

// TODO: Delete ../node_modules/
import "../node_modules/openzeppelin-solidity/contracts/token/ERC721/ERC721Token.sol";
import "../node_modules/openzeppelin-solidity/contracts/ownership/Ownable.sol";
import "../node_modules/openzeppelin-solidity/contracts/math/SafeMath.sol";

contract Scoop is ERC721Token, Ownable {
    using SafeMath for uint;

    struct ScoopStruct {
        uint   timestamp;
        string imageURI;
        string metaDataURI;
    }

    ScoopStruct[] scoops;

    uint public mintCost = 10 ** 16 wei;

    constructor() public ERC721Token("Scoop", "SCP") {}

    function mint(string _imageURI) external payable {
        require(msg.value == mintCost);

        ScoopStruct memory _scoop = ScoopStruct(block.timestamp, _imageURI, "");
        uint tokenID = scoops.push(_scoop).sub(1);

        super._mint(msg.sender, tokenID);
    }

    function setMintCost(uint _mintCost) external onlyOwner {
        mintCost = _mintCost;
    }

    function scoopsOf(address _address) external view returns (uint[]) {
        return ownedTokens[_address];
    }

    function setTokenMetaDataUri(uint256 _tokenID, string _metaDataURI) external {
        require(exists(_tokenID));
        ScoopStruct storage scoop = scoops[_tokenID];
        scoop.metaDataURI = _metaDataURI;
    }

    function scoop(uint _tokenID) external view returns (uint, uint, string, string) {
        ScoopStruct memory _scoop = scoops[_tokenID];
        return (_tokenID, _scoop.timestamp, _scoop.imageURI, _scoop.metaDataURI);
    }
}