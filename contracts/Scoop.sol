pragma solidity ^0.4.23;

// TODO: Delete ../node_modules/
import "../node_modules/openzeppelin-solidity/contracts/token/ERC721/ERC721Token.sol";
import "../node_modules/openzeppelin-solidity/contracts/ownership/Ownable.sol";
import "../node_modules/openzeppelin-solidity/contracts/math/SafeMath.sol";

contract Scoop is ERC721Token, Ownable {
    using SafeMath for uint;

    struct ScoopToken {
        string  name;
        uint    timestamp;
        string  imageURI;
        uint    price;
        bool    forSale;
        string  metaDataURI;
        address author;
        address requestor;
    }
    ScoopToken[] scoops;

    uint public mintCost = 10 ** 16 wei;

    constructor() public ERC721Token("Scoop", "SCP") {}

    function mint(string _name, uint _price, bool _forSale, string _imageURI) external payable {
        require(msg.value == mintCost);

        ScoopToken memory _scoop = ScoopToken(_name, block.timestamp, _imageURI, _price, _forSale, "", msg.sender, address(0));
        uint tokenID = scoops.push(_scoop).sub(1);

        super._mint(msg.sender, tokenID);
    }

    function setMintCost(uint _mintCost) external onlyOwner {
        mintCost = _mintCost;
    }

    function scoopsOf(address _address) external view returns (uint[]) {
        return ownedTokens[_address];
    }

    function scoopsForSale() external view returns (bool[]) {
        bool[] memory result = new bool[](scoops.length);
        for (uint i = 0; i < scoops.length; i ++) {
            result[i] = scoops[i].forSale;
        }
        return result;
    }

    function setTokenMetaDataUri(uint256 _tokenID, string _metaDataURI) external onlyOwnerOf(_tokenID) {
        require(exists(_tokenID));
        ScoopToken storage _scoop = scoops[_tokenID];
        _scoop.metaDataURI = _metaDataURI;
    }

    function setTokenPrice(uint256 _tokenID, uint _price) external onlyOwnerOf(_tokenID) {
        require(exists(_tokenID));
        ScoopToken storage _scoop = scoops[_tokenID];
        _scoop.price = _price;
    }

    function setTokenForSale(uint256 _tokenID, bool _forSale) external onlyOwnerOf(_tokenID) {
        require(exists(_tokenID));
        ScoopToken storage _scoop = scoops[_tokenID];
        _scoop.forSale = _forSale;
    }

    function scoop(uint _tokenID) external view returns (uint, string, uint, string, uint, bool, string, address, address, address) {
        ScoopToken memory _scoop = scoops[_tokenID];
        address owner = tokenOwner[_tokenID];
        return (_tokenID, _scoop.name, _scoop.timestamp, _scoop.imageURI, _scoop.price, _scoop.forSale, 
            _scoop.metaDataURI, _scoop.author, owner, _scoop.requestor);
    }

    function request(uint256 _tokenID) external payable {
        require(scoops[_tokenID].price / 10 <= msg.value);
        ScoopToken storage _scoop = scoops[_tokenID];
        _scoop.requestor = msg.sender;
    }

    function approve(address _to, uint256 _tokenID) public {
        require(_to == scoops[_tokenID].requestor);
        super.approve(_to, _tokenID);
    }

    function purchase(uint256 _tokenID) external payable {
        address oldOwner = tokenOwner[_tokenID];
        address newOwner = msg.sender;
        require(oldOwner != newOwner);
        require(scoops[_tokenID].price <= msg.value);
        require(scoops[_tokenID].forSale == true);
        
        ScoopToken memory _scoop = scoops[_tokenID];
        uint price = _scoop.price;
        require(msg.value >= price);

        safeTransferFrom(oldOwner, newOwner, _tokenID);

        scoops[_tokenID].forSale = false;
        scoops[_tokenID].requestor = address(0);

        if (price > 0) {
            newOwner.transfer(price);
        }
    }
}