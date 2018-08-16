pragma solidity ^0.4.23;

// TODO: Delete ../node_modules 
import "../node_modules/openzeppelin-solidity/contracts/token/ERC721/ERC721Token.sol";
import "../node_modules/openzeppelin-solidity/contracts/math/SafeMath.sol";
import "../node_modules/openzeppelin-solidity/contracts/lifecycle/Pausable.sol";
import "../node_modules/openzeppelin-solidity/contracts/payment/PullPayment.sol";

contract ScoopMarket is ERC721Token, Pausable, PullPayment {
  using SafeMath for uint;

  event Minted(string _name, uint _price, bool _forSale, string _imageHash);
  event MintCostSet(uint _mintCost);
  event TokenEdited(uint _tokenID, string _name, uint _price, bool _forSale, string _metaDataHash);
  event Requested(uint _tokenID);
  event Canceled(uint _tokenID);
  event Denied(uint _tokenID);
  event Purchased(uint _tokenID);
  event PaymentsWithdrawed();

  struct Scoop {
    string  name;
    uint    timestamp;
    string  imageHash;
    address author;
    uint    price;
    bool    forSale;
    string  metaDataHash;
    address requestor;
  }

  Scoop[] scoops;
  uint public mintCost;

  modifier onlyForSaleToken(uint _tokenID) {
    require(scoops[_tokenID].forSale, "Only for sale token.");
    _;
  }

  constructor() public ERC721Token("Scoop", "SCP") {}
  
  function mint(string _name, uint _price, bool _forSale, string _imageHash) external payable whenNotPaused {
    uint _nameLength = bytes(_name).length;
    require(_nameLength > 0 && _nameLength <= 50, "Name length must be between 1 to 50");
    uint _imageHashLength = bytes(_imageHash).length;
    require(_imageHashLength == 46, "Image hash length must be 46.");
    require(msg.value == mintCost, "Mint cost is insufficient.");

    Scoop memory _scoop = Scoop(_name, block.timestamp, _imageHash, msg.sender, _price, _forSale, "", address(0));
    uint _tokenID = scoops.push(_scoop).sub(1);
    super._mint(msg.sender, _tokenID);
    asyncTransfer(owner, msg.value);
    emit Minted(_name, _price, _forSale, _imageHash);
  }
  
  function setMintCost(uint _mintCost) external onlyOwner {
    mintCost = _mintCost;
    emit MintCostSet(_mintCost);
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
  
  function editToken(uint _tokenID, string _name, uint _price, bool _forSale, string _metaDataHash) 
    external onlyOwnerOf(_tokenID) whenNotPaused {
    require(exists(_tokenID), "Token doesn't exist.");
    uint _nameLength = bytes(_name).length;
    require(_nameLength > 0 && _nameLength <= 50, "Name length must be between 1 to 50");
    uint _metaDataHashLength = bytes(_metaDataHash).length;
    require(_metaDataHashLength == 46, "Meta data hash length must be 46.");

    Scoop storage _scoop = scoops[_tokenID];
    _scoop.name = _name;
    _scoop.price = _price;
    _scoop.forSale = _forSale;
    _scoop.metaDataHash = _metaDataHash;
    emit TokenEdited(_tokenID, _name, _price, _forSale, _metaDataHash);
  }
  
  function scoop(uint _tokenID) external view returns (uint, string, uint, string, uint, bool, string, address, address, address) {
    Scoop memory _scoop = scoops[_tokenID];
    address _owner = tokenOwner[_tokenID];
    
    return (_tokenID, _scoop.name, _scoop.timestamp, _scoop.imageHash, _scoop.price, _scoop.forSale, 
      _scoop.metaDataHash, _scoop.author, _owner, _scoop.requestor);
  }
  
  function request(uint _tokenID) external payable onlyForSaleToken(_tokenID) whenNotPaused {
    Scoop storage _scoop = scoops[_tokenID];
    _scoop.requestor = msg.sender;
    emit Requested(_tokenID);
  }
  
  function cancel(uint _tokenID) external payable whenNotPaused {
    Scoop storage _scoop = scoops[_tokenID];
    _scoop.requestor = address(0);
    address owner = tokenOwner[_tokenID];
    super.clearApproval(owner, _tokenID);
    emit Canceled(_tokenID);
  }
  
  function approve(address _to, uint _tokenID) public onlyOwnerOf(_tokenID) onlyForSaleToken(_tokenID) whenNotPaused {
    require(_to == scoops[_tokenID].requestor, "Tried to approve non requestor.");
    super.approve(_to, _tokenID);
  }
  
  function deny(uint _tokenID) external payable onlyOwnerOf(_tokenID) whenNotPaused {
    Scoop storage _scoop = scoops[_tokenID];
    _scoop.requestor = address(0);
    emit Denied(_tokenID);
  }
  
  function purchase(uint _tokenID) external payable onlyForSaleToken(_tokenID) whenNotPaused {
    address seller = tokenOwner[_tokenID];
    address buyer = msg.sender;
    require(seller != buyer, "Can't transfer to myself.");
    require(getApproved(_tokenID) == buyer, "sender is not approved.");
    require(scoops[_tokenID].price == msg.value, "Value is insufficient.");

    safeTransferFrom(seller, buyer, _tokenID);

    Scoop storage _scoop = scoops[_tokenID];
    _scoop.forSale = false;
    _scoop.requestor = address(0);

    if (_scoop.price > 0) {
      asyncTransfer(seller, _scoop.price);
    }
    emit Purchased(_tokenID);
  }

  function withdrawPayments() public whenNotPaused {
    super.withdrawPayments();
    emit PaymentsWithdrawed();
  }
}