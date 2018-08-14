pragma solidity ^0.4.23;

// TODO: Delete ../node_modules 
import "../node_modules/openzeppelin-solidity/contracts/token/ERC721/ERC721Token.sol";
import "../node_modules/openzeppelin-solidity/contracts/ownership/Ownable.sol";
import "../node_modules/openzeppelin-solidity/contracts/math/SafeMath.sol";

contract ScoopMarket is ERC721Token, Ownable {
    using SafeMath for uint;

    struct ScoopToken {
        string  name;
        uint    timestamp;
        string  imageURI;
        address author;
        uint    price;
        bool    forSale;
        string  metaDataURI;
        address requestor;
    }
    ScoopToken[] scoops;
    uint public mintCost = 10 ** 16 wei;

    constructor() public ERC721Token("Scoop", "SCP") {}

    function mint(string _name, uint _price, bool _forSale, string _imageURI) external payable {
        require(msg.value == mintCost, "Mint cost is insufficient.");
        ScoopToken memory _scoop = ScoopToken(_name, block.timestamp, _imageURI, msg.sender, _price, _forSale, "", address(0));
        uint tokenID = scoops.push(_scoop).sub(1);
        super._mint(msg.sender, tokenID);
    }

    function setMintCost(uint _mintCost) external onlyOwner {
        mintCost = _mintCost;
    }

    function withdraw() external onlyOwner {
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

    function editToken(uint _tokenID, string _name, uint _price, bool _forSale, string _metaDataURI) external onlyOwnerOf(_tokenID) {
        require(exists(_tokenID), "Token doesn't exist.");
        ScoopToken storage _scoop = scoops[_tokenID];
        _scoop.name = _name;
        _scoop.price = _price;
        _scoop.forSale = _forSale;
        _scoop.metaDataURI = _metaDataURI;
    }

    function scoop(uint _tokenID) external view returns (uint, string, uint, string, uint, bool, string, address, address, address) {
        ScoopToken memory _scoop = scoops[_tokenID];
        address owner = tokenOwner[_tokenID];
        
        return (_tokenID, _scoop.name, _scoop.timestamp, _scoop.imageURI, _scoop.price, _scoop.forSale, 
            _scoop.metaDataURI, _scoop.author, owner, _scoop.requestor);
    }

    function request(uint _tokenID) external payable {
        ScoopToken storage _scoop = scoops[_tokenID];
        _scoop.requestor = msg.sender;
    }

    function cancel(uint _tokenID) external payable {
        ScoopToken storage _scoop = scoops[_tokenID];
        _scoop.requestor = address(0);
        address owner = tokenOwner[_tokenID];
        super.clearApproval(owner, _tokenID);
    }

    function approve(address _to, uint _tokenID) public onlyOwnerOf(_tokenID) {
        require(_to == scoops[_tokenID].requestor, "Tried to approve non requestor.");
        super.approve(_to, _tokenID);
    }

    function deny(uint _tokenID) external payable onlyOwnerOf(_tokenID) {
        ScoopToken storage _scoop = scoops[_tokenID];
        _scoop.requestor = address(0);
    }

    function purchase(uint _tokenID) external payable {
        address seller = tokenOwner[_tokenID];
        address buyer = msg.sender;
        require(seller != buyer, "Can't transfer to myself.");
        require(scoops[_tokenID].forSale == true, "This token isn't for sale.");
        require(scoops[_tokenID].price <= msg.value, "Value is insufficient.");
        ScoopToken memory scoop = scoops[_tokenID];

        safeTransferFrom(seller, buyer, _tokenID);

        scoop.forSale = false;
        scoop.requestor = address(0);

        if (scoops[_tokenID].price > 0) {
            buyer.transfer(scoops[_tokenID].price);
        }
    }

    // TODO: burn
}